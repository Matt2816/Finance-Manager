package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.WalletNoteImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportResult;
import com.financial.tracker.financial_transactions.Services.WalletNotesImportService;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
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

    private final TransactionsRepo transactionsRepo;
    private final WalletNotesImportService walletNotesImportService;

    public TransactionController(
            TransactionsRepo transactionsRepo,
            WalletNotesImportService walletNotesImportService
    ) {
        this.transactionsRepo = transactionsRepo;
        this.walletNotesImportService = walletNotesImportService;
    }

    @GetMapping
    public List<Transaction> findAll(){
        return transactionsRepo.findAll();
    }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody Transaction transaction) {
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
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(WalletNoteImportResult.skipped("Request body is empty"));
        }

        WalletNoteImportResult result = walletNotesImportService.importSingleFromText(body);

        return switch (result.status()) {
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "duplicate" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.unprocessableEntity().body(result);
        };
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<String> deleteAllTransactions() {
        transactionsRepo.deleteAll();
        return ResponseEntity.ok("All transactions deleted successfully.");
    }
    @GetMapping(value = "/{hash}")
    public ResponseEntity<Transaction> getTransactionByHash(@PathVariable String hash) {
        Transaction transaction = transactionsRepo.findByHash(hash);
        if (transaction != null) {
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

}
