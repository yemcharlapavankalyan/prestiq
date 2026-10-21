package org.prestiq.eligibility.dto.response;

import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleSetResponse(
        Long id,
        Long scholarshipId,
        int version,
        RuleSetStatus status,
        UUID createdByUserId,
        Instant createdAt,
        List<CriterionResponse> criteria
) {
    public static RuleSetResponse fromEntity(ScholarshipRuleSet ruleSet) {
        List<CriterionResponse> criteriaResponses = ruleSet.getCriteria() != null
                ? ruleSet.getCriteria().stream().map(CriterionResponse::fromEntity).toList()
                : List.of();

        return new RuleSetResponse(
                ruleSet.getId(),
                ruleSet.getScholarshipId(),
                ruleSet.getVersion(),
                ruleSet.getStatus(),
                ruleSet.getCreatedByUserId(),
                ruleSet.getCreatedAt(),
                criteriaResponses
        );
    }
}
