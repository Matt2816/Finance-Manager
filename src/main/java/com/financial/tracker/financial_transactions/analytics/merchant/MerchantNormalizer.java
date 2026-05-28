package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.AbbreviationMapping;
import com.financial.tracker.financial_transactions.analytics.model.NormalizationRule;
import com.financial.tracker.financial_transactions.analytics.repo.AbbreviationMappingRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizationRuleRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MerchantNormalizer {

    private final NormalizationRuleRepository ruleRepository;
    private final AbbreviationMappingRepository abbreviationRepository;
    private final BrandCanonicalizer brandCanonicalizer;

    private volatile List<CompiledRule> compiledRules = List.of();
    private volatile Map<String, String> abbreviationMap = Map.of();

    public MerchantNormalizer(NormalizationRuleRepository ruleRepository,
                              AbbreviationMappingRepository abbreviationRepository,
                              BrandCanonicalizer brandCanonicalizer) {
        this.ruleRepository = ruleRepository;
        this.abbreviationRepository = abbreviationRepository;
        this.brandCanonicalizer = brandCanonicalizer;
    }

    @PostConstruct
    public void loadRules() {
        List<NormalizationRule> dbRules = ruleRepository.findAllByEnabledTrueOrderByPriorityAsc();
        List<CompiledRule> rules = new ArrayList<>();
        for (NormalizationRule rule : dbRules) {
            try {
                rules.add(new CompiledRule(rule.getRuleName(), Pattern.compile(rule.getPattern()), rule.getReplacement()));
            } catch (Exception e) {
                // Skip invalid patterns
            }
        }
        this.compiledRules = List.copyOf(rules);

        List<AbbreviationMapping> mappings = abbreviationRepository.findAll();
        this.abbreviationMap = mappings.stream()
                .collect(Collectors.toUnmodifiableMap(AbbreviationMapping::getAbbreviation, AbbreviationMapping::getExpansion));
    }

    /**
     * Base normalization: uppercase, regex cleanup, abbreviation expansion.
     * Does not apply brand-family canonicalization.
     */
    public String baseNormalize(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }

        String result = raw.toUpperCase().trim();

        for (CompiledRule rule : compiledRules) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }

        result = expandAbbreviations(result);
        return StringUtils.normalizeSpace(result);
    }

    /**
     * Full normalization including brand-family canonicalization.
     */
    public String normalize(String raw) {
        String base = baseNormalize(raw);
        if (base.isEmpty()) {
            return base;
        }
        return brandCanonicalizer.canonicalize(base);
    }

    private String expandAbbreviations(String input) {
        String[] tokens = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(abbreviationMap.getOrDefault(token, token));
        }
        return sb.toString();
    }

    public void reloadRules() {
        loadRules();
        brandCanonicalizer.reloadRules();
    }

    private record CompiledRule(String name, Pattern pattern, String replacement) {
    }
}
