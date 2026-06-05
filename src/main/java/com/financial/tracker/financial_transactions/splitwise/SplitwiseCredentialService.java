package com.financial.tracker.financial_transactions.splitwise;

import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.security.CredentialEncryptionService;
import com.financial.tracker.financial_transactions.splitwise.client.SplitwiseApiClient;
import com.financial.tracker.financial_transactions.splitwise.client.SplitwiseApiException;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import org.springframework.stereotype.Service;

@Service
public class SplitwiseCredentialService {

    private final UserRepository userRepository;
    private final CredentialEncryptionService encryptionService;
    private final SplitwiseApiClient apiClient;

    public SplitwiseCredentialService(
            UserRepository userRepository,
            CredentialEncryptionService encryptionService,
            SplitwiseApiClient apiClient
    ) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.apiClient = apiClient;
    }

    public String decryptApiKey(User user) {
        if (user == null || !user.hasSplitwiseCredentials()) {
            return null;
        }
        return encryptionService.decrypt(user.getSplitwiseApiKeyEnc(), user.getSplitwiseApiKeyIv());
    }

    public String keyHintForUser(User user) {
        String key = decryptApiKey(user);
        return key == null ? null : encryptionService.keyHint(key);
    }

    public void validateAndStoreApiKey(User user, String apiKey) {
        if (!encryptionService.isConfigured()) {
            throw new IllegalStateException("SPLITWISE_CREDENTIALS_KEY is not configured on the server");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required");
        }
        try {
            apiClient.getCurrentUser(apiKey.trim());
        } catch (SplitwiseApiException ex) {
            throw new IllegalArgumentException("Invalid Splitwise API key: " + ex.getMessage(), ex);
        }
        CredentialEncryptionService.EncryptedPayload encrypted =
                encryptionService.encrypt(apiKey.trim());
        user.setSplitwiseApiKeyEnc(encrypted.ciphertext());
        user.setSplitwiseApiKeyIv(encrypted.iv());
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
