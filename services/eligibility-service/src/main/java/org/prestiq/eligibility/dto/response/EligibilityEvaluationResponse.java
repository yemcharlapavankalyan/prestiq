package org.prestiq.eligibility.dto.response;

import org.prestiq.eligibility.domain.entity.EligibilityEvaluation;
import org.prestiq.eligibility.domain.entity.Verdict;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EligibilityEvaluationResponse(
        UUID id,
        Long scholarshipId,
        UUID studentId,
        int ruleSetVersion,
        Verdict verdict,
        BigDecimal matchPercentage,
        List<CriterionResultResponse> criteriaBreakdown,
        Instant evaluatedAt
) {
    public static EligibilityEvaluationResponse fromEntity(EligibilityEvaluation entity) {
        return new EligibilityEvaluationResponse(
                entity.getId(),
                entity.getScholarshipId(),
                entity.getStudentId(),
                entity.getRuleSetVersion(),
                entity.getVerdict(),
                entity.getMatchPercentage(),
                entity.getCriteriaBreakdown(),
                entity.getEvaluatedAt()
        );
    }
}
