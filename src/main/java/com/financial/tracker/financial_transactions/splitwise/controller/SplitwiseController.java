package com.financial.tracker.financial_transactions.splitwise.controller;

import com.financial.tracker.financial_transactions.Controller.ControllerRequestLogger;
import com.financial.tracker.financial_transactions.model.SplitwiseImportState;
import com.financial.tracker.financial_transactions.model.SplitwiseSyncRun;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.SplitwiseImportStateRepository;
import com.financial.tracker.financial_transactions.repo.SplitwiseSyncRunRepository;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import com.financial.tracker.financial_transactions.splitwise.SplitwiseCredentialService;
import com.financial.tracker.financial_transactions.splitwise.SplitwisePollingJob;
import com.financial.tracker.financial_transactions.splitwise.controller.dto.SplitwiseConfigDto;
import com.financial.tracker.financial_transactions.splitwise.controller.dto.SplitwiseConfigRequest;
import com.financial.tracker.financial_transactions.splitwise.controller.dto.SplitwiseStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/splitwise")
public class SplitwiseController {

    private static final Logger log = LoggerFactory.getLogger(SplitwiseController.class);

    private final UserRepository userRepository;
    private final SplitwiseCredentialService credentialService;
    private final SplitwiseSyncRunRepository syncRunRepository;
    private final SplitwiseImportStateRepository stateRepository;
    private final SplitwisePollingJob pollingJob;

    public SplitwiseController(
            UserRepository userRepository,
            SplitwiseCredentialService credentialService,
            SplitwiseSyncRunRepository syncRunRepository,
            SplitwiseImportStateRepository stateRepository,
            SplitwisePollingJob pollingJob
    ) {
        this.userRepository = userRepository;
        this.credentialService = credentialService;
        this.syncRunRepository = syncRunRepository;
        this.stateRepository = stateRepository;
        this.pollingJob = pollingJob;
    }

    @GetMapping("/config")
    public SplitwiseConfigDto getConfig() {
        ControllerRequestLogger.logIncoming(log, "getConfig");
        User user = currentUser();
        return new SplitwiseConfigDto(
                user.hasSplitwiseCredentials(),
                credentialService.keyHintForUser(user),
                user.getSplitwiseGroupNames(),
                user.isSplitwiseEnabled()
        );
    }

    @PutMapping("/config")
    public SplitwiseConfigDto updateConfig(@RequestBody SplitwiseConfigRequest request) {
        ControllerRequestLogger.logIncoming(log, "updateConfig");
        User user = currentUser();

        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            try {
                credentialService.validateAndStoreApiKey(user, request.apiKey());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
            }
        } else if (!user.hasSplitwiseCredentials()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key is required");
        }

        if (request.groupNames() != null) {
            user.setSplitwiseGroupNames(request.groupNames().isBlank() ? null : request.groupNames().trim());
        }
        if (request.enabled() != null) {
            user.setSplitwiseEnabled(request.enabled());
        }

        userRepository.save(user);
        return getConfig();
    }

    @GetMapping("/status")
    public SplitwiseStatusDto status() {
        ControllerRequestLogger.logIncoming(log, "status");
        Long userId = SecurityUtils.getCurrentUserId();
        SplitwiseSyncRun run = syncRunRepository.findFirstByUserIdOrderByStartedAtDesc(userId).orElse(null);
        SplitwiseImportState state = stateRepository.findById(userId).orElse(null);
        return SplitwiseStatusDto.from(run, state);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        ControllerRequestLogger.logIncoming(log, "sync");
        Long userId = SecurityUtils.getCurrentUserId();
        User user = currentUser();
        if (!user.isSplitwiseEnabled() || !user.hasSplitwiseCredentials()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Configure and enable Splitwise before syncing"
            );
        }
        pollingJob.triggerAsync(userId);
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "message", "Splitwise sync started"));
    }

    private User currentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return credentialService.requireUser(userId);
    }
}
