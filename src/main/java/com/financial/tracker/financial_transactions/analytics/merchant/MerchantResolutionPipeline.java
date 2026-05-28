package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final MerchantResolutionMetrics metrics;

    public MerchantResolutionPipeline(TierOneMerchantResolver tierOne,
                                      TierTwoMerchantResolver tierTwo,
                                      MerchantNormalizer normalizer,
                                      MerchantAliasRepository aliasRepository,
                                      MerchantRepository merchantRepository,
                                      CacheManager cacheManager,
                                      MerchantResolutionMetrics metrics) {
        this.tierOne = tierOne;
        this.tierTwo = tierTwo;
        this.normalizer = normalizer;
        this.aliasRepository = aliasRepository;
        this.merchantRepository = merchantRepository;
        this.cacheManager = cacheManager;
        this.metrics = metrics;
    }

    public Optional<ResolvedMerchant> resolve(String rawDescription) {
        Optional<ResolvedMerchant> resolved = tierOne.resolve(rawDescription);
        if (resolved.isPresent()) {
            metrics.recordTier1Hit();
            return resolved;
        }

        String normalized = normalizer.normalize(rawDescription);
        Optional<ResolvedMerchant> fuzzyResolved = tierTwo.resolve(normalized);
        if (fuzzyResolved.isPresent()) {
            ResolvedMerchant result = fuzzyResolved.get();
            metrics.recordTier2Hit();
            if (result.confidence() >= PROMOTION_THRESHOLD) {
                promoteAlias(rawDescription, normalized, result);
            }
            return Optional.of(result);
        }

        metrics.recordUnresolved(rawDescription);
        return Optional.empty();
    }

    private void promoteAlias(String rawDescription, String normalizedInput, ResolvedMerchant resolved) {
        if (aliasRepository.findByNormalizedName(normalizedInput).isPresent()) {
            return;
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
        try {
            aliasRepository.save(alias);
            metrics.recordPromotion();
        } catch (DataIntegrityViolationException ex) {
            // Another thread or prior run may have inserted the alias
            return;
        }

        evictCache(rawDescription);
    }

    private void evictCache(String rawDescription) {
        if (cacheManager == null) {
            return;
        }
        org.springframework.cache.Cache cache = cacheManager.getCache("merchantAliasCache");
        if (cache != null) {
            cache.evict(rawDescription);
        }
    }
}
