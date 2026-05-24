package com.financial.tracker.financial_transactions.util;

import com.financial.tracker.financial_transactions.model.Transaction;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Shared parsing and typed-field population for transactions.
 */
public final class TransactionFieldParser {

    public static final String DEFAULT_CURRENCY = "CAD";

    private static final DateTimeFormatter WALLET_DATE = DateTimeFormatter.ofPattern(
            "MMMM d, yyyy 'at' h:mm:ss a",
            Locale.ENGLISH
    );

    private static final DateTimeFormatter EXCEL_DATE_NO_PERIOD = DateTimeFormatter.ofPattern(
            "d MMM yyyy",
            Locale.ENGLISH
    );

    private static final DateTimeFormatter EXCEL_DATE_WITH_PERIOD = DateTimeFormatter.ofPattern(
            "d MMM. yyyy",
            Locale.ENGLISH
    );

    private static final DateTimeFormatter EXCEL_DATE_MONTH_ONLY_PERIOD = DateTimeFormatter.ofPattern(
            "MMM. yyyy",
            Locale.ENGLISH
    );

    private TransactionFieldParser() {
    }

    public static String normalizeAmountString(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        String amount = rawAmount.trim().replace("$", "").replace(",", "");
        if (amount.isEmpty() || !amount.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        return amount;
    }

    public static BigDecimal parseAmountValue(String rawAmount) {
        String normalized = normalizeAmountString(rawAmount);
        if (normalized == null) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    public static LocalDate parseOccurredOn(String rawDate) {
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
            return dateTime.toLocalDate();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDate.parse(dateText, EXCEL_DATE_NO_PERIOD);
                } catch (DateTimeParseException ignored2) {
                    try {
                        return LocalDate.parse(dateText, EXCEL_DATE_WITH_PERIOD);
                    } catch (DateTimeParseException ignored3) {
                        try {
                            return LocalDate.parse(dateText, EXCEL_DATE_MONTH_ONLY_PERIOD);
                        } catch (DateTimeParseException ignored4) {
                            if (dateText.length() >= 10) {
                                try {
                                    return LocalDate.parse(dateText.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                                } catch (DateTimeParseException ignored5) {
                                    return null;
                                }
                            }
                            return null;
                        }
                    }
                }
            }
        }
    }

    public static String formatTransactionDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    public static void applyTypedFields(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        BigDecimal amountValue = transaction.getAmountValue();
        LocalDate occurredOn = transaction.getOccurredOn();

        if (amountValue == null && transaction.getAmount() != null) {
            amountValue = parseAmountValue(transaction.getAmount());
            transaction.setAmountValue(amountValue);
        }
        if (transaction.getAmount() == null && amountValue != null) {
            transaction.setAmount(amountValue.toPlainString());
        }

        if (occurredOn == null && transaction.getTransactionDate() != null) {
            occurredOn = parseOccurredOn(transaction.getTransactionDate());
            transaction.setOccurredOn(occurredOn);
        }
        if (transaction.getTransactionDate() == null && occurredOn != null) {
            transaction.setTransactionDate(formatTransactionDate(occurredOn));
        }

        if (transaction.getCurrency() == null || transaction.getCurrency().isBlank()) {
            transaction.setCurrency(DEFAULT_CURRENCY);
        }
    }

    public static void populateFromRaw(
            Transaction transaction,
            String rawAmount,
            LocalDate occurredOn
    ) {
        String normalizedAmount = normalizeAmountString(rawAmount);
        if (normalizedAmount != null) {
            transaction.setAmount(normalizedAmount);
            transaction.setAmountValue(new BigDecimal(normalizedAmount));
        }
        if (occurredOn != null) {
            transaction.setOccurredOn(occurredOn);
            transaction.setTransactionDate(formatTransactionDate(occurredOn));
        }
        applyTypedFields(transaction);
    }

    public static String hashTransaction(
            String name,
            String merchant,
            BigDecimal amountValue,
            LocalDate occurredOn,
            String location
    ) {
        String amountPart = amountValue == null ? "" : amountValue.toPlainString();
        String datePart = occurredOn == null ? "" : occurredOn.toString();
        String payload = String.join(
                "|",
                name == null ? "" : name,
                merchant == null ? "" : merchant,
                amountPart,
                datePart,
                location == null ? "" : location
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String normalizeLine(String line) {
        return line.replace('\u202f', ' ').trim();
    }
}
