package org.prestiq.recommendation.dto.response;

import org.prestiq.recommendation.domain.entity.EligibilityVerdict;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScholarshipRecommendationResponse(
        UUID id,
        Long scholarshipId,
        String scholarshipTitle,
        String scholarshipSlug,
        String providerName,
        BigDecimal awardAmountMin,
        BigDecimal awardAmountMax,
        String currencyCode,
        LocalDate applicationDeadline,
        long daysUntilDeadline,
        EligibilityVerdict eligibilityVerdict,
        BigDecimal eligibilityScore,
        BigDecimal matchScore,
        int rankPosition,
        ScoreBreakdownDto scoreBreakdown,
        boolean isDismissed,
        Instant calculatedAt
) {
}
