package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.CategorySpendingDto;
import com.financial.tracker.financial_transactions.analytics.controller.dto.ForecastDto;
import com.financial.tracker.financial_transactions.analytics.model.ForecastScope;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.model.SpendingForecast;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingForecastRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingSnapshotRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final SpendingForecastRepository forecastRepository;
    private final SpendingSnapshotRepository snapshotRepository;
    private final SpendingCategoryRepository categoryRepository;

    public ForecastController(
            SpendingForecastRepository forecastRepository,
            SpendingSnapshotRepository snapshotRepository,
            SpendingCategoryRepository categoryRepository
    ) {
        this.forecastRepository = forecastRepository;
        this.snapshotRepository = snapshotRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<ForecastDto> listForecasts(
            @RequestParam(defaultValue = "TOTAL") String scope,
            @RequestParam(required = false) Integer horizon
    ) {
        ForecastScope forecastScope = ForecastScope.valueOf(scope.toUpperCase());
        List<SpendingForecast> forecasts = forecastRepository.findByScopeOrderByForecastDateAsc(forecastScope);
        if (horizon != null && horizon > 0) {
            forecasts = forecasts.stream().limit(horizon).toList();
        }
        return forecasts.stream()
                .filter(f -> f.getScopeKey() == null || !"recurring".equals(f.getScopeKey()))
                .map(ForecastDto::from)
                .toList();
    }

    @GetMapping("/category/{slug}")
    public List<ForecastDto> categoryForecasts(@PathVariable String slug) {
        return forecastRepository.findByScopeAndScopeKeyOrderByForecastDateAsc(
                        ForecastScope.CATEGORY, slug)
                .stream()
                .map(ForecastDto::from)
                .toList();
    }

    @GetMapping("/category-spending")
    public List<CategorySpendingDto> categorySpending() {
        List<Object[]> results = snapshotRepository.findMonthlyCategorySpending();
        List<CategorySpendingDto> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            Long categoryId = (Long) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            long transactionCount = ((Number) row[2]).longValue();
            
            SpendingCategory category = categoryRepository.findById(categoryId).orElse(null);
            String categoryName = category != null ? category.getDisplayName() : "Unknown";
            
            dtos.add(CategorySpendingDto.from(categoryId, categoryName, totalAmount, transactionCount));
        }
        
        return dtos;
    }
}
