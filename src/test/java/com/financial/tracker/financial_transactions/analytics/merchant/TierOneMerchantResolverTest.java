package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TierOneMerchantResolverTest {

    @Mock
    private MerchantAliasRepository aliasRepository;
    @Mock
    private MerchantNormalizer normalizer;

    private TierOneMerchantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TierOneMerchantResolver(aliasRepository, normalizer);
    }

    @Test
    void resolve_exactMatch_returnsResolvedMerchant() {
        Merchant merchant = new Merchant();
        merchant.setId(java.util.UUID.randomUUID());
        merchant.setCanonicalName("Amazon");
        merchant.setCategory("Shopping");

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchant(merchant);
        alias.setNormalizedName("AMAZON");

        when(normalizer.normalize("Amazon CA")).thenReturn("AMAZON");
        when(aliasRepository.findByNormalizedName("AMAZON")).thenReturn(Optional.of(alias));

        Optional<ResolvedMerchant> result = resolver.resolve("Amazon CA");

        assertTrue(result.isPresent());
        assertEquals("Amazon", result.get().canonicalName());
        assertEquals(ResolutionTier.EXACT_MATCH, result.get().resolvedBy());
        assertEquals(1.0, result.get().confidence());
    }

    @Test
    void resolve_noMatch_returnsEmpty() {
        when(normalizer.normalize(anyString())).thenReturn("UNKNOWN");
        when(aliasRepository.findByNormalizedName("UNKNOWN")).thenReturn(Optional.empty());

        Optional<ResolvedMerchant> result = resolver.resolve("Some Unknown Merchant");

        assertTrue(result.isEmpty());
    }
}
