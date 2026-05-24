package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantAliasRepository;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TierTwoMerchantResolver {

    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.82;
    private static final double JARO_WINKLER_WEIGHT = 0.40;
    private static final double TOKEN_SORT_WEIGHT = 0.60;

    private final MerchantAliasRepository aliasRepository;
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    @Value("${merchant.tier2.confidence-threshold:0.82}")
    private double confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;

    public TierTwoMerchantResolver(MerchantAliasRepository aliasRepository) {
        this.aliasRepository = aliasRepository;
    }

    public Optional<ResolvedMerchant> resolve(String normalizedInput) {
        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }

        List<MerchantAlias> candidates = aliasRepository.findCandidatesByTrigram(normalizedInput, 10);

        return candidates.stream()
                .map(c -> score(normalizedInput, c))
                .filter(s -> s.confidence() >= confidenceThreshold)
                .max(Comparator.comparingDouble(ScoredCandidate::confidence))
                .map(best -> new ResolvedMerchant(
                        best.merchantId(),
                        best.canonicalName(),
                        best.category(),
                        best.subcategory(),
                        ResolutionTier.FUZZY_MATCH,
                        best.confidence(),
                        normalizedInput
                ));
    }

    private ScoredCandidate score(String input, MerchantAlias candidate) {
        String candidateName = candidate.getNormalizedName();

        double jw = jaroWinkler.apply(input, candidateName);

        String sortedInput = sortTokens(input);
        String sortedCandidate = sortTokens(candidateName);
        double tokenSort = jaroWinkler.apply(sortedInput, sortedCandidate);

        double combined = (jw * JARO_WINKLER_WEIGHT) + (tokenSort * TOKEN_SORT_WEIGHT);

        return new ScoredCandidate(
                candidate.getMerchant().getId(),
                candidate.getMerchant().getCanonicalName(),
                candidate.getMerchant().getCategory(),
                candidate.getMerchant().getSubcategory(),
                combined
        );
    }

    private String sortTokens(String input) {
        String[] tokens = input.split("\\s+");
        Arrays.sort(tokens);
        return String.join(" ", tokens);
    }

    private record ScoredCandidate(
            java.util.UUID merchantId,
            String canonicalName,
            String category,
            String subcategory,
            double confidence
    ) {
    }
}
