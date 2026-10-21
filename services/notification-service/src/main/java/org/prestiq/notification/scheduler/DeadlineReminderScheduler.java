package org.prestiq.notification.scheduler;

import org.prestiq.notification.config.ReminderProperties;
import org.prestiq.notification.service.DeadlineReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "prestiq.notifications.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DeadlineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

    private final DeadlineReminderService reminderService;
    private final ReminderProperties reminderProperties;

    public DeadlineReminderScheduler(
            DeadlineReminderService reminderService,
            ReminderProperties reminderProperties
    ) {
        this.reminderService = reminderService;
        this.reminderProperties = reminderProperties;
    }

    @Scheduled(cron = "${prestiq.notifications.scheduler.cron:0 0 * * * *}")
    public void runDeadlineCountdownEngine() {
        log.info("Starting scheduled deadline reminder processing execution...");
        try {
            int processed = reminderService.processDueReminders(reminderProperties.getBatchSize());
            log.info("Completed scheduled deadline reminder processing. Total processed: {}", processed);
        } catch (Exception e) {
            log.error("Error occurred during scheduled deadline countdown run: {}", e.getMessage(), e);
        }
    }
}
