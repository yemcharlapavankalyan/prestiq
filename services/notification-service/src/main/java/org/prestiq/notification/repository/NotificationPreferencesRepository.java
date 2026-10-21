package org.prestiq.notification.repository;

import org.prestiq.notification.domain.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {

    Optional<NotificationPreferences> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
