package org.prestiq.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record UpdateApplicationRequest(
        @Min(value = 0, message = "Progress percentage cannot be less than 0")
        @Max(value = 100, message = "Progress percentage cannot exceed 100")
        Integer progressPercentage,

        String notes,

        String externalPortalReference,

        LocalDate submissionDeadline
) {
}
