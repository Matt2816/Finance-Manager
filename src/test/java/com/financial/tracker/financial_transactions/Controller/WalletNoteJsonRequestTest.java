package com.financial.tracker.financial_transactions.Controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
class WalletNoteJsonRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserializesLowercaseKeys() throws Exception {
        WalletNoteJsonRequest request = objectMapper.readValue(
                """
                {
                  "name": "test",
                  "merchant": "Test Shop",
                  "amount": "$69.69",
                  "date": "May 22, 2026 at 11:08 AM",
                  "location": "123 Main St"
                }
                """,
                WalletNoteJsonRequest.class
        );

        assertEquals("test", request.name());
        assertEquals("Test Shop", request.merchant());
    }

    @Test
    void deserializesWalletStyleCapitalizedKeys() throws Exception {
        WalletNoteJsonRequest request = objectMapper.readValue(
                """
                {
                  "Name": "test",
                  "Merchant": "Test Shop",
                  "Amount": "$69.69",
                  "Date": "May 22, 2026 at 11:08 AM",
                  "Location": "123 Main St"
                }
                """,
                WalletNoteJsonRequest.class
        );

        assertEquals("test", request.name());
        assertEquals("Test Shop", request.merchant());
    }

    @Test
    void mixedLowercaseNameAndCapitalizedMerchant_mapsMerchant() throws Exception {
        WalletNoteJsonRequest request = objectMapper.readValue(
                """
                {
                  "name": "test",
                  "Merchant": "Test Shop",
                  "amount": "$69.69",
                  "date": "May 22, 2026 at 11:08 AM",
                  "location": "123 Main St"
                }
                """,
                WalletNoteJsonRequest.class
        );

        assertEquals("test", request.name());
        assertEquals("Test Shop", request.merchant());
    }

}
