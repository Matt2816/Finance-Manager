package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantCategoryRuleRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class MerchantIntelligenceEngine {

    private final NormalizedTransactionRepository normalizedRepo;
    private final MerchantCategoryRuleRepository ruleRepository;
    private final SpendingCategoryRepository categoryRepository;
    private final MerchantResolutionPipeline resolutionPipeline;
    private final MerchantResolutionMetrics metrics;

    public MerchantIntelligenceEngine(
            NormalizedTransactionRepository normalizedRepo,
            MerchantCategoryRuleRepository ruleRepository,
            SpendingCategoryRepository categoryRepository,
            MerchantResolutionPipeline resolutionPipeline,
            MerchantResolutionMetrics metrics
    ) {
        this.normalizedRepo = normalizedRepo;
        this.ruleRepository = ruleRepository;
        this.categoryRepository = categoryRepository;
        this.resolutionPipeline = resolutionPipeline;
        this.metrics = metrics;
    }

    @Transactional
    public ProcessResult processAll() {
        metrics.reset();

        List<MerchantCategoryRule> dbRules = ruleRepository.findAllByOrderByPriorityAsc();
        List<CompiledCategoryRule> compiledRules = new ArrayList<>();
        for (MerchantCategoryRule rule : dbRules) {
            try {
                compiledRules.add(new CompiledCategoryRule(Pattern.compile(rule.getPattern()), rule.getCategoryId()));
            } catch (Exception e) {
                // Skip invalid patterns
            }
        }

        Long uncategorizedId = categoryRepository.findBySlug("uncategorized")
                .map(SpendingCategory::getId)
                .orElse(null);

        int updated = 0;
        for (NormalizedTransaction normalized : normalizedRepo.findAll()) {
            Long categoryId = classify(normalized, compiledRules, uncategorizedId);
            normalized.setCategoryId(categoryId);
            normalizedRepo.save(normalized);
            updated++;
        }

        Map<String, Object> resolutionStats = new LinkedHashMap<>(metrics.snapshot());
        return new ProcessResult(updated, resolutionStats);
    }

    private Long classify(
            NormalizedTransaction normalized,
            List<CompiledCategoryRule> rules,
            Long uncategorizedId
    ) {
        Optional<ResolvedMerchant> resolved = resolutionPipeline.resolve(normalized.getMerchantRaw());
        if (resolved.isPresent()) {
            normalized.setResolvedMerchantId(resolved.get().merchantId());
            Long merchantCategoryId = categoryRepository.findBySlug(
                            resolved.get().category().toLowerCase().replace(" ", "-"))
                    .map(SpendingCategory::getId)
                    .orElse(null);
            if (merchantCategoryId != null) {
                return merchantCategoryId;
            }
        } else {
            normalized.setResolvedMerchantId(null);
        }

        String searchText = normalized.getMerchantRaw() + " " + normalized.getMerchantKey();
        for (CompiledCategoryRule rule : rules) {
            if (rule.pattern().matcher(searchText).find()) {
                return rule.categoryId();
            }
        }

        return uncategorizedId;
    }

    private record CompiledCategoryRule(Pattern pattern, Long categoryId) {
    }

    public record ProcessResult(int transactionsUpdated, Map<String, Object> resolutionStats) {}
}
