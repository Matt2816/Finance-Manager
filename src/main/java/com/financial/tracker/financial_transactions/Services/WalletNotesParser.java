package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletNotesParser {

    private static final Pattern FIELD_LINE = Pattern.compile(
            "^(Name|Merchant|Amount|Date|Location):\\s*(.*)$"
    );
    private static final DateTimeFormatter WALLET_DATE = DateTimeFormatter.ofPattern(
            "MMMM d, yyyy 'at' h:mm:ss a",
            Locale.ENGLISH
    );

    public ParseResult parse(String content) {
        List<Transaction> transactions = new ArrayList<>();
        int skippedBlocks = 0;

        for (String block : content.split("-{28,}")) {
            if (block.isBlank()) {
                continue;
            }
            Transaction transaction = parseBlock(block);
            if (transaction != null) {
                transactions.add(transaction);
            } else if (block.contains("Amount:")) {
                skippedBlocks++;
            }
        }

        return new ParseResult(transactions, skippedBlocks);
    }

    public record ParseResult(List<Transaction> transactions, int skippedBlocks) {}

    private Transaction parseBlock(String block) {
        Map<String, String> fields = parseFields(block);
        String amount = normalizeAmount(fields.get("amount"));
        if (amount == null) {
            return null;
        }

        String name = fields.getOrDefault("name", "").trim();
        String merchant = fields.getOrDefault("merchant", "").trim();
        if (merchant.isEmpty()) {
            merchant = name;
        }
        if (name.isEmpty()) {
            name = merchant;
        }

        String transactionDate = parseTransactionDate(fields.get("date"));
        if (transactionDate == null) {
            return null;
        }

        String location = fields.getOrDefault("location", "").trim();
        String cardType = inferCardType(name);
        String hash = hashTransaction(name, merchant, amount, transactionDate, location);

        Transaction transaction = new Transaction();
        transaction.setName(name);
        transaction.setMerchant(merchant);
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setCardType(cardType);
        transaction.setHash(hash);
        transaction.setAddress(location);
        return transaction;
    }

    private static Map<String, String> parseFields(String block) {
        java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        for (String rawLine : block.split("\\R")) {
            String line = normalizeLine(rawLine);
            if (line.isEmpty()) {
                continue;
            }

            Matcher matcher = FIELD_LINE.matcher(line);
            if (matcher.matches()) {
                flushField(fields, currentKey, currentValue);
                currentKey = matcher.group(1).toLowerCase(Locale.ROOT);
                currentValue = new StringBuilder(matcher.group(2).trim());
            } else if (currentKey != null) {
                if (!currentValue.isEmpty()) {
                    currentValue.append('\n');
                }
                currentValue.append(line);
            }
        }

        flushField(fields, currentKey, currentValue);
        return fields;
    }

    private static void flushField(
            Map<String, String> fields,
            String currentKey,
            StringBuilder currentValue
    ) {
        if (currentKey != null) {
            fields.put(currentKey, currentValue.toString().trim());
        }
    }

    private static String normalizeLine(String line) {
        return line.replace('\u202f', ' ').trim();
    }

    private static String normalizeAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        String amount = rawAmount.trim().replace("$", "").replace(",", "");
        if (amount.isEmpty() || !amount.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        return amount;
    }

    private static String parseTransactionDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String dateText = normalizeLine(rawDate);
        for (String zone : List.of(" EDT", " EST", " PST", " PDT", " CDT", " CST", " MDT", " MST")) {
            if (dateText.endsWith(zone)) {
                dateText = dateText.substring(0, dateText.length() - zone.length()).trim();
                break;
            }
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateText, WALLET_DATE);
            return dateTime.toLocalDate().toString();
        } catch (DateTimeParseException ex) {
            try {
                LocalDate date = LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.toString();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static String inferCardType(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("american express") || lower.contains("amex")) {
            return "amex";
        }
        if (lower.contains("visa")) {
            return "visa";
        }
        if (lower.contains("mastercard")) {
            return "mastercard";
        }
        if (lower.contains("debit")) {
            return "debit";
        }
        return "other";
    }

    static String hashTransaction(
            String name,
            String merchant,
            String amount,
            String transactionDate,
            String location
    ) {
        String payload = String.join(
                "|",
                name,
                merchant,
                amount,
                transactionDate,
                location
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
