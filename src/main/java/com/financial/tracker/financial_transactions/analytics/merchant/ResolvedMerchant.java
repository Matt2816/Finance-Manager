package com.financial.tracker.financial_transactions.analytics.merchant;

import java.util.UUID;

public record ResolvedMerchant(
        UUID merchantId,
        String canonicalName,
        String category,
        String subcategory,
        ResolutionTier resolvedBy,
        double confidence,
        String normalizedInput
) {
}
