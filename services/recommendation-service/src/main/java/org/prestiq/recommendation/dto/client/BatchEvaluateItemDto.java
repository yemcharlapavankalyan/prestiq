package org.prestiq.recommendation.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchEvaluateItemDto(
        Long scholarshipId,
        int ruleSetVersion,
        String verdict,
        BigDecimal matchPercentage,
        int passedCriteriaCount,
        int totalCriteriaCount,
        boolean hasMissingData
) {
}
