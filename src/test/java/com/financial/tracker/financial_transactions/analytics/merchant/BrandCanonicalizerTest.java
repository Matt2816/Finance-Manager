package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.MerchantBrandRule;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantBrandRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandCanonicalizerTest {

    @Mock
    private MerchantBrandRuleRepository brandRuleRepository;

    private BrandCanonicalizer canonicalizer;

    @BeforeEach
    void setUp() {
        canonicalizer = new BrandCanonicalizer(brandRuleRepository);
        when(brandRuleRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of(
                rule("(?i)^amazon", "AMAZON", 10),
                rule("(?i)^amzn", "AMAZON", 11),
                rule("(?i)^tim hortons", "TIM HORTONS", 130),
                rule("(?i)^tims\\b", "TIM HORTONS", 131)
        ));
        canonicalizer.loadRules();
    }

    private MerchantBrandRule rule(String pattern, String canonical, int priority) {
        MerchantBrandRule r = new MerchantBrandRule();
        r.setPattern(pattern);
        r.setCanonicalName(canonical);
        r.setPriority(priority);
        return r;
    }

    @Test
    void canonicalize_amazonVariants() {
        assertEquals("AMAZON", canonicalizer.canonicalize("AMZN MKTP CANADA"));
        assertEquals("AMAZON", canonicalizer.canonicalize("AMAZON"));
    }

    @Test
    void canonicalize_timHortonsVariants() {
        assertEquals("TIM HORTONS", canonicalizer.canonicalize("TIM HORTONS"));
        assertEquals("TIM HORTONS", canonicalizer.canonicalize("TIMS"));
    }

    @Test
    void canonicalize_unknownReturnsInput() {
        assertEquals("UNKNOWN MERCHANT", canonicalizer.canonicalize("UNKNOWN MERCHANT"));
    }
}
