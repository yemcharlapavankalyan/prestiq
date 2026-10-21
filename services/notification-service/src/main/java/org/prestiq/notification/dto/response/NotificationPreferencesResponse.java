package org.prestiq.notification.dto.response;

import org.prestiq.notification.domain.entity.NotificationPreferences;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferencesResponse(
        UUID userId,
        boolean emailEnabled,
        boolean inAppEnabled,
        boolean deadlineRemindersEnabled,
        boolean applicationStatusEnabled,
        boolean marketingAnnouncementsEnabled,
        Instant updatedAt
) {
    public static NotificationPreferencesResponse fromEntity(NotificationPreferences entity) {
        if (entity == null) return null;
        return new NotificationPreferencesResponse(
                entity.getUserId(),
                entity.isEmailEnabled(),
                entity.isInAppEnabled(),
                entity.isDeadlineRemindersEnabled(),
                entity.isApplicationStatusEnabled(),
                entity.isMarketingAnnouncementsEnabled(),
                entity.getUpdatedAt()
        );
    }
}
