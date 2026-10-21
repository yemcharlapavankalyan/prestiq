package org.prestiq.eligibility.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.prestiq.eligibility.domain.entity.CriterionOperator;

public record CreateCriterionRequest(
        @NotBlank(message = "Criterion code cannot be blank")
        String criterionCode,

        @NotBlank(message = "Target field cannot be blank")
        String targetField,

        @NotNull(message = "Operator must be specified")
        CriterionOperator operator,

        @NotBlank(message = "Target value cannot be blank")
        String targetValue,

        Boolean isMandatory,

        Integer weight,

        @NotBlank(message = "Display name cannot be blank")
        String displayName,

        String passExplanationTemplate,

        String failExplanationTemplate
) {
    public boolean getIsMandatory() {
        return isMandatory != null ? isMandatory : true;
    }

    public int getWeight() {
        return weight != null ? weight : 0;
    }
}
