package org.prestiq.notification.dto.response;

import org.prestiq.notification.domain.entity.DeadlineReminder;
import org.prestiq.notification.domain.entity.ReminderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledReminderResponse(
        UUID reminderId,
        UUID applicationId,
        UUID studentId,
        String reminderWindow,
        LocalDate deadlineDate,
        Instant remindAt,
        ReminderStatus status
) {
    public static ScheduledReminderResponse fromEntity(DeadlineReminder entity) {
        if (entity == null) return null;
        return new ScheduledReminderResponse(
                entity.getId(),
                entity.getApplicationId(),
                entity.getStudentId(),
                entity.getReminderWindow(),
                entity.getDeadlineDate(),
                entity.getRemindAt(),
                entity.getStatus()
        );
    }
}
