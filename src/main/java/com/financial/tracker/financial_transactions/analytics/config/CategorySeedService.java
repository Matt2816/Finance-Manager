package com.financial.tracker.financial_transactions.analytics.config;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantCategoryRuleRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class CategorySeedService {

    private static final Logger log = LoggerFactory.getLogger(CategorySeedService.class);

    private final SpendingCategoryRepository categoryRepository;
    private final MerchantCategoryRuleRepository ruleRepository;

    public CategorySeedService(
            SpendingCategoryRepository categoryRepository,
            MerchantCategoryRuleRepository ruleRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional
    public void seedForUser(Long userId) {
        if (categoryRepository.countByUserId(userId) > 0) {
            return;
        }
        try {
            seedFromYaml(userId);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to seed categories for user " + userId, ex);
        }
    }

    void seedFromYaml(Long userId) throws Exception {
        ClassPathResource resource = new ClassPathResource("analytics/merchant-rules.yml");
        Map<String, Long> slugToId = new HashMap<>();
        String currentSection = "";
        String currentSlug = null;
        String currentDisplay = null;
        String currentPattern = null;
        String currentCategory = null;
        int currentPriority = 100;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("categories:")) {
                    currentSection = "categories";
                    continue;
                }
                if (trimmed.startsWith("rules:")) {
                    flushRule(userId, currentPattern, currentCategory, currentPriority, slugToId);
                    currentSection = "rules";
                    continue;
                }
                if (trimmed.startsWith("- slug:")) {
                    currentSlug = trimmed.substring("- slug:".length()).trim();
                    currentDisplay = null;
                } else if (trimmed.startsWith("displayName:") && "categories".equals(currentSection)) {
                    currentDisplay = trimmed.substring("displayName:".length()).trim();
                    if (currentSlug != null) {
                        SpendingCategory category = new SpendingCategory();
                        category.setUserId(userId);
                        category.setSlug(currentSlug);
                        category.setDisplayName(currentDisplay);
                        category = categoryRepository.save(category);
                        slugToId.put(currentSlug, category.getId());
                    }
                } else if (trimmed.startsWith("- pattern:") && "rules".equals(currentSection)) {
                    flushRule(userId, currentPattern, currentCategory, currentPriority, slugToId);
                    currentPattern = unquote(trimmed.substring("- pattern:".length()).trim());
                    currentCategory = null;
                    currentPriority = 100;
                } else if (trimmed.startsWith("category:") && "rules".equals(currentSection)) {
                    currentCategory = trimmed.substring("category:".length()).trim();
                } else if (trimmed.startsWith("priority:") && "rules".equals(currentSection)) {
                    currentPriority = Integer.parseInt(trimmed.substring("priority:".length()).trim());
                }
            }
            flushRule(userId, currentPattern, currentCategory, currentPriority, slugToId);
        }
        log.info("Seeded {} categories for user {}", slugToId.size(), userId);
    }

    private void flushRule(
            Long userId,
            String pattern,
            String categorySlug,
            int priority,
            Map<String, Long> slugToId
    ) {
        if (pattern == null || categorySlug == null) {
            return;
        }
        Long categoryId = slugToId.get(categorySlug);
        if (categoryId == null) {
            return;
        }
        MerchantCategoryRule rule = new MerchantCategoryRule();
        rule.setUserId(userId);
        rule.setPattern(pattern);
        rule.setCategoryId(categoryId);
        rule.setPriority(priority);
        ruleRepository.save(rule);
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
