package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.AbbreviationMapping;
import com.financial.tracker.financial_transactions.analytics.model.NormalizationRule;
import com.financial.tracker.financial_transactions.analytics.repo.AbbreviationMappingRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MerchantNormalizerTest {

    @Mock
    private NormalizationRuleRepository ruleRepository;
    @Mock
    private AbbreviationMappingRepository abbreviationRepository;
    @Mock
    private BrandCanonicalizer brandCanonicalizer;

    private MerchantNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new MerchantNormalizer(ruleRepository, abbreviationRepository, brandCanonicalizer);

        when(ruleRepository.findAllByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(
                createRule("Expand TIMS", "^TIMS\\b", "TIM HORTONS", 5),
                createRule("Expand TIMHORTONS", "^TIMHORTONS\\b", "TIM HORTONS", 6),
                createRule("Strip terminal reference codes",
                        "\\s+(?=[A-Z0-9]*[0-9])(?=[A-Z0-9]*[A-Z])[A-Z0-9]{5,}$", "", 18),
                createRule("Strip store numbers", "\\s*#\\d+", "", 20),
                createRule("Strip trailing 3+ digits", "\\s+\\d{3,}$", "", 30),
                createRule("Strip geo suffixes", "\\s+(ON|BC|CA)$", "", 40),
                createRule("Strip trailing city names", "\\s+TORONTO\\s*$", "", 45)
        ));

        when(abbreviationRepository.findAll()).thenReturn(List.of(
                createAbbreviation("MKT", "MARKET"),
                createAbbreviation("XFER", "TRANSFER")
        ));

        lenient().when(brandCanonicalizer.canonicalize(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(0));

        normalizer.loadRules();
    }

    private NormalizationRule createRule(String name, String pattern, String replacement, int priority) {
        NormalizationRule rule = new NormalizationRule();
        rule.setRuleName(name);
        rule.setPattern(pattern);
        rule.setReplacement(replacement);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }

    private AbbreviationMapping createAbbreviation(String abbreviation, String expansion) {
        AbbreviationMapping mapping = new AbbreviationMapping();
        mapping.setAbbreviation(abbreviation);
        mapping.setExpansion(expansion);
        return mapping;
    }

    @Test
    void normalize_stripsStoreNumber() {
        assertEquals("WENDYS", normalizer.baseNormalize("Wendys #6758"));
    }

    @Test
    void normalize_stripsTerminalCode() {
        assertEquals("PRESTO FARE", normalizer.baseNormalize("Presto Fare Qr878m2vpq"));
    }

    @Test
    void normalize_doesNotStripMerchantWordsLikeHortons() {
        assertEquals("TIM HORTONS", normalizer.baseNormalize("TIM HORTONS TORONTO ON"));
    }

    @Test
    void normalize_expandsTimsToTimHortons() {
        assertEquals("TIM HORTONS", normalizer.baseNormalize("TIMS #4021 ON"));
    }

    @Test
    void normalize_expandsTimhortonsWithTerminalCode() {
        assertEquals("TIM HORTONS", normalizer.baseNormalize("TIMHORTONS 88KQ21"));
    }

    @Test
    void normalize_expandsAbbreviations() {
        assertEquals("AMZN MARKET", normalizer.baseNormalize("AMZN MKT CA"));
        assertEquals("BANK TRANSFER", normalizer.baseNormalize("BANK XFER"));
    }

    @Test
    void normalize_uppercasesAndTrims() {
        assertEquals("STARBUCKS", normalizer.baseNormalize("  starbucks  "));
    }

    @Test
    void normalize_handlesBlank() {
        assertEquals("", normalizer.normalize(""));
        assertEquals("", normalizer.normalize("   "));
        assertEquals("", normalizer.normalize(null));
    }

    @Test
    void normalize_appliesBrandCanonicalization() {
        when(brandCanonicalizer.canonicalize("TIM HORTONS")).thenReturn("TIM HORTONS");
        when(brandCanonicalizer.canonicalize("AMZN MKTP")).thenReturn("AMAZON");

        assertEquals("TIM HORTONS", normalizer.normalize("TIM HORTONS #2387 TORONTO"));
        assertEquals("AMAZON", normalizer.normalize("AMZN MKTP CA"));
    }
}
