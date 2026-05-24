package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.MerchantLoyaltyDto;
import com.financial.tracker.financial_transactions.analytics.merchant.MerchantLoyaltyService;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantLoyaltyMetricsRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-loyalty")
public class MerchantLoyaltyController {

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
        List<MerchantLoyaltyDto> metrics;
        if (categoryId != null) {
            metrics = loyaltyRepository.findByCategoryIdOrderByLoyaltyScoreDesc(categoryId).stream()
                    .map(MerchantLoyaltyDto::from)
                    .toList();
        } else {
            metrics = loyaltyRepository.findAllByOrderByLoyaltyScoreDesc().stream()
                    .map(MerchantLoyaltyDto::from)
                    .toList();
        }
        return metrics.stream().limit(limit).toList();
    }

    @PostMapping("/calculate")
    public MerchantLoyaltyService.CalculationResult calculateLoyaltyMetrics() {
        return loyaltyService.calculateAllLoyaltyMetrics();
    }
}
