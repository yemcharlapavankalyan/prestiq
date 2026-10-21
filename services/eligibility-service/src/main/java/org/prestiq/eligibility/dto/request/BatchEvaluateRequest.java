package org.prestiq.eligibility.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.prestiq.eligibility.dto.response.StudentProfileDto;

import java.util.List;

public record BatchEvaluateRequest(
        @NotNull(message = "Student profile must be provided for batch evaluation")
        StudentProfileDto studentProfile,

        @NotEmpty(message = "Scholarship IDs list cannot be empty")
        List<Long> scholarshipIds
) {
}
