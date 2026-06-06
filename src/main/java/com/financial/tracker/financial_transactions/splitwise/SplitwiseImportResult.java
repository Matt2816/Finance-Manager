package com.financial.tracker.financial_transactions.splitwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SplitwiseImportResult {
    private int imported;
    private int updated;
    private int deleted;
    private int skipped;

    private final List<TransactionSummary> importedTransactions = new ArrayList<>();
    private final List<TransactionSummary> updatedTransactions = new ArrayList<>();
    private final List<TransactionSummary> deletedTransactions = new ArrayList<>();
    private final List<TransactionSummary> skippedTransactions = new ArrayList<>();

    public static final SplitwiseImportResult EMPTY = new SplitwiseImportResult();

    public SplitwiseImportResult() {}

    public int imported() { return imported; }
    public int updated() { return updated; }
    public int deleted() { return deleted; }
    public int skipped() { return skipped; }

    public void addImported(TransactionSummary summary) {
        imported++;
        importedTransactions.add(summary);
    }

    public void addUpdated(TransactionSummary summary) {
        updated++;
        updatedTransactions.add(summary);
    }

    public void addDeleted(TransactionSummary summary) {
        deleted++;
        deletedTransactions.add(summary);
    }

    public void addSkipped(TransactionSummary summary) {
        skipped++;
        skippedTransactions.add(summary);
    }

    public List<TransactionSummary> getImportedTransactions() {
        return Collections.unmodifiableList(importedTransactions);
    }

    public List<TransactionSummary> getUpdatedTransactions() {
        return Collections.unmodifiableList(updatedTransactions);
    }

    public List<TransactionSummary> getDeletedTransactions() {
        return Collections.unmodifiableList(deletedTransactions);
    }

    public List<TransactionSummary> getSkippedTransactions() {
        return Collections.unmodifiableList(skippedTransactions);
    }

    public record TransactionSummary(String description, String amount, String date, String groupName, String reason) {}
}
