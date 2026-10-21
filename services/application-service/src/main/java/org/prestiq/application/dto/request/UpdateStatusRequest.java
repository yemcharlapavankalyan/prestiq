package org.prestiq.application.dto.request;

import jakarta.validation.constraints.NotNull;
import org.prestiq.application.domain.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateStatusRequest(
        @NotNull(message = "Target status is required")
        ApplicationStatus status,

        String notes,

        LocalDate actualSubmissionDate,

        BigDecimal awardAmountReceived
) {
}
