package org.prestiq.notification.dto.request;

public record UpdatePreferencesRequest(
        Boolean emailEnabled,
        Boolean inAppEnabled,
        Boolean deadlineRemindersEnabled,
        Boolean applicationStatusEnabled,
        Boolean marketingAnnouncementsEnabled
) {
}
