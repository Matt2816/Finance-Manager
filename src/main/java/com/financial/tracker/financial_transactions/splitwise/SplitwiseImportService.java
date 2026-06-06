package com.financial.tracker.financial_transactions.splitwise;

import com.financial.tracker.financial_transactions.Services.TransactionImportPipeline;
import com.financial.tracker.financial_transactions.analytics.model.RefreshRunStatus;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.model.SplitwiseImportState;
import com.financial.tracker.financial_transactions.model.SplitwiseSyncRun;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.SplitwiseImportStateRepository;
import com.financial.tracker.financial_transactions.repo.SplitwiseSyncRunRepository;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.splitwise.client.SplitwiseApiClient;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseGroupDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import com.financial.tracker.financial_transactions.splitwise.config.SplitwiseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class SplitwiseImportService {

    private static final Logger log = LoggerFactory.getLogger(SplitwiseImportService.class);

    private final UserRepository userRepository;
    private final SplitwiseImportStateRepository stateRepository;
    private final SplitwiseSyncRunRepository syncRunRepository;
    private final TransactionsRepo transactionsRepo;
    private final NormalizedTransactionRepository normalizedTransactionRepository;
    private final SplitwiseApiClient apiClient;
    private final SplitwiseCredentialService credentialService;
    private final SplitwiseExpenseMapper expenseMapper;
    private final TransactionImportPipeline importPipeline;
    private final SplitwiseProperties properties;
    private final ConcurrentHashMap<Long, AtomicBoolean> userSyncRunning = new ConcurrentHashMap<>();

    public SplitwiseImportService(
            UserRepository userRepository,
            SplitwiseImportStateRepository stateRepository,
            SplitwiseSyncRunRepository syncRunRepository,
            TransactionsRepo transactionsRepo,
            NormalizedTransactionRepository normalizedTransactionRepository,
            SplitwiseApiClient apiClient,
            SplitwiseCredentialService credentialService,
            SplitwiseExpenseMapper expenseMapper,
            TransactionImportPipeline importPipeline,
            SplitwiseProperties properties
    ) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.syncRunRepository = syncRunRepository;
        this.transactionsRepo = transactionsRepo;
        this.normalizedTransactionRepository = normalizedTransactionRepository;
        this.apiClient = apiClient;
        this.credentialService = credentialService;
        this.expenseMapper = expenseMapper;
        this.importPipeline = importPipeline;
        this.properties = properties;
    }

    public boolean tryStartSyncForUser(Long userId) {
        AtomicBoolean running = userSyncRunning.computeIfAbsent(userId, id -> new AtomicBoolean(false));
        return running.compareAndSet(false, true);
    }

    public void finishSyncForUser(Long userId) {
        AtomicBoolean running = userSyncRunning.get(userId);
        if (running != null) {
            running.set(false);
        }
    }

    @Transactional
    public SplitwiseImportResult syncForUser(Long userId) {
        long startMs = System.currentTimeMillis();
        log.info("Starting Splitwise sync for userId={}", userId);
        User user = credentialService.requireUser(userId);
        if (!user.isSplitwiseEnabled() || !user.hasSplitwiseCredentials()) {
            log.warn("Splitwise not enabled or API key missing for userId={}", userId);
            throw new IllegalStateException("Splitwise is not enabled or API key is not configured for user " + userId);
        }

        String apiKey = credentialService.decryptApiKey(user);
        SplitwiseSyncRun run = startRun(userId);
        SplitwiseImportState state = loadOrCreateState(userId);
        ZonedDateTime pollStartedAt = ZonedDateTime.now();
        state.setLastPollAt(pollStartedAt);

        try {
            SplitwiseUserDto currentUser = apiClient.getCurrentUser(apiKey);
            log.info("Resolved Splitwise userId={} for userId={}", currentUser.getId(), userId);
            state.setSplitwiseUserId(currentUser.getId());

            List<SplitwiseGroupDto> groups = apiClient.getGroups(apiKey);
            log.info("Fetched {} Splitwise groups for userId={}", groups.size(), userId);
            Map<Long, String> groupNamesById = groups.stream()
                    .filter(g -> g.getId() != null)
                    .collect(Collectors.toMap(
                            SplitwiseGroupDto::getId,
                            g -> g.getName() == null ? "Group " + g.getId() : g.getName(),
                            (a, b) -> a
                    ));

            List<Long> groupIdsToPoll = resolveGroupIds(user.getSplitwiseGroupNames(), groups);
            log.info("Polling {} group(s) for userId={}: {}", groupIdsToPoll.size(), userId, groupIdsToPoll);
            ZonedDateTime updatedAfter = computeUpdatedAfter(state);
            LocalDate datedAfter = state.getLastSuccessAt() == null
                    ? LocalDate.now().minusDays(properties.getInitialLookbackDays())
                    : null;

            log.info("Fetching expenses for userId={} updatedAfter={} datedAfter={}", userId, updatedAfter, datedAfter);
            Map<Long, SplitwiseExpenseDto> expensesById = fetchAllExpenses(
                    apiKey, groupIdsToPoll, updatedAfter, datedAfter
            );
            log.info("Fetched {} unique expenses for userId={}", expensesById.size(), userId);

            SplitwiseImportResult result = processExpenses(
                    userId, currentUser.getId(), expensesById, groupNamesById
            );

            state.setLastSuccessAt(ZonedDateTime.now());
            state.setLastError(null);
            stateRepository.save(state);
            completeRun(run, RefreshRunStatus.SUCCESS, result, null);

            long durationMs = System.currentTimeMillis() - startMs;
            log.info(
                    "Splitwise sync completed userId={} imported={} updated={} deleted={} skipped={} durationMs={}",
                    userId,
                    result.imported(),
                    result.updated(),
                    result.deleted(),
                    result.skipped(),
                    durationMs
            );
            if (log.isDebugEnabled()) {
                log.debug("Imported transactions: {}", result.getImportedTransactions().stream().map(SplitwiseImportResult.TransactionSummary::description).toList());
                log.debug("Updated transactions: {}", result.getUpdatedTransactions().stream().map(SplitwiseImportResult.TransactionSummary::description).toList());
                log.debug("Deleted transactions: {}", result.getDeletedTransactions().stream().map(SplitwiseImportResult.TransactionSummary::description).toList());
                log.debug("Skipped transactions: {}", result.getSkippedTransactions().stream().map(SplitwiseImportResult.TransactionSummary::description).toList());
            }
            return result;
        } catch (Exception ex) {
            String error = truncate(ex.getMessage(), 4000);
            state.setLastError(error);
            stateRepository.save(state);
            completeRun(run, RefreshRunStatus.FAILED, SplitwiseImportResult.EMPTY, error);
            log.error("Splitwise sync failed for userId={}", userId, ex);
            throw ex;
        }
    }

    private SplitwiseImportResult processExpenses(
            Long userId,
            Long splitwiseUserId,
            Map<Long, SplitwiseExpenseDto> expensesById,
            Map<Long, String> groupNamesById
    ) {
        SplitwiseImportResult result = new SplitwiseImportResult();
        log.info("Processing {} expenses for userId={}", expensesById.size(), userId);

        for (SplitwiseExpenseDto expense : expensesById.values()) {
            if (expense.getId() == null) {
                log.debug("Skipping expense with null ID for userId={}", userId);
                result.addSkipped(summaryFromExpense(expense, groupNamesById, "Missing expense ID"));
                continue;
            }
            String hash = expenseMapper.expenseHash(expense.getId());

            if (expense.isDeleted() || expense.isPayment()) {
                if (deleteByHash(userId, hash)) {
                    log.info("Deleted transaction hash={} desc='{}' for userId={}", hash, expense.getDescription(), userId);
                    result.addDeleted(summaryFromExpense(expense, groupNamesById, null));
                } else {
                    log.debug("Skipping deleted/payment expense desc='{}' for userId={}", expense.getDescription(), userId);
                    result.addSkipped(summaryFromExpense(expense, groupNamesById, "Deleted/payment expense"));
                }
                continue;
            }

            if (!expenseMapper.isOwedExpense(expense, splitwiseUserId)) {
                if (deleteByHash(userId, hash)) {
                    log.info("Deleted non-owed transaction hash={} desc='{}' for userId={}", hash, expense.getDescription(), userId);
                    result.addDeleted(summaryFromExpense(expense, groupNamesById, null));
                } else {
                    log.debug("Skipping non-owed expense desc='{}' for userId={}", expense.getDescription(), userId);
                    result.addSkipped(summaryFromExpense(expense, groupNamesById, "Not owed by user"));
                }
                continue;
            }

            Transaction mapped = expenseMapper.toTransaction(expense, splitwiseUserId, groupNamesById);
            if (mapped == null) {
                log.debug("Failed to map expense desc='{}' id={} for userId={}", expense.getDescription(), expense.getId(), userId);
                result.addSkipped(summaryFromExpense(expense, groupNamesById, "Failed to map expense"));
                continue;
            }

            Transaction existing = transactionsRepo.findByHashAndUserId(hash, userId);
            if (existing == null) {
                log.info("Importing new transaction hash={} desc='{}' amount={} for userId={}", hash, mapped.getName(), mapped.getAmountValue(), userId);
                importPipeline.saveNew(mapped, userId);
                result.addImported(summaryFromTransaction(mapped, expense, groupNamesById, null));
            } else if (importPipeline.fieldsDiffer(existing, mapped)) {
                log.info("Updating transaction hash={} desc='{}' for userId={}", hash, mapped.getName(), userId);
                importPipeline.updateExisting(existing, mapped, userId);
                result.addUpdated(summaryFromTransaction(mapped, expense, groupNamesById, null));
            } else {
                log.debug("Skipping unchanged transaction hash={} desc='{}' for userId={}", hash, mapped.getName(), userId);
                result.addSkipped(summaryFromTransaction(mapped, expense, groupNamesById, "No changes"));
            }
        }

        log.info("Finished processing expenses for userId={}: imported={} updated={} deleted={} skipped={}",
                userId, result.imported(), result.updated(), result.deleted(), result.skipped());
        return result;
    }

    private SplitwiseImportResult.TransactionSummary summaryFromExpense(
            SplitwiseExpenseDto expense,
            Map<Long, String> groupNamesById,
            String reason
    ) {
        String desc = expense.getDescription() != null ? expense.getDescription() : "Unknown";
        String amount = expense.getCost() != null ? expense.getCost() : "0.00";
        String date = expense.getDate() != null ? expense.getDate() : "";
        String group = groupNamesById.getOrDefault(expense.getGroupId(), "");
        return new SplitwiseImportResult.TransactionSummary(desc, amount, date, group, reason);
    }

    private SplitwiseImportResult.TransactionSummary summaryFromTransaction(
            Transaction tx,
            SplitwiseExpenseDto expense,
            Map<Long, String> groupNamesById,
            String reason
    ) {
        String desc = tx.getName() != null ? tx.getName() : "Unknown";
        String amount = tx.getAmountValue() != null ? tx.getAmountValue().toString() : "0.00";
        String date = tx.getOccurredOn() != null ? tx.getOccurredOn().toString() : "";
        String group = groupNamesById.getOrDefault(expense.getGroupId(), "");
        return new SplitwiseImportResult.TransactionSummary(desc, amount, date, group, reason);
    }

    private boolean deleteByHash(Long userId, String hash) {
        Transaction existing = transactionsRepo.findByHashAndUserId(hash, userId);
        if (existing == null) {
            return false;
        }
        normalizedTransactionRepository.deleteByTransactionIdAndUserId(existing.getId(), userId);
        transactionsRepo.delete(existing);
        return true;
    }

    private Map<Long, SplitwiseExpenseDto> fetchAllExpenses(
            String apiKey,
            List<Long> groupIdsToPoll,
            ZonedDateTime updatedAfter,
            LocalDate datedAfter
    ) {
        Map<Long, SplitwiseExpenseDto> byId = new LinkedHashMap<>();
        for (Long groupId : groupIdsToPoll) {
            log.debug("Fetching expenses for groupId={} updatedAfter={} datedAfter={}", groupId, updatedAfter, datedAfter);
            List<SplitwiseExpenseDto> page = apiClient.getExpenses(apiKey, groupId, updatedAfter, datedAfter);
            log.debug("Received {} expenses for groupId={}", page.size(), groupId);
            for (SplitwiseExpenseDto expense : page) {
                if (expense.getId() != null) {
                    byId.put(expense.getId(), expense);
                } else {
                    log.warn("Ignoring expense with null ID in groupId={}", groupId);
                }
            }
        }
        return byId;
    }

    private List<Long> resolveGroupIds(String groupNamesCsv, List<SplitwiseGroupDto> groups) {
        List<String> requested = parseGroupNames(groupNamesCsv);
        if (requested.isEmpty()) {
            return List.of((Long) null);
        }

        Map<String, Long> byLowerName = new LinkedHashMap<>();
        for (SplitwiseGroupDto group : groups) {
            if (group.getName() != null && group.getId() != null) {
                byLowerName.put(group.getName().trim().toLowerCase(Locale.ROOT), group.getId());
            }
        }

        List<Long> ids = new ArrayList<>();
        for (String name : requested) {
            Long id = byLowerName.get(name.toLowerCase(Locale.ROOT));
            if (id != null) {
                ids.add(id);
            } else {
                log.warn("Splitwise group not found: {}", name);
            }
        }
        return ids.isEmpty() ? List.of((Long) null) : ids;
    }

    private static List<String> parseGroupNames(String groupNamesCsv) {
        if (groupNamesCsv == null || groupNamesCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groupNamesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private ZonedDateTime computeUpdatedAfter(SplitwiseImportState state) {
        if (state.getLastSuccessAt() == null) {
            return null;
        }
        return state.getLastSuccessAt().minusSeconds(properties.getPollOverlapMinutes() * 60L);
    }

    private SplitwiseImportState loadOrCreateState(Long userId) {
        return stateRepository.findById(userId).orElseGet(() -> {
            SplitwiseImportState created = new SplitwiseImportState();
            created.setUserId(userId);
            return stateRepository.save(created);
        });
    }

    private SplitwiseSyncRun startRun(Long userId) {
        SplitwiseSyncRun run = new SplitwiseSyncRun();
        run.setUserId(userId);
        run.setStatus(RefreshRunStatus.RUNNING);
        run.setStartedAt(ZonedDateTime.now());
        return syncRunRepository.save(run);
    }

    private void completeRun(
            SplitwiseSyncRun run,
            RefreshRunStatus status,
            SplitwiseImportResult result,
            String error
    ) {
        run.setStatus(status);
        run.setFinishedAt(ZonedDateTime.now());
        run.setImportedCount(result.imported());
        run.setUpdatedCount(result.updated());
        run.setDeletedCount(result.deleted());
        run.setSkippedCount(result.skipped());
        run.setErrorLog(error);
        syncRunRepository.save(run);
    }

    public List<User> usersEligibleForPolling() {
        return userRepository.findBySplitwiseEnabledTrue().stream()
                .filter(User::hasSplitwiseCredentials)
                .toList();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
