package org.prestiq.notification.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ApplicationClientDto(
        UUID id,
        UUID studentId,
        Long scholarshipId,
        String scholarshipTitle,
        String scholarshipProvider,
        String currentStatus,
        LocalDate submissionDeadline
) {
}
