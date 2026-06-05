package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.MerchantLoyaltyDto;
import com.financial.tracker.financial_transactions.analytics.merchant.MerchantLoyaltyService;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantLoyaltyMetricsRepository;
import com.financial.tracker.financial_transactions.Controller.ControllerRequestLogger;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchant-loyalty")
public class MerchantLoyaltyController {

    private static final Logger log = LoggerFactory.getLogger(MerchantLoyaltyController.class);

    private final MerchantLoyaltyMetricsRepository loyaltyRepository;
    private final MerchantLoyaltyService loyaltyService;

    public MerchantLoyaltyController(
            MerchantLoyaltyMetricsRepository loyaltyRepository,
            MerchantLoyaltyService loyaltyService
    ) {
        this.loyaltyRepository = loyaltyRepository;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping
    public List<MerchantLoyaltyDto> listLoyaltyMetrics(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        ControllerRequestLogger.logIncoming(log, "listLoyaltyMetrics", "categoryId", categoryId, "limit", limit);
        Long userId = SecurityUtils.getCurrentUserId();
        List<MerchantLoyaltyDto> metrics;
        if (categoryId != null) {
            metrics = loyaltyRepository.findByUserIdAndCategoryIdOrderByLoyaltyScoreDesc(userId, categoryId).stream()
                    .map(MerchantLoyaltyDto::from)
                    .toList();
        } else {
            metrics = loyaltyRepository.findByUserIdOrderByLoyaltyScoreDesc(userId).stream()
                    .map(MerchantLoyaltyDto::from)
                    .toList();
        }
        return metrics.stream().limit(limit).toList();
    }

    @PostMapping("/calculate")
    public MerchantLoyaltyService.CalculationResult calculateLoyaltyMetrics() {
        ControllerRequestLogger.logIncoming(log, "calculateLoyaltyMetrics");
        Long userId = SecurityUtils.getCurrentUserId();
        return loyaltyService.calculateAllLoyaltyMetrics(userId);
    }
}
