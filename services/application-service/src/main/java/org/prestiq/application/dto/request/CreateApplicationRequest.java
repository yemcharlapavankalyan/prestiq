package org.prestiq.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotNull(message = "Scholarship ID is required")
        Long scholarshipId,

        String scholarshipTitle,

        String scholarshipProvider,

        LocalDate submissionDeadline,

        String externalPortalReference,

        String notes
) {
}
