package org.prestiq.eligibility.dto.request;

import jakarta.validation.constraints.NotNull;
import org.prestiq.eligibility.dto.response.StudentProfileDto;

public record EvaluateStudentRequest(
        @NotNull(message = "Scholarship ID must be specified")
        Long scholarshipId,

        StudentProfileDto studentProfile
) {
}
