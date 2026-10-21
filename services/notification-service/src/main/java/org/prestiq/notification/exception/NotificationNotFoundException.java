package org.prestiq.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID id) {
        super("Notification with ID " + id + " was not found or does not belong to the authenticated user");
    }
}
