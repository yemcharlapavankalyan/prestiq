package org.prestiq.notification.service;

import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.response.MarkAllReadResponse;
import org.prestiq.notification.dto.response.NotificationResponse;
import org.prestiq.notification.dto.response.PagedResponse;
import org.prestiq.notification.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface NotificationService {

    PagedResponse<NotificationResponse> getNotifications(
            UUID userId,
            Boolean unreadOnly,
            NotificationType type,
            Pageable pageable
    );

    UnreadCountResponse getUnreadCount(UUID userId);

    NotificationResponse markAsRead(UUID id, UUID userId);

    MarkAllReadResponse markAllAsRead(UUID userId);

    void deleteNotification(UUID id, UUID userId);

    Optional<Notification> createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            String relatedEntityId,
            String idempotencyKey
    );
}
