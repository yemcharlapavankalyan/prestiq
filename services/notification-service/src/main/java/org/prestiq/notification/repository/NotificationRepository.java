package org.prestiq.notification.repository;

import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndTypeAndIsReadOrderByCreatedAtDesc(
            UUID userId,
            NotificationType type,
            boolean isRead,
            Pageable pageable
    );

    long countByUserIdAndIsReadFalse(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
