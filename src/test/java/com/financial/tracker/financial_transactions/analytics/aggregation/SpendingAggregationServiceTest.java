package com.financial.tracker.financial_transactions.analytics.aggregation;

import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendingAggregationServiceTest {

    @Mock
    private NormalizedTransactionRepository normalizedRepo;
    @Mock
    private SpendingSnapshotRepository snapshotRepository;

    private SpendingAggregationService service;

    @BeforeEach
    void setUp() {
        service = new SpendingAggregationService(normalizedRepo, snapshotRepository);
        ReflectionTestUtils.setField(service, "snapshotMonths", 24);
    }

    @Test
    void rebuildSnapshots_createsMonthlySnapshot() {
        Long userId = 1L;
        NormalizedTransaction tx = new NormalizedTransaction();
        tx.setUserId(userId);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setOccurredOn(LocalDate.of(2025, 6, 15));
        tx.setCategoryId(1L);
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setRecurringGenerated(false);

        when(normalizedRepo.findByUserIdAndOccurredOnBetween(eq(userId), any(), any())).thenReturn(List.of(tx));
        when(snapshotRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.rebuildSnapshots(userId);

        assertTrue(count > 0);
        verify(snapshotRepository).deleteByUserId(userId);
        ArgumentCaptor<Iterable<SpendingSnapshot>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(snapshotRepository).saveAll(captor.capture());
        boolean hasMonthly = false;
        for (SpendingSnapshot s : captor.getValue()) {
            if (s.getGrain() == SnapshotGrain.MONTHLY && Long.valueOf(1L).equals(s.getCategoryId())) {
                hasMonthly = true;
                assertEquals(0, new BigDecimal("50.00").compareTo(s.getTotalAmount()));
            }
        }
        assertTrue(hasMonthly);
    }
}
