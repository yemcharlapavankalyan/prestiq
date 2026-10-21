package org.prestiq.notification.service;

import org.prestiq.notification.dto.request.ScheduleDeadlineReminderRequest;
import org.prestiq.notification.dto.response.ScheduledReminderResponse;

import java.util.List;
import java.util.UUID;

public interface DeadlineReminderService {

    List<ScheduledReminderResponse> scheduleReminders(ScheduleDeadlineReminderRequest request);

    int cancelReminders(UUID applicationId);

    int processDueReminders(int batchSize);
}
