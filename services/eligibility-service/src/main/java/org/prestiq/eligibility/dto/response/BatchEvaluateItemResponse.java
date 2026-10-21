package org.prestiq.eligibility.dto.response;

import org.prestiq.eligibility.domain.entity.Verdict;

import java.math.BigDecimal;

public record BatchEvaluateItemResponse(
        Long scholarshipId,
        int ruleSetVersion,
        Verdict verdict,
        BigDecimal matchPercentage,
        int passedCriteriaCount,
        int totalCriteriaCount,
        boolean hasMissingData
) {
}
