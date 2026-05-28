package com.financial.tracker.financial_transactions.analytics.merchant;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MerchantResolutionMetrics {

    private final AtomicInteger tier1Hits = new AtomicInteger();
    private final AtomicInteger tier2Hits = new AtomicInteger();
    private final AtomicInteger unresolved = new AtomicInteger();
    private final AtomicInteger promotedAliases = new AtomicInteger();
    private final AtomicInteger totalResolved = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicInteger> unresolvedDescriptors = new ConcurrentHashMap<>();

    public void reset() {
        tier1Hits.set(0);
        tier2Hits.set(0);
        unresolved.set(0);
        promotedAliases.set(0);
        totalResolved.set(0);
        unresolvedDescriptors.clear();
    }

    public void recordTier1Hit() {
        tier1Hits.incrementAndGet();
        totalResolved.incrementAndGet();
    }

    public void recordTier2Hit() {
        tier2Hits.incrementAndGet();
        totalResolved.incrementAndGet();
    }

    public void recordUnresolved(String rawDescription) {
        unresolved.incrementAndGet();
        if (rawDescription != null && !rawDescription.isBlank()) {
            unresolvedDescriptors
                    .computeIfAbsent(rawDescription.trim(), k -> new AtomicInteger())
                    .incrementAndGet();
        }
    }

    public void recordPromotion() {
        promotedAliases.incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int total = totalResolved.get() + unresolved.get();
        stats.put("totalProcessed", total);
        stats.put("tier1Hits", tier1Hits.get());
        stats.put("tier2Hits", tier2Hits.get());
        stats.put("unresolved", unresolved.get());
        stats.put("promotedAliases", promotedAliases.get());
        stats.put("tier1HitRate", rate(tier1Hits.get(), total));
        stats.put("tier2HitRate", rate(tier2Hits.get(), total));
        stats.put("unresolvedRate", rate(unresolved.get(), total));
        stats.put("topUnresolvedDescriptors", topUnresolved(10));
        return stats;
    }

    private static double rate(int count, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((count * 10000.0 / total)) / 10000.0;
    }

    private List<Map<String, Object>> topUnresolved(int limit) {
        return unresolvedDescriptors.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                .limit(limit)
                .map(e -> Map.<String, Object>of(
                        "descriptor", e.getKey(),
                        "count", e.getValue().get()
                ))
                .toList();
    }
}
