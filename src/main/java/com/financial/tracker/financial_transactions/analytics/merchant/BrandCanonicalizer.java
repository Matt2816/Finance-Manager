package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.MerchantBrandRule;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantBrandRuleRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Applies brand-family regex rules after base normalization to collapse
 * noisy descriptors (e.g. AMZN MKTP CA*... -> AMAZON, TIMS #4021 -> TIM HORTONS).
 */
@Service
public class BrandCanonicalizer {

    private final MerchantBrandRuleRepository brandRuleRepository;

    private volatile List<CompiledBrandRule> compiledRules = List.of();

    public BrandCanonicalizer(MerchantBrandRuleRepository brandRuleRepository) {
        this.brandRuleRepository = brandRuleRepository;
    }

    @PostConstruct
    public void loadRules() {
        List<MerchantBrandRule> dbRules = brandRuleRepository.findAllByOrderByPriorityAsc();
        List<CompiledBrandRule> rules = new ArrayList<>();
        for (MerchantBrandRule rule : dbRules) {
            try {
                rules.add(new CompiledBrandRule(
                        Pattern.compile(rule.getPattern()),
                        rule.getCanonicalName().toUpperCase(),
                        rule.getPriority()
                ));
            } catch (Exception ignored) {
                // Skip invalid patterns
            }
        }
        this.compiledRules = List.copyOf(rules);
    }

    public String canonicalize(String normalizedInput) {
        if (StringUtils.isBlank(normalizedInput)) {
            return "";
        }
        for (CompiledBrandRule rule : compiledRules) {
            if (rule.pattern().matcher(normalizedInput).lookingAt()) {
                return rule.canonicalName();
            }
        }
        return normalizedInput;
    }

    public void reloadRules() {
        loadRules();
    }

    private record CompiledBrandRule(Pattern pattern, String canonicalName, int priority) {
    }
}
