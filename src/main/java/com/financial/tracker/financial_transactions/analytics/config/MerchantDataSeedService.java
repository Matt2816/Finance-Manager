package com.financial.tracker.financial_transactions.analytics.config;

import com.financial.tracker.financial_transactions.analytics.merchant.MerchantNormalizer;
import com.financial.tracker.financial_transactions.analytics.model.*;
import com.financial.tracker.financial_transactions.analytics.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class MerchantDataSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantDataSeedService.class);

    private final NormalizationRuleRepository normalizationRuleRepository;
    private final AbbreviationMappingRepository abbreviationRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository aliasRepository;
    private final MerchantNormalizer merchantNormalizer;

    public MerchantDataSeedService(
            NormalizationRuleRepository normalizationRuleRepository,
            AbbreviationMappingRepository abbreviationRepository,
            MerchantRepository merchantRepository,
            MerchantAliasRepository aliasRepository,
            MerchantNormalizer merchantNormalizer
    ) {
        this.normalizationRuleRepository = normalizationRuleRepository;
        this.abbreviationRepository = abbreviationRepository;
        this.merchantRepository = merchantRepository;
        this.aliasRepository = aliasRepository;
        this.merchantNormalizer = merchantNormalizer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedNormalizationRules();
        seedAbbreviationMappings();
        seedMerchantsAndAliases();
        merchantNormalizer.reloadRules();
    }

    private void seedNormalizationRules() throws Exception {
        if (normalizationRuleRepository.count() > 0) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("analytics/normalization-rules.yml");
        List<NormalizationRule> rules = new ArrayList<>();
        NormalizationRule current = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("normalizationRules:")) {
                    continue;
                }
                if (trimmed.startsWith("- ruleName:") && current != null) {
                    rules.add(current);
                }
                if (trimmed.startsWith("- ruleName:")) {
                    current = new NormalizationRule();
                    current.setRuleName(unquote(trimmed.substring("- ruleName:".length()).trim()));
                } else if (trimmed.startsWith("pattern:") && current != null) {
                    current.setPattern(unquote(trimmed.substring("pattern:".length()).trim()));
                } else if (trimmed.startsWith("replacement:") && current != null) {
                    current.setReplacement(unquote(trimmed.substring("replacement:".length()).trim()));
                } else if (trimmed.startsWith("priority:") && current != null) {
                    current.setPriority(Integer.parseInt(trimmed.substring("priority:".length()).trim()));
                }
            }
            if (current != null) {
                rules.add(current);
            }
        }
        normalizationRuleRepository.saveAll(rules);
        log.info("Seeded {} normalization rules", rules.size());
    }

    private void seedAbbreviationMappings() throws Exception {
        if (abbreviationRepository.count() > 0) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("analytics/abbreviation-mappings.yml");
        List<AbbreviationMapping> mappings = new ArrayList<>();
        AbbreviationMapping current = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("abbreviationMappings:")) {
                    continue;
                }
                if (trimmed.startsWith("- abbreviation:") && current != null) {
                    mappings.add(current);
                }
                if (trimmed.startsWith("- abbreviation:")) {
                    current = new AbbreviationMapping();
                    current.setAbbreviation(unquote(trimmed.substring("- abbreviation:".length()).trim()));
                } else if (trimmed.startsWith("expansion:") && current != null) {
                    current.setExpansion(unquote(trimmed.substring("expansion:".length()).trim()));
                }
            }
            if (current != null) {
                mappings.add(current);
            }
        }
        abbreviationRepository.saveAll(mappings);
        log.info("Seeded {} abbreviation mappings", mappings.size());
    }

    private void seedMerchantsAndAliases() throws Exception {
        if (merchantRepository.count() > 0) {
            return;
        }
        // Seed from existing brand rules YAML
        ClassPathResource resource = new ClassPathResource("analytics/merchant-brand-rules.yml");
        List<Merchant> merchants = new ArrayList<>();
        List<MerchantAlias> aliases = new ArrayList<>();
        java.util.Set<String> seenAliases = new java.util.HashSet<>();

        String currentPattern = null;
        String currentCanonical = null;
        String currentSection = "";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("brandRules:")) {
                    currentSection = "brandRules";
                    continue;
                }
                if (trimmed.startsWith("- pattern:") && "brandRules".equals(currentSection)) {
                    flushMerchantAndAlias(merchants, aliases, seenAliases, currentPattern, currentCanonical);
                    currentPattern = unquote(trimmed.substring("- pattern:".length()).trim());
                    currentCanonical = null;
                } else if (trimmed.startsWith("canonical:") && "brandRules".equals(currentSection)) {
                    currentCanonical = trimmed.substring("canonical:".length()).trim();
                }
            }
            flushMerchantAndAlias(merchants, aliases, seenAliases, currentPattern, currentCanonical);
        }

        if (!merchants.isEmpty()) {
            merchantRepository.saveAll(merchants);
            for (MerchantAlias alias : aliases) {
                // Find the merchant that matches this alias's canonical name
                merchants.stream()
                        .filter(m -> m.getCanonicalName().equalsIgnoreCase(alias.getMerchant().getCanonicalName()))
                        .findFirst()
                        .ifPresent(alias::setMerchant);
            }
            aliasRepository.saveAll(aliases);
        }
        log.info("Seeded {} merchants and {} aliases", merchants.size(), aliases.size());
    }

    private void flushMerchantAndAlias(List<Merchant> merchants, List<MerchantAlias> aliases,
                                       java.util.Set<String> seenAliases,
                                       String pattern, String canonical) {
        if (pattern == null || canonical == null) {
            return;
        }
        // Check if merchant already in list
        boolean merchantExists = merchants.stream()
                .anyMatch(m -> m.getCanonicalName().equalsIgnoreCase(canonical));
        if (!merchantExists) {
            Merchant merchant = new Merchant();
            merchant.setCanonicalName(canonical);
            merchant.setCategory("uncategorized");
            merchants.add(merchant);
        }

        // Create alias using the canonical as normalized name for now
        String normalizedName = canonical.toLowerCase();
        if (seenAliases.add(normalizedName)) {
            MerchantAlias alias = new MerchantAlias();
            Merchant placeholder = new Merchant();
            placeholder.setCanonicalName(canonical);
            alias.setMerchant(placeholder);
            alias.setNormalizedName(normalizedName);
            alias.setSource("seed");
            aliases.add(alias);
        }
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
