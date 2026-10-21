package org.prestiq.application.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record DeadlineReminderRequestDto(
        UUID applicationId,
        UUID studentId,
        LocalDate deadline
) {
}
