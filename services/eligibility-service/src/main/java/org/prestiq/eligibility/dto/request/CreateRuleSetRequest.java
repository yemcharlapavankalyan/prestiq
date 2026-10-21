package org.prestiq.eligibility.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;

import java.util.List;

public record CreateRuleSetRequest(
        @NotNull(message = "Scholarship ID must be specified")
        Long scholarshipId,

        Integer version,

        RuleSetStatus status,

        @NotEmpty(message = "Rule set must contain at least one criterion")
        @Valid
        List<CreateCriterionRequest> criteria
) {
    public RuleSetStatus getEffectiveStatus() {
        return status != null ? status : RuleSetStatus.ACTIVE;
    }
}
