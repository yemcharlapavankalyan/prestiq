package org.prestiq.notification.dto.response;

import org.prestiq.notification.domain.entity.DeliveryStatus;
import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        boolean isRead,
        Instant readAt,
        DeliveryStatus deliveryStatus,
        Instant createdAt
) {
    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) return null;
        return new NotificationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRelatedEntityType(),
                entity.getRelatedEntityId(),
                entity.isRead(),
                entity.getReadAt(),
                entity.getDeliveryStatus(),
                entity.getCreatedAt()
        );
    }
}
