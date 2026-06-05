package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class WalletNotesImportService {

    private static final Logger log = LoggerFactory.getLogger(WalletNotesImportService.class);

    private final WalletNotesParser parser = new WalletNotesParser();
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final TransactionImportPipeline importPipeline;

    public WalletNotesImportService(
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            TransactionImportPipeline importPipeline
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.importPipeline = importPipeline;
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
