package org.prestiq.notification.service;

import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.response.MarkAllReadResponse;
import org.prestiq.notification.dto.response.NotificationResponse;
import org.prestiq.notification.dto.response.PagedResponse;
import org.prestiq.notification.dto.response.UnreadCountResponse;
import org.prestiq.notification.exception.NotificationNotFoundException;
import org.prestiq.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesService preferencesService;
    private final Clock clock;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationPreferencesService preferencesService,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.preferencesService = preferencesService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(
            UUID userId,
            Boolean unreadOnly,
            NotificationType type,
            Pageable pageable
    ) {
        Page<Notification> page;

        boolean filterUnread = Boolean.TRUE.equals(unreadOnly);

        if (type != null && filterUnread) {
            page = notificationRepository.findByUserIdAndTypeAndIsReadOrderByCreatedAtDesc(userId, type, false, pageable);
        } else if (type != null) {
            page = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else if (filterUnread) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        Page<NotificationResponse> responsePage = page.map(NotificationResponse::fromEntity);
        return PagedResponse.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        if (!notification.isRead()) {
            notification.markAsRead(clock.instant());
            notification = notificationRepository.save(notification);
            log.info("Marked notification {} as read for user {}", id, userId);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public MarkAllReadResponse markAllAsRead(UUID userId) {
        int markedCount = notificationRepository.markAllAsReadForUser(userId, clock.instant());
        log.info("Marked {} notifications as read for user {}", markedCount, userId);
        return new MarkAllReadResponse(markedCount);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID id, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", id, userId);
    }

    @Override
    @Transactional
    public Optional<Notification> createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            String relatedEntityId,
            String idempotencyKey
    ) {
        // 1. Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Notification with idempotency key {} already exists. Skipping duplicate dispatch.", idempotencyKey);
                return Optional.empty();
            }
        }

        // 2. Preferences check
        if (!preferencesService.isNotificationAllowed(userId, type)) {
            log.info("Notification of type {} suppressed due to user preferences for user {}", type, userId);
            return Optional.empty();
        }

        // 3. Persist notification
        Notification notification = new Notification(
                UUID.randomUUID(),
                userId,
                type,
                title,
                message,
                relatedEntityType,
                relatedEntityId,
                idempotencyKey
        );
        notification.setCreatedAt(clock.instant());

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app notification {} of type {} for user {}", saved.getId(), type, userId);
        return Optional.of(saved);
    }
}
