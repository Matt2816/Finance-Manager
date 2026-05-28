package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.model.MerchantLoyaltyMetrics;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantLoyaltyMetricsRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * One-time and recurring maintenance to align legacy alias/loyalty data
 * with the hardened normalization pipeline.
 */
@Service
public class MerchantAnalyticsRebuildService {

    private static final Logger log = LoggerFactory.getLogger(MerchantAnalyticsRebuildService.class);

    private final MerchantAliasRepository aliasRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantLoyaltyMetricsRepository loyaltyRepository;
    private final MerchantNormalizer normalizer;

    public MerchantAnalyticsRebuildService(
            MerchantAliasRepository aliasRepository,
            MerchantRepository merchantRepository,
            MerchantLoyaltyMetricsRepository loyaltyRepository,
            MerchantNormalizer normalizer
    ) {
        this.aliasRepository = aliasRepository;
        this.merchantRepository = merchantRepository;
        this.loyaltyRepository = loyaltyRepository;
        this.normalizer = normalizer;
    }

    @Transactional
    public RebuildResult rebuildMerchantData() {
        int merchantsUppercased = uppercaseMerchantCanonicalNames();
        int aliasesNormalized = normalizeAliasKeys();
        int aliasesMerged = mergeDuplicateAliases();
        int loyaltyKeysRemoved = removeDuplicateLoyaltyMetrics();
        log.info("Merchant rebuild: merchantsUppercased={}, aliasesNormalized={}, aliasesMerged={}, loyaltyKeysRemoved={}",
                merchantsUppercased, aliasesNormalized, aliasesMerged, loyaltyKeysRemoved);
        return new RebuildResult(merchantsUppercased, aliasesNormalized, aliasesMerged, loyaltyKeysRemoved);
    }

    private int uppercaseMerchantCanonicalNames() {
        int updated = 0;
        for (Merchant merchant : merchantRepository.findAll()) {
            String upper = merchant.getCanonicalName().toUpperCase().trim();
            if (!upper.equals(merchant.getCanonicalName())) {
                Optional<Merchant> existing = merchantRepository.findByCanonicalNameIgnoreCase(upper);
                if (existing.isPresent() && !existing.get().getId().equals(merchant.getId())) {
                    continue;
                }
                merchant.setCanonicalName(upper);
                merchantRepository.save(merchant);
                updated++;
            }
        }
        return updated;
    }

    private int normalizeAliasKeys() {
        int updated = 0;
        for (MerchantAlias alias : aliasRepository.findAll()) {
            String upper = alias.getNormalizedName().toUpperCase().trim();
            if (!upper.equals(alias.getNormalizedName())) {
                Optional<MerchantAlias> existing = aliasRepository.findByNormalizedName(upper);
                if (existing.isPresent()) {
                    continue;
                }
                alias.setNormalizedName(upper);
                aliasRepository.save(alias);
                updated++;
            }
        }
        return updated;
    }

    private int mergeDuplicateAliases() {
        Map<String, List<MerchantAlias>> byCanonicalKey = aliasRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> normalizer.normalize(a.getNormalizedName())));

        int merged = 0;
        for (Map.Entry<String, List<MerchantAlias>> entry : byCanonicalKey.entrySet()) {
            List<MerchantAlias> group = entry.getValue();
            if (group.size() <= 1) {
                continue;
            }

            MerchantAlias keeper = group.stream()
                    .filter(a -> a.getNormalizedName().equals(entry.getKey()))
                    .findFirst()
                    .orElse(group.get(0));

            for (MerchantAlias duplicate : group) {
                if (duplicate.getId().equals(keeper.getId())) {
                    continue;
                }
                if (duplicate.getMerchant().getId().equals(keeper.getMerchant().getId())) {
                    aliasRepository.delete(duplicate);
                    merged++;
                }
            }
        }
        return merged;
    }

    private int removeDuplicateLoyaltyMetrics() {
        int removed = 0;
        Map<String, MerchantLoyaltyMetrics> byKey = new LinkedHashMap<>();
        for (MerchantLoyaltyMetrics metrics : loyaltyRepository.findAll()) {
            String key = metrics.getMerchantKey();
            if (!byKey.containsKey(key)) {
                byKey.put(key, metrics);
            } else {
                loyaltyRepository.delete(metrics);
                removed++;
            }
        }
        return removed;
    }

    public record RebuildResult(
            int merchantsUppercased,
            int aliasesNormalized,
            int aliasesMerged,
            int loyaltyKeysRemoved
    ) {
    }
}
