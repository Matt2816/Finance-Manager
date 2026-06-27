package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.ExcelStatementImportResult;
import com.financial.tracker.financial_transactions.Services.ExcelStatementImportService;
import com.financial.tracker.financial_transactions.Services.WalletNoteImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesBatchImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportService;
import com.financial.tracker.financial_transactions.Services.WalletNotesParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionsRepo transactionsRepo;
    private final NormalizedTransactionRepository normalizedRepo;
    private final WalletNotesImportService walletNotesImportService;
    private final WalletNotesParser walletNotesParser;
    private final ExcelStatementImportService excelStatementImportService;
    private final ObjectMapper objectMapper;

    public TransactionController(
            TransactionsRepo transactionsRepo,
            NormalizedTransactionRepository normalizedRepo,
            WalletNotesImportService walletNotesImportService,
            ExcelStatementImportService excelStatementImportService,
            ObjectMapper objectMapper
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizedRepo = normalizedRepo;
        this.walletNotesImportService = walletNotesImportService;
        this.excelStatementImportService = excelStatementImportService;
        this.walletNotesParser = new WalletNotesParser();
        this.objectMapper = objectMapper;
    }

    /**
     * Quick connectivity check from a browser on your phone (GET, no body).
     */
    @Profile("dev")
    @GetMapping("/test")
    public TransactionTestResponse getTest(HttpServletRequest request) {
        ControllerRequestLogger.logIncoming(log, "getTest");
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? ""
                : ":" + request.getServerPort());
        return ControllerRequestLogger.logResponseBody(log, "getTest", TransactionTestResponse.getHints(baseUrl));
    }

    /**
     * Dry-run POST from Shortcuts: echoes body, reports whether it parses.
     * Add ?save=true to actually write one transaction (for end-to-end tests).
     */
    @Profile("dev")
    @PostMapping(value = "/test", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<TransactionTestResponse> postTest(
            @RequestBody(required = false) String body,
            @RequestParam(defaultValue = "false") boolean save,
            HttpServletRequest request
    ) {
        ControllerRequestLogger.logIncoming(log, "postTest", "save", save, "body", body);
        Long userId = SecurityUtils.getCurrentUserId();
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? ""
                : ":" + request.getServerPort());
        TransactionTestResponse.WalletNoteTestHints hints = new TransactionTestResponse.WalletNoteTestHints(
                baseUrl + "/api/transaction/import/wallet-note",
                "text/plain",
                null
        );

        Transaction parsed = body == null || body.isBlank() ? null : walletNotesParser.parseSingle(body);
        String clientAddress = HealthController.clientAddress(request);

        if (save && parsed != null) {
            WalletNoteImportResult importResult = walletNotesImportService.importSingleFromText(body, userId);
            if ("created".equals(importResult.status()) || "duplicate".equals(importResult.status())) {
                Transaction saved = importResult.transaction();
                return ControllerRequestLogger.logResponse(log, "postTest", ResponseEntity.ok(
                        TransactionTestResponse.postResult(clientAddress, body, saved, true, hints)
                ));
            }
            return ControllerRequestLogger.logResponse(log, "postTest", ResponseEntity.unprocessableEntity().body(
                    TransactionTestResponse.postResult(clientAddress, body, null, false, hints)
            ));
        }

        return ControllerRequestLogger.logResponse(log, "postTest", ResponseEntity.ok(
                TransactionTestResponse.postResult(clientAddress, body, parsed, false, hints)
        ));
    }

    @GetMapping
    public List<Transaction> findAll() {
        ControllerRequestLogger.logIncoming(log, "findAll");
        Long userId = SecurityUtils.getCurrentUserId();
        return ControllerRequestLogger.logResponseBody(log, "findAll", transactionsRepo.findByUserId(userId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody Transaction transaction) {
        ControllerRequestLogger.logIncoming(log, "createTransaction", transaction);
        Long userId = SecurityUtils.getCurrentUserId();
        if (transactionsRepo.findByHashAndUserId(transaction.getHash(), userId) != null) {
            return ControllerRequestLogger.logResponse(log, "createTransaction",
                    ResponseEntity.status(HttpStatus.CONFLICT).body("Transaction with the same hash already exists."));
        }

        transaction.setUserId(userId);
        transactionsRepo.save(transaction);
        return ControllerRequestLogger.logResponse(log, "createTransaction",
                new ResponseEntity<>("Transaction created successfully.", HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTransaction(
            @PathVariable int id,
            @RequestBody Transaction transaction) {
        ControllerRequestLogger.logIncoming(log, "updateTransaction", "id", id, "body", transaction);
        Long userId = SecurityUtils.getCurrentUserId();
        return transactionsRepo.findByIdAndUserId(id, userId)
                .map(existing -> {
                    existing.setName(transaction.getName());
                    existing.setMerchant(transaction.getMerchant());
                    existing.setAmount(transaction.getAmount());
                    existing.setCardType(transaction.getCardType());
                    existing.setTransactionDate(transaction.getTransactionDate());
                    existing.setAddress(transaction.getAddress());
                    transactionsRepo.save(existing);
                    return ControllerRequestLogger.logResponse(log, "updateTransaction",
                            ResponseEntity.ok("Transaction updated successfully."));
                })
                .orElseGet(() -> ControllerRequestLogger.logResponse(log, "updateTransaction",
                        ResponseEntity.notFound().build()));
    }

    @PostMapping(value = "/import/wallet-notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WalletNotesImportResult> importWalletNotes(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ControllerRequestLogger.logIncoming(log, "importWalletNotes", "file", file);
        if (file.isEmpty()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNotes", ResponseEntity.badRequest().build());
        }
        Long userId = SecurityUtils.getCurrentUserId();
        WalletNotesImportResult result = walletNotesImportService.importFromBytes(file.getBytes(), userId);
        return ControllerRequestLogger.logResponse(log, "importWalletNotes", ResponseEntity.ok(result));
    }

    /**
     * Accepts an Excel statement file (.xlsx) with transaction data.
     * Headers start at line 12, transactions start at line 13.
     * Column A: Date, Column C: Description, Column D: Amount
     */
    @PostMapping(value = "/import/excel-statement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelStatementImportResult> importExcelStatement(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ControllerRequestLogger.logIncoming(log, "importExcelStatement", "file", file);
        if (file.isEmpty()) {
            return ControllerRequestLogger.logResponse(log, "importExcelStatement", ResponseEntity.badRequest().build());
        }
        Long userId = SecurityUtils.getCurrentUserId();
        ExcelStatementImportResult result = excelStatementImportService.importFromBytes(file.getBytes(), userId);
        return ControllerRequestLogger.logResponse(log, "importExcelStatement", ResponseEntity.ok(result));
    }

    /**
     * Accepts one Apple Wallet note (plain text) from Shortcuts or other automations.
     * Body format matches a single block in testData.txt (Name, Merchant, Amount, Date, Location).
     */
    @PostMapping(value = "/import/wallet-note", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<WalletNoteImportResult> importWalletNote(@RequestBody String body) {
        ControllerRequestLogger.logIncoming(log, "importWalletNote", "body", body);
        if (body == null || body.isBlank()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNote", ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Request body is empty")));
        }

        Long userId = SecurityUtils.getCurrentUserId();
        WalletNoteImportResult result = walletNotesImportService.importSingleFromText(body, userId);

        return ControllerRequestLogger.logResponse(log, "importWalletNote", switch (result.status()) {
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "duplicate" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.unprocessableEntity().body(result);
        });
    }

    /**
     * Accepts one Wallet note as JSON (name, merchant, amount, date, location).
     * Uses the same normalization, hashing, and duplicate detection as {@code /import/wallet-note}.
     */
    @PostMapping(value = "/import/wallet-note-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalletNoteImportResult> importWalletNoteJson(@RequestBody String jsonPayload) {
        ControllerRequestLogger.logJsonPayload(log, "importWalletNoteJson", jsonPayload);

        if (jsonPayload == null || jsonPayload.isBlank()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNoteJson", ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Request body is required")));
        }

        WalletNoteJsonRequest request;
        try {
            request = objectMapper.readValue(jsonPayload, WalletNoteJsonRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("importWalletNoteJson: invalid JSON payload", e);
            return ControllerRequestLogger.logResponse(log, "importWalletNoteJson", ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Invalid JSON: " + e.getOriginalMessage())));
        }
        ControllerRequestLogger.logIncoming(log, "importWalletNoteJson", "parsed", request);

        Long userId = SecurityUtils.getCurrentUserId();
        WalletNoteImportResult result = walletNotesImportService.importSingleFromFields(
                request.name(),
                request.merchant(),
                request.amount(),
                request.date(),
                request.location(),
                userId
        );

        return ControllerRequestLogger.logResponse(log, "importWalletNoteJson", switch (result.status()) {
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "duplicate" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.unprocessableEntity().body(result);
        });
    }

    /**
     * Batch import of wallet-note JSON items. Accepts a JSON array of objects
     * (name, merchant, amount, date, location, categoryId). Used to drain a PWA
     * offline queue or an Apple Shortcuts queue in a single request. Each item is
     * processed independently; the response reports per-item status so the caller
     * can drain created and duplicate items from its queue.
     */
    @PostMapping(value = "/import/wallet-notes-batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalletNotesBatchImportResult> importWalletNotesBatch(@RequestBody String jsonPayload) {
        ControllerRequestLogger.logJsonPayload(log, "importWalletNotesBatch", jsonPayload);

        if (jsonPayload == null || jsonPayload.isBlank()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNotesBatch", ResponseEntity.badRequest()
                    .body(new WalletNotesBatchImportResult(0, 0, 0, 0, java.util.List.of())));
        }

        List<WalletNoteJsonRequest> items;
        try {
            items = objectMapper.readValue(
                    jsonPayload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WalletNoteJsonRequest.class)
            );
        } catch (JsonProcessingException e) {
            log.warn("importWalletNotesBatch: invalid JSON payload", e);
            return ControllerRequestLogger.logResponse(log, "importWalletNotesBatch", ResponseEntity.badRequest()
                    .body(new WalletNotesBatchImportResult(0, 0, 0, 0, java.util.List.of())));
        }

        if (items == null || items.isEmpty()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNotesBatch", ResponseEntity.ok(
                    new WalletNotesBatchImportResult(0, 0, 0, 0, java.util.List.of())));
        }

        Long userId = SecurityUtils.getCurrentUserId();
        WalletNotesBatchImportResult result = walletNotesImportService.importBatchFromFields(items, userId);
        return ControllerRequestLogger.logResponse(log, "importWalletNotesBatch", ResponseEntity.ok(result));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> deleteTransaction(@PathVariable int id) {
        ControllerRequestLogger.logIncoming(log, "deleteTransaction", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        return transactionsRepo.findByIdAndUserId(id, userId)
                .map(existing -> {
                    transactionsRepo.deleteById(id);
                    normalizedRepo.deleteByTransactionIdAndUserId(id, userId);
                    return ControllerRequestLogger.logResponse(log, "deleteTransaction",
                            ResponseEntity.ok("Transaction deleted successfully."));
                })
                .orElseGet(() -> ControllerRequestLogger.logResponse(log, "deleteTransaction",
                        ResponseEntity.notFound().build()));
    }

    @Profile("dev")
    @DeleteMapping
    @ResponseBody
    @Transactional
    public ResponseEntity<String> deleteAllTransactions() {
        ControllerRequestLogger.logIncoming(log, "deleteAllTransactions");
        Long userId = SecurityUtils.getCurrentUserId();
        transactionsRepo.deleteByUserId(userId);
        normalizedRepo.deleteByUserId(userId);
        return ControllerRequestLogger.logResponse(log, "deleteAllTransactions",
                ResponseEntity.ok("All transactions deleted successfully."));
    }

    @GetMapping(value = "/{hash}")
    public ResponseEntity<Transaction> getTransactionByHash(@PathVariable String hash) {
        ControllerRequestLogger.logIncoming(log, "getTransactionByHash", "hash", hash);
        Long userId = SecurityUtils.getCurrentUserId();
        Transaction transaction = transactionsRepo.findByHashAndUserId(hash, userId);
        if (transaction != null) {
            return ControllerRequestLogger.logResponse(log, "getTransactionByHash", ResponseEntity.ok(transaction));
        }
        return ControllerRequestLogger.logResponse(log, "getTransactionByHash", ResponseEntity.notFound().build());
    }

}
