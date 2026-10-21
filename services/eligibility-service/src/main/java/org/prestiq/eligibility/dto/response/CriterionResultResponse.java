package org.prestiq.eligibility.dto.response;

import org.prestiq.eligibility.domain.entity.CriterionStatus;

public record CriterionResultResponse(
        String criterionCode,
        String displayName,
        String targetField,
        boolean isMandatory,
        CriterionStatus status,
        Object studentValue,
        Object requiredValue,
        String explanation,
        String remediationAction
) {
}
