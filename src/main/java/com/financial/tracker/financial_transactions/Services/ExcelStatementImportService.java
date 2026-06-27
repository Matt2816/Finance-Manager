package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.util.TransactionExclusionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelStatementImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelStatementImportService.class);

    private final ExcelStatementParser parser;
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;

    public ExcelStatementImportService(
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            TransactionExclusionFilter exclusionFilter
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.parser = new ExcelStatementParser(exclusionFilter);
    }

    public ExcelStatementImportResult importFromBytes(byte[] bytes, Long userId) throws IOException {
        log.info("importFromBytes: size={}", bytes.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ExcelStatementParser.ParseResult parseResult = parser.parse(inputStream);
        List<Transaction> parsed = parseResult.transactions();
        List<ExcelStatementImportResult.ImportedTransaction> importedTransactions = new ArrayList<>();
        int imported = 0;
        int skippedDuplicates = 0;

        for (Transaction transaction : parsed) {
            if (transactionsRepo.findByHashAndUserId(transaction.getHash(), userId) != null) {
                skippedDuplicates++;
                log.debug("importFromBytes: duplicate hash={}, skipping", transaction.getHash());
                continue;
            }
            transaction.setUserId(userId);
            Transaction saved = transactionsRepo.save(transaction);
            normalizationService.normalizeOne(saved.getId());
            importedTransactions.add(new ExcelStatementImportResult.ImportedTransaction(
                    saved.getName(),
                    saved.getMerchant(),
                    saved.getAmount(),
                    saved.getTransactionDate()
            ));
            imported++;
            log.debug(
                    "importFromBytes: saved name={}, amount={}, hash={}",
                    transaction.getName(),
                    transaction.getAmount(),
                    transaction.getHash()
            );
        }

        ExcelStatementImportResult result = new ExcelStatementImportResult(
                imported,
                parseResult.skippedRows(),
                parseResult.excludedRows(),
                skippedDuplicates,
                parsed.size(),
                importedTransactions
        );
        log.info("importFromBytes: result={}", result);
        return result;
    }
}
