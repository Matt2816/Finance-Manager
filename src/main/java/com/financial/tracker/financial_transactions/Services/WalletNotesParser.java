package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletNotesParser {

    private static final Logger log = LoggerFactory.getLogger(WalletNotesParser.class);

    private static final Pattern FIELD_LINE = Pattern.compile(
            "^(Name|Merchant|Amount|Date|Location):\\s*(.*)$"
    );

    public ParseResult parse(String content) {
        int contentLength = content == null ? 0 : content.length();
        log.info("Parsing wallet notes batch, contentLength={}", contentLength);

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
                log.debug("Skipped wallet note block (invalid or missing amount)");
            }
        }

        log.info(
                "Parsed wallet notes batch: transactions={}, skippedBlocks={}",
                transactions.size(),
                skippedBlocks
        );
        return new ParseResult(transactions, skippedBlocks);
    }

    public record ParseResult(List<Transaction> transactions, int skippedBlocks) {}

    public Transaction parseSingle(String content) {
        if (content == null || content.isBlank()) {
            log.info("parseSingle: empty content");
            return null;
        }

        String block = content.trim().replaceAll("(?m)^-{28,}\\s*$", "").trim();
        if (block.isBlank()) {
            log.info("parseSingle: blank block after trim");
            return null;
        }

        log.info("parseSingle: parsing block, length={}", block.length());
        Transaction transaction = parseBlock(block);
        if (transaction == null) {
            log.info("parseSingle: failed to parse transaction");
        } else {
            log.info(
                    "parseSingle: parsed name={}, merchant={}, amount={}, date={}, hash={}",
                    transaction.getName(),
                    transaction.getMerchant(),
                    transaction.getAmount(),
                    transaction.getTransactionDate(),
                    transaction.getHash()
            );
        }
        return transaction;
    }

    private Transaction parseBlock(String block) {
        Map<String, String> fields = parseFields(block);
        return buildFromFields(
                fields.getOrDefault("name", ""),
                fields.getOrDefault("merchant", ""),
                fields.get("amount"),
                fields.get("date"),
                fields.getOrDefault("location", "")
        );
    }

    public Transaction buildFromFields(
            String name,
            String merchant,
            String amount,
            String date,
            String location
    ) {
        log.info(
                "buildFromFields: name={}, merchant={}, amount={}, date={}, locationLength={}",
                name,
                merchant,
                amount,
                date,
                location == null ? 0 : location.length()
        );

        BigDecimal amountValue = TransactionFieldParser.parseAmountValue(amount);
        if (amountValue == null) {
            log.info("buildFromFields: invalid amount");
            return null;
        }

        LocalDate occurredOn = TransactionFieldParser.parseOccurredOn(date);
        if (occurredOn == null) {
            log.info("buildFromFields: invalid date");
            return null;
        }

        String resolvedName = name == null ? "" : name.trim();
        String resolvedMerchant = merchant == null ? "" : merchant.trim();
        if (resolvedMerchant.isEmpty()) {
            resolvedMerchant = resolvedName;
        }
        if (resolvedName.isEmpty()) {
            resolvedName = resolvedMerchant;
        }

        String resolvedLocation = location == null ? "" : location.trim();
        String cardType = inferCardType(resolvedName);
        String normalizedAmount = amountValue.toPlainString();

        Transaction transaction = new Transaction();
        transaction.setName(resolvedName);
        transaction.setMerchant(resolvedMerchant);
        transaction.setAmount(normalizedAmount);
        transaction.setAmountValue(amountValue);
        transaction.setOccurredOn(occurredOn);
        transaction.setTransactionDate(TransactionFieldParser.formatTransactionDate(occurredOn));
        transaction.setCurrency(TransactionFieldParser.DEFAULT_CURRENCY);
        transaction.setCardType(cardType);
        transaction.setAddress(resolvedLocation);
        transaction.setRecurringGenerated(false);
        transaction.setHash(TransactionFieldParser.hashTransaction(
                resolvedName,
                resolvedMerchant,
                amountValue,
                occurredOn,
                resolvedLocation
        ));
        log.info(
                "buildFromFields: built transaction name={}, amount={}, date={}, cardType={}, hash={}",
                resolvedName,
                normalizedAmount,
                occurredOn,
                cardType,
                transaction.getHash()
        );
        return transaction;
    }

    private static Map<String, String> parseFields(String block) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
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

    /** @deprecated use {@link TransactionFieldParser#hashTransaction} */
    @Deprecated
    static String hashTransaction(
            String name,
            String merchant,
            String amount,
            String transactionDate,
            String location
    ) {
        BigDecimal amountValue = TransactionFieldParser.parseAmountValue(amount);
        LocalDate occurredOn = TransactionFieldParser.parseOccurredOn(transactionDate);
        return TransactionFieldParser.hashTransaction(name, merchant, amountValue, occurredOn, location);
    }
}
