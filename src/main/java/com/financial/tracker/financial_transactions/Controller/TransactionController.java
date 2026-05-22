package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.WalletNoteImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportService;
import com.financial.tracker.financial_transactions.Services.WalletNotesParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public TransactionController(
            TransactionsRepo transactionsRepo,
            WalletNotesImportService walletNotesImportService,
            ObjectMapper objectMapper
    ) {
        this.transactionsRepo = transactionsRepo;
        this.walletNotesImportService = walletNotesImportService;
        this.walletNotesParser = new WalletNotesParser();
        this.objectMapper = objectMapper;
    }

    /**
     * Quick connectivity check from a browser on your phone (GET, no body).
     */
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
    @PostMapping(value = "/test", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<TransactionTestResponse> postTest(
            @RequestBody(required = false) String body,
            @RequestParam(defaultValue = "false") boolean save,
            HttpServletRequest request
    ) {
        ControllerRequestLogger.logIncoming(log, "postTest", "save", save, "body", body);
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
        return ControllerRequestLogger.logResponseBody(log, "findAll", transactionsRepo.findAll());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody Transaction transaction) {
        ControllerRequestLogger.logIncoming(log, "createTransaction", transaction);
        if (transactionsRepo.findByHash(transaction.getHash()) != null) {
            return ControllerRequestLogger.logResponse(log, "createTransaction",
                    ResponseEntity.status(HttpStatus.CONFLICT).body("Transaction with the same hash already exists."));
        }

        transactionsRepo.save(transaction);
        return ControllerRequestLogger.logResponse(log, "createTransaction",
                new ResponseEntity<>("Transaction created successfully.", HttpStatus.CREATED));
    }

    @PostMapping(value = "/import/wallet-notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WalletNotesImportResult> importWalletNotes(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ControllerRequestLogger.logIncoming(log, "importWalletNotes", "file", file);
        if (file.isEmpty()) {
            return ControllerRequestLogger.logResponse(log, "importWalletNotes", ResponseEntity.badRequest().build());
        }
        WalletNotesImportResult result = walletNotesImportService.importFromBytes(file.getBytes());
        return ControllerRequestLogger.logResponse(log, "importWalletNotes", ResponseEntity.ok(result));
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

        WalletNoteImportResult result = walletNotesImportService.importSingleFromText(body);

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

        WalletNoteImportResult result = walletNotesImportService.importSingleFromFields(
                request.name(),
                request.merchant(),
                request.amount(),
                request.date(),
                request.location()
        );

        return ControllerRequestLogger.logResponse(log, "importWalletNoteJson", switch (result.status()) {
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "duplicate" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.unprocessableEntity().body(result);
        });
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<String> deleteAllTransactions() {
        ControllerRequestLogger.logIncoming(log, "deleteAllTransactions");
        transactionsRepo.deleteAll();
        return ControllerRequestLogger.logResponse(log, "deleteAllTransactions",
                ResponseEntity.ok("All transactions deleted successfully."));
    }

    @GetMapping(value = "/{hash}")
    public ResponseEntity<Transaction> getTransactionByHash(@PathVariable String hash) {
        ControllerRequestLogger.logIncoming(log, "getTransactionByHash", "hash", hash);
        Transaction transaction = transactionsRepo.findByHash(hash);
        if (transaction != null) {
            return ControllerRequestLogger.logResponse(log, "getTransactionByHash", ResponseEntity.ok(transaction));
        }
        return ControllerRequestLogger.logResponse(log, "getTransactionByHash",
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

}
