package com.financial.tracker.financial_transactions.analytics.normalization;

import com.financial.tracker.financial_transactions.analytics.merchant.MerchantNormalizer;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionNormalizationServiceTest {

    @Mock
    private TransactionsRepo transactionsRepo;

    @Mock
    private NormalizedTransactionRepository normalizedRepo;

    @Mock
    private MerchantNormalizer merchantNormalizer;

    @InjectMocks
    private TransactionNormalizationService service;

    private Transaction splitwiseTransaction;

    @BeforeEach
    void setUp() {
        splitwiseTransaction = new Transaction();
        splitwiseTransaction.setId(1);
        splitwiseTransaction.setUserId(42L);
        splitwiseTransaction.setCardType("splitwise");
        splitwiseTransaction.setName("Costco");
        splitwiseTransaction.setMerchant("Splitwise: Niagara Neighbours");
        splitwiseTransaction.setAmountValue(new BigDecimal("10.00"));
        splitwiseTransaction.setOccurredOn(LocalDate.of(2024, 6, 15));
    }

    @Test
    void splitwiseUsesDescriptionForMerchantRaw() {
        when(transactionsRepo.findById(1)).thenReturn(Optional.of(splitwiseTransaction));
        when(normalizedRepo.findByTransactionId(1)).thenReturn(Optional.empty());
        when(merchantNormalizer.normalize("Costco")).thenReturn("COSTCO");
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(normalizedRepo.save(any(NormalizedTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<NormalizedTransaction> result = service.normalizeOne(1);

        assertTrue(result.isPresent());
        assertEquals("Costco", result.get().getMerchantRaw());
        assertEquals("COSTCO", result.get().getMerchantKey());
    }
}
