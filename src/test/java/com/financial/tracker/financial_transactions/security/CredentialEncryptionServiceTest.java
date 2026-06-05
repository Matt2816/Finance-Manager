package com.financial.tracker.financial_transactions.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialEncryptionServiceTest {

    private static final String TEST_KEY_BASE64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void encryptDecryptRoundTrip() {
        CredentialEncryptionService service = new CredentialEncryptionService(TEST_KEY_BASE64);
        assertTrue(service.isConfigured());

        CredentialEncryptionService.EncryptedPayload encrypted = service.encrypt("my-splitwise-key");
        String decrypted = service.decrypt(encrypted.ciphertext(), encrypted.iv());

        assertEquals("my-splitwise-key", decrypted);
        assertNotEquals("my-splitwise-key", new String(encrypted.ciphertext()));
    }

    @Test
    void keyHintShowsLastFourCharacters() {
        CredentialEncryptionService service = new CredentialEncryptionService(TEST_KEY_BASE64);
        assertEquals("…-key", service.keyHint("my-splitwise-key"));
    }
}
