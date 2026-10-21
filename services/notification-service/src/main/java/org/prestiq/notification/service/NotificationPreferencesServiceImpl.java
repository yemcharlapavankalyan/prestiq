package org.prestiq.notification.service;

import org.prestiq.notification.domain.entity.NotificationPreferences;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.request.UpdatePreferencesRequest;
import org.prestiq.notification.dto.response.NotificationPreferencesResponse;
import org.prestiq.notification.repository.NotificationPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesServiceImpl.class);

    private final NotificationPreferencesRepository preferencesRepository;

    public NotificationPreferencesServiceImpl(NotificationPreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    @Override
    @Transactional
    public NotificationPreferences getOrCreatePreferencesEntity(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating default notification preferences for user {}", userId);
                    NotificationPreferences defaultPreferences = new NotificationPreferences(userId);
                    return preferencesRepository.save(defaultPreferences);
                });
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse getPreferences(UUID userId) {
        NotificationPreferences entity = getOrCreatePreferencesEntity(userId);
        return NotificationPreferencesResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        NotificationPreferences prefs = getOrCreatePreferencesEntity(userId);

        if (request != null) {
            if (request.emailEnabled() != null) {
                prefs.setEmailEnabled(request.emailEnabled());
            }
            if (request.inAppEnabled() != null) {
                prefs.setInAppEnabled(request.inAppEnabled());
            }
            if (request.deadlineRemindersEnabled() != null) {
                prefs.setDeadlineRemindersEnabled(request.deadlineRemindersEnabled());
            }
            if (request.applicationStatusEnabled() != null) {
                prefs.setApplicationStatusEnabled(request.applicationStatusEnabled());
            }
            if (request.marketingAnnouncementsEnabled() != null) {
                prefs.setMarketingAnnouncementsEnabled(request.marketingAnnouncementsEnabled());
            }
        }

        prefs.setUpdatedAt(Instant.now());
        NotificationPreferences updated = preferencesRepository.save(prefs);
        log.info("Updated notification preferences for user {}", userId);

        return NotificationPreferencesResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationAllowed(UUID userId, NotificationType type) {
        NotificationPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
        if (prefs == null) {
            // Safe default: allowed if user has not yet customized
            return true;
        }

        if (!prefs.isInAppEnabled()) {
            return false;
        }

        return switch (type) {
            case DEADLINE_REMINDER -> prefs.isDeadlineRemindersEnabled();
            case APPLICATION_STATUS_UPDATE -> prefs.isApplicationStatusEnabled();
            case SCHOLARSHIP_ANNOUNCEMENT -> prefs.isMarketingAnnouncementsEnabled();
            case SYSTEM -> true;
        };
    }
}
