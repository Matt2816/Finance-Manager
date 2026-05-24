package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantCategoryRuleRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantIntelligenceEngineTest {

    @Mock
    private NormalizedTransactionRepository normalizedRepo;
    @Mock
    private MerchantCategoryRuleRepository ruleRepository;
    @Mock
    private SpendingCategoryRepository categoryRepository;
    @Mock
    private MerchantResolutionPipeline resolutionPipeline;

    private MerchantIntelligenceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MerchantIntelligenceEngine(
                normalizedRepo, ruleRepository, categoryRepository, resolutionPipeline
        );
    }

    @Test
    void processAll_assignsCategoryFromRule() {
        SpendingCategory coffee = new SpendingCategory();
        coffee.setId(1L);
        coffee.setSlug("coffee");

        MerchantCategoryRule rule = new MerchantCategoryRule();
        rule.setPattern("(?i)starbucks");
        rule.setCategoryId(1L);
        rule.setPriority(10);

        NormalizedTransaction tx = new NormalizedTransaction();
        tx.setMerchantRaw("Starbucks King St");
        tx.setMerchantKey("starbucks king st");
        tx.setAmount(BigDecimal.TEN);
        tx.setOccurredOn(LocalDate.now());
        tx.setDirection(TransactionDirection.DEBIT);

        when(normalizedRepo.findAll()).thenReturn(List.of(tx));
        when(ruleRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of(rule));
        when(categoryRepository.findBySlug("uncategorized")).thenReturn(Optional.empty());
        when(resolutionPipeline.resolve(any())).thenReturn(Optional.empty());
        when(normalizedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.processAll();

        assertEquals(1L, tx.getCategoryId());
        verify(normalizedRepo).save(tx);
    }
}
