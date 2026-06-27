package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.Controller.WalletNoteJsonRequest;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletNotesImportService {

    private static final Logger log = LoggerFactory.getLogger(WalletNotesImportService.class);

    private final WalletNotesParser parser = new WalletNotesParser();
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final TransactionImportPipeline importPipeline;
    private final NormalizedTransactionRepository normalizedRepository;

    public WalletNotesImportService(
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            TransactionImportPipeline importPipeline,
            NormalizedTransactionRepository normalizedRepository
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.importPipeline = importPipeline;
        this.normalizedRepository = normalizedRepository;
    }

    public WalletNotesImportResult importFromText(String content, Long userId) {
        log.info("importFromText: starting batch import, contentLength={}", content == null ? 0 : content.length());
        WalletNotesParser.ParseResult parseResult = parser.parse(content);
        List<Transaction> parsed = parseResult.transactions();
        int imported = 0;
        int skippedDuplicates = 0;

        for (Transaction transaction : parsed) {
            if (transactionsRepo.findByHashAndUserId(transaction.getHash(), userId) != null) {
                skippedDuplicates++;
                log.debug("importFromText: duplicate hash={}, skipping", transaction.getHash());
                continue;
            }
            transaction.setUserId(userId);
            Transaction saved = transactionsRepo.save(transaction);
            normalizationService.normalizeOne(saved.getId());
            imported++;
            log.debug(
                    "importFromText: saved name={}, amount={}, hash={}",
                    transaction.getName(),
                    transaction.getAmount(),
                    transaction.getHash()
            );
        }

        WalletNotesImportResult result = new WalletNotesImportResult(
                imported,
                parseResult.skippedBlocks(),
                skippedDuplicates,
                parsed.size()
        );
        log.info("importFromText: result={}", result);
        return result;
    }

    public WalletNotesImportResult importFromBytes(byte[] bytes, Long userId) throws IOException {
        log.info("importFromBytes: size={}", bytes.length);
        String content = new String(bytes, StandardCharsets.UTF_8);
        return importFromText(content, userId);
    }

    public WalletNoteImportResult importSingleFromText(String content, Long userId) {
        log.info("importSingleFromText: contentLength={}", content == null ? 0 : content.length());
        Transaction transaction = parser.parseSingle(content);
        if (transaction == null) {
            WalletNoteImportResult result = WalletNoteImportResult.skipped(
                    "Could not parse a transaction (missing or invalid Amount)."
            );
            log.info("importSingleFromText: result={}", result);
            return result;
        }

        return saveIfNew(transaction, "importSingleFromText", userId);
    }

    public WalletNoteImportResult importSingleFromFields(
            String name,
            String merchant,
            String amount,
            String date,
            String location,
            Long userId
    ) {
        log.info(
                "importSingleFromFields: name={}, merchant={}, amount={}, date={}",
                name,
                merchant,
                amount,
                date
        );
        Transaction transaction = parser.buildFromFields(name, merchant, amount, date, location);
        if (transaction == null) {
            WalletNoteImportResult result = WalletNoteImportResult.skipped(
                    "Could not build a transaction (missing or invalid amount or date)."
            );
            log.info("importSingleFromFields: result={}", result);
            return result;
        }

        return saveIfNew(transaction, "importSingleFromFields", userId);
    }

    /**
     * Batch import of JSON wallet-note items (e.g. drained from a PWA offline queue
     * or an Apple Shortcuts queue). Each item is processed independently so a single
     * bad item never fails the whole batch. Items that are created or already exist
     * (duplicate) are considered safe to drain from the caller's queue.
     */
    public WalletNotesBatchImportResult importBatchFromFields(
            List<WalletNoteJsonRequest> items,
            Long userId
    ) {
        int created = 0;
        int duplicates = 0;
        int skipped = 0;
        List<WalletNotesBatchItemResult> results = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            WalletNoteJsonRequest item = items.get(i);
            WalletNoteImportResult result = importSingleFromFields(
                    item.name(),
                    item.merchant(),
                    item.amount(),
                    item.date(),
                    item.location(),
                    userId
            );

            String hash = result.transaction() != null ? result.transaction().getHash() : null;
            switch (result.status()) {
                case "created" -> {
                    created++;
                    if (item.categoryId() != null && result.transaction() != null) {
                        applyCategory(result.transaction(), item.categoryId().longValue(), userId);
                    }
                }
                case "duplicate" -> duplicates++;
                default -> skipped++;
            }

            results.add(new WalletNotesBatchItemResult(i, result.status(), result.message(), hash));
        }

        WalletNotesBatchImportResult batchResult = new WalletNotesBatchImportResult(
                items.size(), created, duplicates, skipped, results
        );
        log.info("importBatchFromFields: result={}", batchResult);
        return batchResult;
    }

    private void applyCategory(Transaction transaction, Long categoryId, Long userId) {
        transaction.setCategoryId(categoryId);
        transactionsRepo.save(transaction);
        NormalizedTransaction normalized =
                normalizedRepository.findByTransactionIdAndUserId(transaction.getId(), userId).orElse(null);
        if (normalized != null) {
            normalized.setCategoryId(categoryId);
            normalizedRepository.save(normalized);
        }
    }

    private WalletNoteImportResult saveIfNew(Transaction transaction, String operation, Long userId) {
        log.info("saveIfNew: hash={}, name={}, amount={}", transaction.getHash(), transaction.getName(), transaction.getAmount());
        Transaction existing = transactionsRepo.findByHashAndUserId(transaction.getHash(), userId);
        if (existing != null) {
            WalletNoteImportResult result = WalletNoteImportResult.duplicate(existing);
            log.info("{}: result={}", operation, result);
            return result;
        }

        Transaction saved = importPipeline.saveNew(transaction, userId);
        WalletNoteImportResult result = WalletNoteImportResult.created(saved);
        log.info("{}: result={}", operation, result);
        return result;
    }
}
