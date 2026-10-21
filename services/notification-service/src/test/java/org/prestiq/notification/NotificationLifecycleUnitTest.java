package org.prestiq.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.notification.domain.entity.DeadlineReminder;
import org.prestiq.notification.domain.entity.DeliveryStatus;
import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationPreferences;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.domain.entity.ReminderStatus;
import org.prestiq.notification.domain.entity.ReminderWindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationLifecycleUnitTest {

    @Test
    @DisplayName("ReminderWindow returns correct days and window code")
    void testReminderWindowValues() {
        assertEquals(30, ReminderWindow.DAYS_30.getDays());
        assertEquals("30_DAYS_BEFORE", ReminderWindow.DAYS_30.getWindowCode());

        assertEquals(7, ReminderWindow.DAYS_7.getDays());
        assertEquals("7_DAYS_BEFORE", ReminderWindow.DAYS_7.getWindowCode());

        assertEquals(1, ReminderWindow.DAY_1.getDays());
        assertEquals("1_DAY_BEFORE", ReminderWindow.DAY_1.getWindowCode());

        assertEquals(ReminderWindow.DAYS_30, ReminderWindow.fromDays(30));
        assertEquals(ReminderWindow.DAYS_7, ReminderWindow.fromDays(7));
        assertEquals(ReminderWindow.DAY_1, ReminderWindow.fromDays(1));

        assertThrows(IllegalArgumentException.class, () -> ReminderWindow.fromDays(15));
    }

    @Test
    @DisplayName("Notification entity marks as read correctly")
    void testNotificationMarkAsRead() {
        UUID userId = UUID.randomUUID();
        Notification notification = new Notification(
                UUID.randomUUID(),
                userId,
                NotificationType.DEADLINE_REMINDER,
                "Deadline Approaching",
                "Your application deadline is tomorrow",
                "APPLICATION",
                UUID.randomUUID().toString(),
                "test-key"
        );

        assertFalse(notification.isRead());
        assertEquals(DeliveryStatus.DELIVERED, notification.getDeliveryStatus());

        Instant now = Instant.now();
        notification.markAsRead(now);

        assertTrue(notification.isRead());
        assertEquals(now, notification.getReadAt());
    }

    @Test
    @DisplayName("DeadlineReminder entity transitions between lifecycle statuses")
    void testDeadlineReminderLifecycle() {
        UUID appId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDate deadline = LocalDate.of(2026, 12, 1);
        Instant remindAt = Instant.now().plusSeconds(3600);

        DeadlineReminder reminder = new DeadlineReminder(
                appId, studentId, deadline, "7_DAYS_BEFORE", remindAt
        );

        assertEquals(ReminderStatus.PENDING, reminder.getStatus());

        reminder.markDispatched(Instant.now());
        assertEquals(ReminderStatus.DISPATCHED, reminder.getStatus());
        assertNotNull(reminder.getDispatchedAt());

        reminder.markCancelled();
        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());

        reminder.markSkipped();
        assertEquals(ReminderStatus.SKIPPED, reminder.getStatus());
    }

    @Test
    @DisplayName("NotificationPreferences has safe defaults enabled")
    void testNotificationPreferencesDefaults() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences prefs = new NotificationPreferences(userId);

        assertEquals(userId, prefs.getUserId());
        assertTrue(prefs.isInAppEnabled());
        assertTrue(prefs.isDeadlineRemindersEnabled());
        assertTrue(prefs.isApplicationStatusEnabled());
        assertTrue(prefs.isMarketingAnnouncementsEnabled());
        assertFalse(prefs.isEmailEnabled());
    }
}
