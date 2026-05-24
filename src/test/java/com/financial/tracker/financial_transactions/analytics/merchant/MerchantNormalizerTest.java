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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantNormalizerTest {

    @Mock
    private NormalizationRuleRepository ruleRepository;
    @Mock
    private AbbreviationMappingRepository abbreviationRepository;

    private MerchantNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new MerchantNormalizer(ruleRepository, abbreviationRepository);

        when(ruleRepository.findAllByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(
                createRule("Strip terminal reference codes", "\\s+[A-Z0-9]{5,}$", "", 10),
                createRule("Strip store numbers", "\\s*#\\d+", "", 20),
                createRule("Strip trailing 3+ digits", "\\s+\\d{3,}$", "", 30),
                createRule("Strip geo suffixes", "\\s+(ON|BC|CA)$", "", 40)
        ));

        when(abbreviationRepository.findAll()).thenReturn(List.of(
                createAbbreviation("MKT", "MARKET"),
                createAbbreviation("XFER", "TRANSFER")
        ));

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
        assertEquals("WENDYS", normalizer.normalize("Wendys #6758"));
    }

    @Test
    void normalize_stripsTerminalCode() {
        assertEquals("PRESTO FARE", normalizer.normalize("Presto Fare Qr878m2vpq"));
    }

    @Test
    void normalize_stripsGeoSuffix() {
        // Geo suffix "ON" is stripped; city name "TORONTO" remains (needs separate city rule)
        assertEquals("TIM HORTONS TORONTO", normalizer.normalize("Tim Hortons Toronto ON"));
    }

    @Test
    void normalize_expandsAbbreviations() {
        // "CA" is stripped as geo suffix before abbreviation expansion
        assertEquals("AMZN MARKET", normalizer.normalize("AMZN MKT CA"));
        assertEquals("BANK TRANSFER", normalizer.normalize("BANK XFER"));
    }

    @Test
    void normalize_uppercasesAndTrims() {
        assertEquals("STARBUCKS", normalizer.normalize("  starbucks  "));
    }

    @Test
    void normalize_handlesBlank() {
        assertEquals("", normalizer.normalize(""));
        assertEquals("", normalizer.normalize("   "));
        assertEquals("", normalizer.normalize(null));
    }
}
