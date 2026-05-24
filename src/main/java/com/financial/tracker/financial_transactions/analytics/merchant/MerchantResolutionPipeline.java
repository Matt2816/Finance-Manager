package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class MerchantResolutionPipeline {

    private static final double PROMOTION_THRESHOLD = 0.82;

    private final TierOneMerchantResolver tierOne;
    private final TierTwoMerchantResolver tierTwo;
    private final MerchantNormalizer normalizer;
    private final MerchantAliasRepository aliasRepository;
    private final MerchantRepository merchantRepository;
    private final CacheManager cacheManager;

    public MerchantResolutionPipeline(TierOneMerchantResolver tierOne,
                                      TierTwoMerchantResolver tierTwo,
                                      MerchantNormalizer normalizer,
                                      MerchantAliasRepository aliasRepository,
                                      MerchantRepository merchantRepository,
                                      CacheManager cacheManager) {
        this.tierOne = tierOne;
        this.tierTwo = tierTwo;
        this.normalizer = normalizer;
        this.aliasRepository = aliasRepository;
        this.merchantRepository = merchantRepository;
        this.cacheManager = cacheManager;
    }

    public Optional<ResolvedMerchant> resolve(String rawDescription) {
        // Tier 1: Exact match
        Optional<ResolvedMerchant> resolved = tierOne.resolve(rawDescription);
        if (resolved.isPresent()) {
            return resolved;
        }

        // Tier 2: Fuzzy match
        String normalized = normalizer.normalize(rawDescription);
        Optional<ResolvedMerchant> fuzzyResolved = tierTwo.resolve(normalized);
        if (fuzzyResolved.isPresent()) {
            ResolvedMerchant result = fuzzyResolved.get();
            if (result.confidence() >= PROMOTION_THRESHOLD) {
                promoteAlias(normalized, result);
            }
            return Optional.of(result);
        }

        return Optional.empty();
    }

    private void promoteAlias(String normalizedInput, ResolvedMerchant resolved) {
        if (aliasRepository.findByNormalizedName(normalizedInput).isPresent()) {
            return; // Already exists
        }

        Optional<Merchant> merchantOpt = merchantRepository.findById(resolved.merchantId());
        if (merchantOpt.isEmpty()) {
            return;
        }

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchant(merchantOpt.get());
        alias.setNormalizedName(normalizedInput);
        alias.setSource("tier2_promoted");
        alias.setConfidence(BigDecimal.valueOf(resolved.confidence()));
        aliasRepository.save(alias);

        // Evict cache so the new alias is picked up on next call
        if (cacheManager != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache("merchantAliasCache");
            if (cache != null) {
                cache.evict(normalizedInput);
            }
        }
    }
}
