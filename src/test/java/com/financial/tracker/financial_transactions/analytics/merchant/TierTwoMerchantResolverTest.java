package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TierTwoMerchantResolverTest {

    @Mock
    private MerchantAliasRepository aliasRepository;

    private TierTwoMerchantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TierTwoMerchantResolver(aliasRepository);
    }

    @Test
    void resolve_fuzzyMatchHighConfidence_returnsResolvedMerchant() {
        Merchant merchant = new Merchant();
        merchant.setId(java.util.UUID.randomUUID());
        merchant.setCanonicalName("Tim Hortons");
        merchant.setCategory("Coffee");

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchant(merchant);
        alias.setNormalizedName("TIM HORTONS");

        when(aliasRepository.findCandidatesByTrigram(anyString(), anyInt()))
                .thenReturn(List.of(alias));

        Optional<ResolvedMerchant> result = resolver.resolve("TIM HORTONS 9436");

        assertTrue(result.isPresent());
        assertEquals("Tim Hortons", result.get().canonicalName());
        assertEquals(ResolutionTier.FUZZY_MATCH, result.get().resolvedBy());
    }

    @Test
    void resolve_rejectsLowTokenOverlap() {
        Merchant merchant = new Merchant();
        merchant.setId(java.util.UUID.randomUUID());
        merchant.setCanonicalName("Tim Hortons");
        merchant.setCategory("Coffee");

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchant(merchant);
        alias.setNormalizedName("TIM HORTONS");

        when(aliasRepository.findCandidatesByTrigram(anyString(), anyInt()))
                .thenReturn(List.of(alias));

        Optional<ResolvedMerchant> result = resolver.resolve("TIM");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_noCandidates_returnsEmpty() {
        when(aliasRepository.findCandidatesByTrigram(anyString(), anyInt()))
                .thenReturn(List.of());

        Optional<ResolvedMerchant> result = resolver.resolve("UNKNOWN MERCHANT XYZ");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_fallsBackWhenTrigramUnavailable() {
        Merchant merchant = new Merchant();
        merchant.setId(java.util.UUID.randomUUID());
        merchant.setCanonicalName("Amazon");
        merchant.setCategory("Shopping");

        MerchantAlias alias = new MerchantAlias();
        alias.setMerchant(merchant);
        alias.setNormalizedName("AMAZON");

        when(aliasRepository.findCandidatesByTrigram(anyString(), anyInt()))
                .thenThrow(new InvalidDataAccessResourceUsageException("operator does not exist: %"));
        when(aliasRepository.findAll()).thenReturn(List.of(alias));

        Optional<ResolvedMerchant> result = resolver.resolve("AMAZON");

        assertTrue(result.isPresent());
        assertEquals("Amazon", result.get().canonicalName());
    }
}
