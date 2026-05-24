package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TierOneMerchantResolver {

    private final MerchantAliasRepository aliasRepository;
    private final MerchantNormalizer normalizer;

    public TierOneMerchantResolver(MerchantAliasRepository aliasRepository, MerchantNormalizer normalizer) {
        this.aliasRepository = aliasRepository;
        this.normalizer = normalizer;
    }

    @Cacheable(value = "merchantAliasCache", key = "#rawDescription")
    public Optional<ResolvedMerchant> resolve(String rawDescription) {
        String normalized = normalizer.normalize(rawDescription);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return aliasRepository.findByNormalizedName(normalized)
                .map(alias -> new ResolvedMerchant(
                        alias.getMerchant().getId(),
                        alias.getMerchant().getCanonicalName(),
                        alias.getMerchant().getCategory(),
                        alias.getMerchant().getSubcategory(),
                        ResolutionTier.EXACT_MATCH,
                        1.0,
                        normalized
                ));
    }
}
