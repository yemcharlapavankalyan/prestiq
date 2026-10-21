package org.prestiq.recommendation.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record ScoreBreakdownDto(
        BigDecimal eligibilityScore,
        BigDecimal academicAffinityScore,
        BigDecimal deadlineUrgencyScore,
        BigDecimal awardFactorScore,
        Map<String, Double> weights,
        String explanation
) {
}
