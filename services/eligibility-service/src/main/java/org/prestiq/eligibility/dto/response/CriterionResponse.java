package org.prestiq.eligibility.dto.response;

import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;

public record CriterionResponse(
        Long id,
        String criterionCode,
        String targetField,
        CriterionOperator operator,
        String targetValue,
        boolean isMandatory,
        int weight,
        String displayName,
        String passExplanationTemplate,
        String failExplanationTemplate
) {
    public static CriterionResponse fromEntity(EligibilityCriterion criterion) {
        return new CriterionResponse(
                criterion.getId(),
                criterion.getCriterionCode(),
                criterion.getTargetField(),
                criterion.getOperator(),
                criterion.getTargetValue(),
                criterion.isMandatory(),
                criterion.getWeight(),
                criterion.getDisplayName(),
                criterion.getPassExplanationTemplate(),
                criterion.getFailExplanationTemplate()
        );
    }
}
