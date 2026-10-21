package org.prestiq.application.dto.response;

import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.domain.entity.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        Long id,
        UUID applicationId,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        UUID changedByUserId,
        Instant changedAt,
        String notes
) {
    public static StatusHistoryResponse fromEntity(ApplicationStatusHistory h) {
        if (h == null) return null;
        return new StatusHistoryResponse(
                h.getId(),
                h.getApplicationId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getChangedByUserId(),
                h.getChangedAt(),
                h.getNotes()
        );
    }
}
