package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantCategoryRuleRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.Controller.ControllerRequestLogger;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final SpendingCategoryRepository categoryRepository;
    private final MerchantCategoryRuleRepository ruleRepository;
    private final NormalizedTransactionRepository normalizedRepo;
    private final TransactionsRepo transactionsRepo;

    public CategoryController(
            SpendingCategoryRepository categoryRepository,
            MerchantCategoryRuleRepository ruleRepository,
            NormalizedTransactionRepository normalizedRepo,
            TransactionsRepo transactionsRepo
    ) {
        this.categoryRepository = categoryRepository;
        this.ruleRepository = ruleRepository;
        this.normalizedRepo = normalizedRepo;
        this.transactionsRepo = transactionsRepo;
    }

    @GetMapping
    public List<CategoryDto> listCategories() {
        ControllerRequestLogger.logIncoming(log, "listCategories");
        Long userId = SecurityUtils.getCurrentUserId();
        return categoryRepository.findByUserId(userId).stream()
                .map(c -> new CategoryDto(c.getId(), c.getSlug(), c.getDisplayName()))
                .toList();
    }

    @GetMapping("/uncategorized")
    public List<UncategorizedTransactionDto> getUncategorizedTransactions() {
        ControllerRequestLogger.logIncoming(log, "getUncategorizedTransactions");
        Long userId = SecurityUtils.getCurrentUserId();
        SpendingCategory uncategorized = categoryRepository.findBySlugAndUserId("uncategorized", userId).orElse(null);
        if (uncategorized == null) {
            return List.of();
        }

        List<NormalizedTransaction> normalizedList = normalizedRepo.findByUserIdAndCategoryId(userId, uncategorized.getId());
        List<UncategorizedTransactionDto> result = new ArrayList<>();

        for (NormalizedTransaction n : normalizedList) {
            Transaction tx = transactionsRepo.findByIdAndUserId(n.getTransactionId(), userId).orElse(null);
            if (tx == null) continue;

            result.add(new UncategorizedTransactionDto(
                    tx.getId(),
                    tx.getName(),
                    tx.getMerchant(),
                    tx.getAmount(),
                    tx.getTransactionDate(),
                    tx.getCardType(),
                    n.getMerchantRaw(),
                    n.getMerchantKey()
            ));
        }
        return result;
    }

    @PostMapping("/assign")
    public ResponseEntity<String> assignCategory(@RequestBody AssignCategoryRequest request) {
        ControllerRequestLogger.logIncoming(log, "assignCategory", "transactionId", request.transactionId(), "categoryId", request.categoryId());
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<Transaction> txOpt = transactionsRepo.findByIdAndUserId(request.transactionId(), userId);
        if (txOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Transaction tx = txOpt.get();
        tx.setCategoryId(request.categoryId());
        transactionsRepo.save(tx);

        NormalizedTransaction normalized = normalizedRepo.findByTransactionIdAndUserId(tx.getId(), userId).orElse(null);
        if (normalized != null) {
            normalized.setCategoryId(request.categoryId());
            normalizedRepo.save(normalized);
        }

        if (Boolean.TRUE.equals(request.createRule())) {
            String ruleSource = (normalized != null && normalized.getMerchantKey() != null && !normalized.getMerchantKey().isBlank())
                    ? normalized.getMerchantKey()
                    : tx.getMerchant();
            if (ruleSource != null && !ruleSource.isBlank()) {
                String merchantLower = ruleSource.trim().toLowerCase();
                MerchantCategoryRule existingRule = null;
                for (MerchantCategoryRule existing : ruleRepository.findByUserIdAndCategoryId(userId, request.categoryId())) {
                    existingRule = existing;
                    break;
                }
                if (existingRule != null) {
                    List<String> alts = extractAlternatives(existingRule.getPattern());
                    if (!alts.contains(merchantLower)) {
                        alts.add(merchantLower);
                        existingRule.setPattern(buildPattern(alts));
                        ruleRepository.save(existingRule);
                        log.info("Updated merchant category rule for category {}: added {}", request.categoryId(), merchantLower);
                    }
                } else {
                    MerchantCategoryRule rule = new MerchantCategoryRule();
                    rule.setUserId(userId);
                    rule.setPattern(buildPattern(List.of(merchantLower)));
                    rule.setCategoryId(request.categoryId());
                    rule.setPriority(50);
                    ruleRepository.save(rule);
                    log.info("Created merchant category rule: {} -> category {}", merchantLower, request.categoryId());
                }
            }
        }

        return ResponseEntity.ok("Category assigned successfully");
    }

    @GetMapping("/rules")
    public List<MerchantRuleDto> listRules() {
        ControllerRequestLogger.logIncoming(log, "listRules");
        Long userId = SecurityUtils.getCurrentUserId();
        Map<Long, String> categoryNames = new HashMap<>();
        for (SpendingCategory c : categoryRepository.findByUserId(userId)) {
            categoryNames.put(c.getId(), c.getDisplayName());
        }

        return ruleRepository.findByUserIdOrderByPriorityAsc(userId).stream()
                .map(r -> new MerchantRuleDto(
                        r.getId(),
                        r.getPattern(),
                        r.getCategoryId(),
                        categoryNames.getOrDefault(r.getCategoryId(), "Unknown"),
                        r.getPriority()
                ))
                .toList();
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<String> deleteRule(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "deleteRule", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        if (ruleRepository.findByIdAndUserId(id, userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ruleRepository.deleteById(id);
        return ResponseEntity.ok("Rule deleted");
    }

    @PostMapping("/rules")
    public ResponseEntity<String> createRule(@RequestBody CreateRuleRequest request) {
        ControllerRequestLogger.logIncoming(log, "createRule", "pattern", request.pattern(), "categoryId", request.categoryId());
        try {
            Pattern.compile(request.pattern());
        } catch (PatternSyntaxException e) {
            return ResponseEntity.badRequest().body("Invalid regex pattern: " + e.getMessage());
        }

        Long userId = SecurityUtils.getCurrentUserId();
        MerchantCategoryRule rule = new MerchantCategoryRule();
        rule.setUserId(userId);
        rule.setPattern(request.pattern());
        rule.setCategoryId(request.categoryId());
        rule.setPriority(request.priority() != null ? request.priority() : 50);
        ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body("Rule created");
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CreateCategoryRequest request) {
        ControllerRequestLogger.logIncoming(log, "createCategory", "slug", request.slug(), "displayName", request.displayName());
        if (request.slug() == null || request.slug().isBlank() || request.displayName() == null || request.displayName().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (categoryRepository.findBySlugAndUserId(request.slug(), userId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        SpendingCategory cat = new SpendingCategory();
        cat.setUserId(userId);
        cat.setSlug(request.slug());
        cat.setDisplayName(request.displayName());
        SpendingCategory saved = categoryRepository.save(cat);
        log.info("Created spending category: {} ({})", saved.getSlug(), saved.getDisplayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CategoryDto(saved.getId(), saved.getSlug(), saved.getDisplayName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "deleteCategory", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<SpendingCategory> catOpt = categoryRepository.findByIdAndUserId(id, userId);
        if (catOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SpendingCategory cat = catOpt.get();
        if ("uncategorized".equals(cat.getSlug())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot delete the uncategorized category");
        }

        SpendingCategory uncategorized = categoryRepository.findBySlugAndUserId("uncategorized", userId).orElse(null);
        if (uncategorized == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Uncategorized category missing");
        }
        Long uncategorizedId = uncategorized.getId();

        List<Transaction> transactions = transactionsRepo.findByUserIdAndCategoryId(userId, id);
        for (Transaction tx : transactions) {
            tx.setCategoryId(uncategorizedId);
        }
        transactionsRepo.saveAll(transactions);

        List<NormalizedTransaction> normalizedList = normalizedRepo.findByUserIdAndCategoryId(userId, id);
        for (NormalizedTransaction n : normalizedList) {
            n.setCategoryId(uncategorizedId);
        }
        normalizedRepo.saveAll(normalizedList);

        for (MerchantCategoryRule rule : ruleRepository.findByUserIdAndCategoryId(userId, id)) {
            ruleRepository.delete(rule);
        }

        categoryRepository.deleteById(id);
        log.info("Deleted spending category {} and re-assigned {} transactions to uncategorized", id, transactions.size());
        return ResponseEntity.ok("Category deleted");
    }

    private List<String> extractAlternatives(String pattern) {
        List<String> alternatives = new ArrayList<>();
        String base = pattern.startsWith("(?i)") ? pattern.substring(4) : pattern;

        Matcher m = Pattern.compile("\\\\Q(.*?)\\\\E").matcher(base);
        boolean found = false;
        while (m.find()) {
            alternatives.add(m.group(1).toLowerCase());
            found = true;
        }

        if (!found) {
            for (String alt : base.split("\\|")) {
                if (!alt.trim().isEmpty()) {
                    alternatives.add(alt.trim().toLowerCase());
                }
            }
        }

        return alternatives;
    }

    private String buildPattern(List<String> alternatives) {
        StringBuilder sb = new StringBuilder("(?i)");
        for (int i = 0; i < alternatives.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append("\\Q").append(alternatives.get(i)).append("\\E");
        }
        return sb.toString();
    }

    public record CategoryDto(Long id, String slug, String displayName) {}
    public record UncategorizedTransactionDto(
            int id, String name, String merchant, String amount,
            String transactionDate, String cardType, String merchantRaw, String merchantKey
    ) {}
    public record AssignCategoryRequest(int transactionId, Long categoryId, Boolean createRule) {}
    public record MerchantRuleDto(Long id, String pattern, Long categoryId, String categoryName, int priority) {}
    public record CreateRuleRequest(String pattern, Long categoryId, Integer priority) {}
    public record CreateCategoryRequest(String slug, String displayName) {}
}
