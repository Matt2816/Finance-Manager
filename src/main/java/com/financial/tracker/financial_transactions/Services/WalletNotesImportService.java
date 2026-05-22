package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class WalletNotesImportService {

    private final WalletNotesParser parser = new WalletNotesParser();
    private final TransactionsRepo transactionsRepo;

    public WalletNotesImportService(TransactionsRepo transactionsRepo) {
        this.transactionsRepo = transactionsRepo;
    }

    public WalletNotesImportResult importFromText(String content) {
        WalletNotesParser.ParseResult parseResult = parser.parse(content);
        List<Transaction> parsed = parseResult.transactions();
        int imported = 0;
        int skippedDuplicates = 0;

        for (Transaction transaction : parsed) {
            if (transactionsRepo.findByHash(transaction.getHash()) != null) {
                skippedDuplicates++;
                continue;
            }
            transactionsRepo.save(transaction);
            imported++;
        }

        return new WalletNotesImportResult(
                imported,
                parseResult.skippedBlocks(),
                skippedDuplicates,
                parsed.size()
        );
    }

    public WalletNotesImportResult importFromBytes(byte[] bytes) throws IOException {
        String content = new String(bytes, StandardCharsets.UTF_8);
        return importFromText(content);
    }

    public WalletNoteImportResult importSingleFromText(String content) {
        Transaction transaction = parser.parseSingle(content);
        if (transaction == null) {
            return WalletNoteImportResult.skipped(
                    "Could not parse a transaction (missing or invalid Amount)."
            );
        }

        return saveIfNew(transaction);
    }

    public WalletNoteImportResult importSingleFromFields(
            String name,
            String merchant,
            String amount,
            String date,
            String location
    ) {
        Transaction transaction = parser.buildFromFields(name, merchant, amount, date, location);
        if (transaction == null) {
            return WalletNoteImportResult.skipped(
                    "Could not build a transaction (missing or invalid amount or date)."
            );
        }

        return saveIfNew(transaction);
    }

    private WalletNoteImportResult saveIfNew(Transaction transaction) {
        Transaction existing = transactionsRepo.findByHash(transaction.getHash());
        if (existing != null) {
            return WalletNoteImportResult.duplicate(existing);
        }

        transactionsRepo.save(transaction);
        return WalletNoteImportResult.created(transaction);
    }
}
