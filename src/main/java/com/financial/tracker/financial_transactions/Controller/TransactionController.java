package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.WalletNoteImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportService;
import com.financial.tracker.financial_transactions.Services.WalletNotesParser;
import jakarta.servlet.http.HttpServletRequest;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionsRepo transactionsRepo;
    private final WalletNotesImportService walletNotesImportService;
    private final WalletNotesParser walletNotesParser;

    public TransactionController(
            TransactionsRepo transactionsRepo,
            WalletNotesImportService walletNotesImportService
    ) {
        this.transactionsRepo = transactionsRepo;
        this.walletNotesImportService = walletNotesImportService;
        this.walletNotesParser = new WalletNotesParser();
    }

    /**
     * Quick connectivity check from a browser on your phone (GET, no body).
     */
    @GetMapping("/test")
    public TransactionTestResponse getTest(HttpServletRequest request) {
        ControllerRequestLogger.logReceived(log, "GET /api/transaction/test", request);
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? ""
                : ":" + request.getServerPort());
        return TransactionTestResponse.getHints(baseUrl);
    }

    /**
     * Dry-run POST from Shortcuts: echoes body, reports whether it parses.
     * Add ?save=true to actually write one transaction (for end-to-end tests).
     */
    @PostMapping(value = "/test", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<TransactionTestResponse> postTest(
            @RequestBody(required = false) String body,
            @RequestParam(defaultValue = "false") boolean save,
            HttpServletRequest request
    ) {
        ControllerRequestLogger.logReceived(log, "POST /api/transaction/test?save=" + save, body);
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
            WalletNoteImportResult importResult = walletNotesImportService.importSingleFromText(body);
            if ("created".equals(importResult.status()) || "duplicate".equals(importResult.status())) {
                Transaction saved = importResult.transaction();
                return ResponseEntity.ok(TransactionTestResponse.postResult(
                        clientAddress, body, saved, true, hints
                ));
            }
            return ResponseEntity.unprocessableEntity().body(
                    TransactionTestResponse.postResult(clientAddress, body, null, false, hints)
            );
        }

        return ResponseEntity.ok(
                TransactionTestResponse.postResult(clientAddress, body, parsed, false, hints)
        );
    }

    @GetMapping
    public List<Transaction> findAll(){
        ControllerRequestLogger.logReceived(log, "GET /api/transaction");
        return transactionsRepo.findAll();
    }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody Transaction transaction) {
        ControllerRequestLogger.logReceived(log, "POST /api/transaction", transaction);
        if (transactionsRepo.findByHash(transaction.getHash()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Transaction with the same hash already exists.");
        }

        transactionsRepo.save(transaction);
        return new ResponseEntity<>("Transaction created successfully.", HttpStatus.CREATED);
    }

    @PostMapping(value = "/import/wallet-notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WalletNotesImportResult> importWalletNotes(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ControllerRequestLogger.logReceived(log, "POST /api/transaction/import/wallet-notes", file);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        WalletNotesImportResult result = walletNotesImportService.importFromBytes(file.getBytes());
        return ResponseEntity.ok(result);
    }

    /**
     * Accepts one Apple Wallet note (plain text) from Shortcuts or other automations.
     * Body format matches a single block in testData.txt (Name, Merchant, Amount, Date, Location).
     */
    @PostMapping(value = "/import/wallet-note", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<WalletNoteImportResult> importWalletNote(@RequestBody String body) {
        ControllerRequestLogger.logReceived(log, "POST /api/transaction/import/wallet-note", body);
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Request body is empty"));
        }

        WalletNoteImportResult result = walletNotesImportService.importSingleFromText(body);

        return walletNoteImportResponse(result);
    }

    /**
     * Same insert rules as {@code POST /import/wallet-note}, but accepts JSON fields.
     */
    @PostMapping(value = "/import/wallet-note/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WalletNoteImportResult> importWalletNoteJson(
            @RequestBody WalletNoteJsonRequest body
    ) {
        ControllerRequestLogger.logReceived(log, "POST /api/transaction/import/wallet-note/json", body);
        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Request body is empty"));
        }

        WalletNoteImportResult result = walletNotesImportService.importSingleFromFields(
                body.name(),
                body.merchant(),
                body.amount(),
                body.date(),
                body.location()
        );

        return walletNoteImportResponse(result);
    }

    private static ResponseEntity<WalletNoteImportResult> walletNoteImportResponse(
            WalletNoteImportResult result
    ) {
        return switch (result.status()) {
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "duplicate" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.unprocessableEntity().body(result);
        };
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<String> deleteAllTransactions() {
        ControllerRequestLogger.logReceived(log, "DELETE /api/transaction");
        transactionsRepo.deleteAll();
        return ResponseEntity.ok("All transactions deleted successfully.");
    }
    @GetMapping(value = "/{hash}")
    public ResponseEntity<Transaction> getTransactionByHash(@PathVariable String hash) {
        ControllerRequestLogger.logReceived(log, "GET /api/transaction/{hash}", hash);
        Transaction transaction = transactionsRepo.findByHash(hash);
        if (transaction != null) {
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

}
