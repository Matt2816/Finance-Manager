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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(2)
public class MerchantDataSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantDataSeedService.class);

    private static final Map<String, String> CATEGORY_BY_CANONICAL = Map.ofEntries(
            Map.entry("AMAZON", "shopping"),
            Map.entry("WALMART", "groceries"),
            Map.entry("COSTCO", "groceries"),
            Map.entry("TARGET", "shopping"),
            Map.entry("BEST BUY", "shopping"),
            Map.entry("APPLE", "subscriptions"),
            Map.entry("GOOGLE", "subscriptions"),
            Map.entry("MICROSOFT", "subscriptions"),
            Map.entry("NETFLIX", "entertainment"),
            Map.entry("SPOTIFY", "entertainment"),
            Map.entry("UBER", "transit"),
            Map.entry("LYFT", "transit"),
            Map.entry("TIM HORTONS", "coffee"),
            Map.entry("LITE BITE", "coffee"),
            Map.entry("FREEDOM MOBILE", "subscriptions"),
            Map.entry("PRESTO", "transit"),
            Map.entry("METRO", "groceries"),
            Map.entry("SOBEYS", "groceries"),
            Map.entry("LOBLAWS", "groceries"),
            Map.entry("SHOPPERS DRUG MART", "health")
    );

    private final NormalizationRuleRepository normalizationRuleRepository;
    private final AbbreviationMappingRepository abbreviationRepository;
    private final MerchantBrandRuleRepository brandRuleRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository aliasRepository;
    private final MerchantNormalizer merchantNormalizer;

    public MerchantDataSeedService(
            NormalizationRuleRepository normalizationRuleRepository,
            AbbreviationMappingRepository abbreviationRepository,
            MerchantBrandRuleRepository brandRuleRepository,
            MerchantRepository merchantRepository,
            MerchantAliasRepository aliasRepository,
            MerchantNormalizer merchantNormalizer
    ) {
        this.normalizationRuleRepository = normalizationRuleRepository;
        this.abbreviationRepository = abbreviationRepository;
        this.brandRuleRepository = brandRuleRepository;
        this.merchantRepository = merchantRepository;
        this.aliasRepository = aliasRepository;
        this.merchantNormalizer = merchantNormalizer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedNormalizationRules();
        seedAbbreviationMappings();
        seedBrandRules();
        seedMerchantsAndAliases();
        merchantNormalizer.reloadRules();
    }

    private void seedNormalizationRules() throws Exception {
        ClassPathResource resource = new ClassPathResource("analytics/normalization-rules.yml");
        List<NormalizationRule> yamlRules = parseNormalizationRules(resource);
        Map<String, NormalizationRule> existingByName = normalizationRuleRepository.findAll().stream()
                .collect(Collectors.toMap(NormalizationRule::getRuleName, r -> r, (a, b) -> a));

        int inserted = 0;
        int updated = 0;
        for (NormalizationRule yamlRule : yamlRules) {
            NormalizationRule existing = existingByName.get(yamlRule.getRuleName());
            if (existing == null) {
                normalizationRuleRepository.save(yamlRule);
                inserted++;
            } else if (!existing.getPattern().equals(yamlRule.getPattern())
                    || !Objects.equals(existing.getReplacement(), yamlRule.getReplacement())
                    || existing.getPriority() != yamlRule.getPriority()) {
                existing.setPattern(yamlRule.getPattern());
                existing.setReplacement(yamlRule.getReplacement());
                existing.setPriority(yamlRule.getPriority());
                existing.setEnabled(true);
                normalizationRuleRepository.save(existing);
                updated++;
            }
        }
        log.info("Normalization rules: inserted={}, updated={}", inserted, updated);
    }

    private void seedAbbreviationMappings() throws Exception {
        if (abbreviationRepository.count() > 0) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("analytics/abbreviation-mappings.yml");
        List<AbbreviationMapping> mappings = parseAbbreviationMappings(resource);
        abbreviationRepository.saveAll(mappings);
        log.info("Seeded {} abbreviation mappings", mappings.size());
    }

    private void seedBrandRules() throws Exception {
        ClassPathResource resource = new ClassPathResource("analytics/merchant-brand-rules.yml");
        List<BrandRuleSeed> seeds = parseBrandRules(resource);
        int inserted = 0;
        for (BrandRuleSeed seed : seeds) {
            boolean exists = brandRuleRepository.findAllByOrderByPriorityAsc().stream()
                    .anyMatch(r -> r.getPattern().equals(seed.pattern()));
            if (exists) {
                continue;
            }
            MerchantBrandRule rule = new MerchantBrandRule();
            rule.setPattern(seed.pattern());
            rule.setCanonicalName(seed.canonical().toUpperCase());
            rule.setPriority(seed.priority());
            brandRuleRepository.save(rule);
            inserted++;
        }
        log.info("Seeded {} new brand rules ({} total in YAML)", inserted, seeds.size());
    }

    private void seedMerchantsAndAliases() throws Exception {
        ClassPathResource resource = new ClassPathResource("analytics/merchant-brand-rules.yml");
        List<BrandRuleSeed> seeds = parseBrandRules(resource);
        Set<String> canonicalNames = new LinkedHashSet<>();
        for (BrandRuleSeed seed : seeds) {
            canonicalNames.add(seed.canonical().toUpperCase());
        }

        int merchantsCreated = 0;
        int aliasesCreated = 0;
        for (String canonical : canonicalNames) {
            Merchant merchant = merchantRepository.findByCanonicalNameIgnoreCase(canonical)
                    .orElseGet(() -> {
                        Merchant m = new Merchant();
                        m.setCanonicalName(canonical);
                        m.setCategory(CATEGORY_BY_CANONICAL.getOrDefault(canonical, "uncategorized"));
                        return merchantRepository.save(m);
                    });
            if (merchant.getCreatedAt() != null && merchant.getUpdatedAt() != null
                    && merchant.getCreatedAt().equals(merchant.getUpdatedAt())) {
                merchantsCreated++;
            }

            String aliasKey = canonical;
            if (aliasRepository.findByNormalizedName(aliasKey).isEmpty()) {
                MerchantAlias alias = new MerchantAlias();
                alias.setMerchant(merchant);
                alias.setNormalizedName(aliasKey);
                alias.setSource("seed");
                alias.setConfidence(BigDecimal.ONE);
                aliasRepository.save(alias);
                aliasesCreated++;
            }
        }
        log.info("Merchant seed complete: {} new merchants, {} new aliases", merchantsCreated, aliasesCreated);
    }

    private List<NormalizationRule> parseNormalizationRules(ClassPathResource resource) throws Exception {
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
                    current.setEnabled(true);
                }
            }
            if (current != null) {
                rules.add(current);
            }
        }
        return rules;
    }

    private List<AbbreviationMapping> parseAbbreviationMappings(ClassPathResource resource) throws Exception {
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
        return mappings;
    }

    private List<BrandRuleSeed> parseBrandRules(ClassPathResource resource) throws Exception {
        List<BrandRuleSeed> seeds = new ArrayList<>();
        String currentPattern = null;
        String currentCanonical = null;
        int currentPriority = 100;
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
                    if (currentPattern != null && currentCanonical != null) {
                        seeds.add(new BrandRuleSeed(currentPattern, currentCanonical, currentPriority));
                    }
                    currentPattern = unquote(trimmed.substring("- pattern:".length()).trim());
                    currentCanonical = null;
                    currentPriority = 100;
                } else if (trimmed.startsWith("canonical:") && "brandRules".equals(currentSection)) {
                    currentCanonical = trimmed.substring("canonical:".length()).trim();
                } else if (trimmed.startsWith("priority:") && "brandRules".equals(currentSection)) {
                    currentPriority = Integer.parseInt(trimmed.substring("priority:".length()).trim());
                }
            }
            if (currentPattern != null && currentCanonical != null) {
                seeds.add(new BrandRuleSeed(currentPattern, currentCanonical, currentPriority));
            }
        }
        return seeds;
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record BrandRuleSeed(String pattern, String canonical, int priority) {
    }
}
