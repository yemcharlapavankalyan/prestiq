package org.prestiq.notification.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleDeadlineReminderRequest(
        @NotNull(message = "applicationId is required")
        UUID applicationId,

        @NotNull(message = "studentId is required")
        UUID studentId,

        @NotNull(message = "deadline is required")
        LocalDate deadline,

        String scholarshipTitle
) {
}
