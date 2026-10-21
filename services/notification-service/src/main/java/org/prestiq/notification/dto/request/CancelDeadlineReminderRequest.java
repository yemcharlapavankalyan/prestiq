package org.prestiq.notification.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelDeadlineReminderRequest(
        @NotNull(message = "applicationId is required")
        UUID applicationId
) {
}
