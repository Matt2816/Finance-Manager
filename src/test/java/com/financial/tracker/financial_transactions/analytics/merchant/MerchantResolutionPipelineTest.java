package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantResolutionPipelineTest {

    @Mock
    private TierOneMerchantResolver tierOne;
    @Mock
    private TierTwoMerchantResolver tierTwo;
    @Mock
    private MerchantNormalizer normalizer;
    @Mock
    private MerchantAliasRepository aliasRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private MerchantResolutionMetrics metrics;

    private MerchantResolutionPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new MerchantResolutionPipeline(
                tierOne, tierTwo, normalizer, aliasRepository, merchantRepository, cacheManager, metrics
        );
    }

    @Test
    void resolve_tier1Hit_recordsMetrics() {
        UUID merchantId = UUID.randomUUID();
        ResolvedMerchant resolved = new ResolvedMerchant(
                merchantId, "AMAZON", "shopping", null, ResolutionTier.EXACT_MATCH, 1.0, "AMAZON"
        );
        when(tierOne.resolve("AMZN MKTP CA")).thenReturn(Optional.of(resolved));

        Optional<ResolvedMerchant> result = pipeline.resolve("AMZN MKTP CA");

        assertTrue(result.isPresent());
        assertEquals("AMAZON", result.get().canonicalName());
        verify(metrics).recordTier1Hit();
        verify(metrics, never()).recordUnresolved(any());
    }

    @Test
    void resolve_tier2PromotesAliasAndEvictsRawCacheKey() {
        UUID merchantId = UUID.randomUUID();
        String raw = "AMZN MKTP CA*PY8IM93N3";
        String normalized = "AMAZON";

        when(tierOne.resolve(raw)).thenReturn(Optional.empty());
        when(normalizer.normalize(raw)).thenReturn(normalized);
        when(tierTwo.resolve(normalized)).thenReturn(Optional.of(
                new ResolvedMerchant(merchantId, "AMAZON", "shopping", null, ResolutionTier.FUZZY_MATCH, 0.95, normalized)
        ));
        when(aliasRepository.findByNormalizedName(normalized)).thenReturn(Optional.empty());

        Merchant merchant = new Merchant();
        merchant.setId(merchantId);
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        pipeline.resolve(raw);

        verify(aliasRepository).save(any(MerchantAlias.class));
        verify(metrics).recordTier2Hit();
        verify(metrics).recordPromotion();
    }
}
