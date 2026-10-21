package org.prestiq.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplicationStatusNotificationRequest(
        @NotNull(message = "applicationId is required")
        UUID applicationId,

        @NotNull(message = "studentId is required")
        UUID studentId,

        String scholarshipTitle,

        @NotBlank(message = "fromStatus is required")
        String fromStatus,

        @NotBlank(message = "toStatus is required")
        String toStatus,

        String idempotencyKey
) {
}
