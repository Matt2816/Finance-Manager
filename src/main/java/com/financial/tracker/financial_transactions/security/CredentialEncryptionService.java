package com.financial.tracker.financial_transactions.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CredentialEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(
            @Value("${splitwise.credentials.key:}") String credentialsKeyBase64
    ) {
        if (credentialsKeyBase64 == null || credentialsKeyBase64.isBlank()) {
            this.secretKey = null;
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(credentialsKeyBase64.trim());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "splitwise.credentials.key must decode to 32 bytes (AES-256); got " + keyBytes.length
                );
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    public boolean isConfigured() {
        return secretKey != null;
    }

    public EncryptedPayload encrypt(String plaintext) {
        requireKey();
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("plaintext must not be blank");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(ciphertext, iv);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt credential", ex);
        }
    }

    public String decrypt(byte[] ciphertext, byte[] iv) {
        requireKey();
        if (ciphertext == null || ciphertext.length == 0 || iv == null || iv.length == 0) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt credential", ex);
        }
    }

    public String keyHint(String plaintext) {
        if (plaintext == null || plaintext.length() < 4) {
            return null;
        }
        return "…" + plaintext.substring(plaintext.length() - 4);
    }

    private void requireKey() {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "SPLITWISE_CREDENTIALS_KEY is not configured; cannot encrypt or decrypt API keys"
            );
        }
    }

    public record EncryptedPayload(byte[] ciphertext, byte[] iv) {}
}
