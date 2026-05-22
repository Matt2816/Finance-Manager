package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletNotesImportServiceTest {

    @Mock
    private TransactionsRepo transactionsRepo;

    private WalletNotesImportService importService;

    @BeforeEach
    void setUp() {
        importService = new WalletNotesImportService(transactionsRepo);
    }

    @Test
    void importSingleFromText_savesNewTransaction() {
        String note = """
                Name: Wendys 6758
                Merchant: Wendys 6758
                Amount: $18.06
                Date: June 27, 2025 at 8:37:02 PM EDT
                Location: 370 King St W
                Toronto ON M5V 1J9
                Canada
                """;

        when(transactionsRepo.findByHash(any())).thenReturn(null);

        WalletNoteImportResult result = importService.importSingleFromText(note);

        assertEquals("created", result.status());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionsRepo).save(captor.capture());
        assertEquals("18.06", captor.getValue().getAmount());
        assertEquals("Wendys 6758", captor.getValue().getName());
    }

    @Test
    void importSingleFromFields_savesNewTransaction() {
        when(transactionsRepo.findByHash(any())).thenReturn(null);

        WalletNoteImportResult result = importService.importSingleFromFields(
                "Wendys 6758",
                "Wendys 6758",
                "$18.06",
                "June 27, 2025 at 8:37:02 PM EDT",
                "370 King St W\nToronto ON M5V 1J9\nCanada"
        );

        assertEquals("created", result.status());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionsRepo).save(captor.capture());
        assertEquals("18.06", captor.getValue().getAmount());
        assertEquals("2025-06-27", captor.getValue().getTransactionDate());
    }

    @Test
    void importSingleFromText_returnsDuplicateWhenHashExists() {
        String note = """
                Name: Test Shop
                Merchant: Test Shop
                Amount: $9.99
                Date: July 1, 2025 at 12:00:00 PM EDT
                Location: 1 Main St
                Canada
                """;

        Transaction existing = new Transaction();
        existing.setHash("existing");
        when(transactionsRepo.findByHash(any())).thenReturn(existing);

        WalletNoteImportResult result = importService.importSingleFromText(note);

        assertEquals("duplicate", result.status());
        verify(transactionsRepo, never()).save(any());
    }
}
