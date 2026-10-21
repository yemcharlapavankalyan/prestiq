package org.prestiq.notification.service;

import org.prestiq.notification.domain.entity.NotificationPreferences;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.request.UpdatePreferencesRequest;
import org.prestiq.notification.dto.response.NotificationPreferencesResponse;

import java.util.UUID;

public interface NotificationPreferencesService {

    NotificationPreferences getOrCreatePreferencesEntity(UUID userId);

    NotificationPreferencesResponse getPreferences(UUID userId);

    NotificationPreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request);

    boolean isNotificationAllowed(UUID userId, NotificationType type);
}
