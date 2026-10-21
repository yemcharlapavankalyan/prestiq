package org.prestiq.notification.service;

import org.prestiq.notification.client.ApplicationServiceClient;
import org.prestiq.notification.client.dto.ApplicationClientDto;
import org.prestiq.notification.config.ReminderProperties;
import org.prestiq.notification.domain.entity.DeadlineReminder;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.domain.entity.ReminderStatus;
import org.prestiq.notification.dto.request.ScheduleDeadlineReminderRequest;
import org.prestiq.notification.dto.response.ScheduledReminderResponse;
import org.prestiq.notification.repository.DeadlineReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeadlineReminderServiceImpl implements DeadlineReminderService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderServiceImpl.class);

    private final DeadlineReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final NotificationPreferencesService preferencesService;
    private final ApplicationServiceClient applicationServiceClient;
    private final ReminderProperties reminderProperties;
    private final Clock clock;

    public DeadlineReminderServiceImpl(
            DeadlineReminderRepository reminderRepository,
            NotificationService notificationService,
            NotificationPreferencesService preferencesService,
            ApplicationServiceClient applicationServiceClient,
            ReminderProperties reminderProperties,
            Clock clock
    ) {
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.preferencesService = preferencesService;
        this.applicationServiceClient = applicationServiceClient;
        this.reminderProperties = reminderProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<ScheduledReminderResponse> scheduleReminders(ScheduleDeadlineReminderRequest request) {
        List<ScheduledReminderResponse> scheduled = new ArrayList<>();
        List<Integer> windows = reminderProperties.getParsedWindows();
        LocalDate deadline = request.deadline();
        Instant now = clock.instant();

        for (int days : windows) {
            String windowCode = (days == 1) ? "1_DAY_BEFORE" : days + "_DAYS_BEFORE";
            LocalDate targetDate = deadline.minusDays(days);
            Instant remindAt = targetDate.atTime(9, 0).atZone(ZoneOffset.UTC).toInstant();

            // Do not schedule if target remind timestamp has already passed
            if (remindAt.isBefore(now)) {
                log.debug("Skipping reminder window {} for deadline {} because remindAt {} has already passed",
                        windowCode, deadline, remindAt);
                continue;
            }

            // Deduplication: concurrency-safe check before insert (in addition to table constraint)
            boolean alreadyExists = reminderRepository.existsByApplicationIdAndReminderWindowAndDeadlineDate(
                    request.applicationId(),
                    windowCode,
                    deadline
            );

            if (alreadyExists) {
                log.info("Reminder for application {} window {} and deadline {} already exists. Skipping duplicate.",
                        request.applicationId(), windowCode, deadline);
                reminderRepository.findByApplicationIdAndReminderWindowAndDeadlineDate(
                        request.applicationId(), windowCode, deadline
                ).ifPresent(r -> scheduled.add(ScheduledReminderResponse.fromEntity(r)));
                continue;
            }

            DeadlineReminder reminder = new DeadlineReminder(
                    request.applicationId(),
                    request.studentId(),
                    deadline,
                    windowCode,
                    remindAt
            );
            reminder.setCreatedAt(now);
            reminder.setUpdatedAt(now);

            DeadlineReminder saved = reminderRepository.save(reminder);
            log.info("Scheduled deadline reminder {} for application {} at {}", saved.getId(), request.applicationId(), remindAt);
            scheduled.add(ScheduledReminderResponse.fromEntity(saved));
        }

        return scheduled;
    }

    @Override
    @Transactional
    public int cancelReminders(UUID applicationId) {
        List<DeadlineReminder> pending = reminderRepository.findByApplicationIdAndStatus(
                applicationId,
                ReminderStatus.PENDING
        );

        for (DeadlineReminder reminder : pending) {
            reminder.markCancelled();
            reminderRepository.save(reminder);
        }

        log.info("Cancelled {} pending reminders for application {}", pending.size(), applicationId);
        return pending.size();
    }

    @Override
    @Transactional
    public int processDueReminders(int batchSize) {
        Instant now = clock.instant();
        int effectiveBatchSize = batchSize > 0 ? batchSize : reminderProperties.getBatchSize();
        Pageable pageable = PageRequest.of(0, effectiveBatchSize);

        Page<DeadlineReminder> duePage = reminderRepository
                .findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(ReminderStatus.PENDING, now, pageable);

        List<DeadlineReminder> dueReminders = duePage.getContent();
        if (dueReminders.isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        for (DeadlineReminder reminder : dueReminders) {
            try {
                // 1. Check user preferences
                if (!preferencesService.isNotificationAllowed(reminder.getStudentId(), NotificationType.DEADLINE_REMINDER)) {
                    log.info("User {} has disabled deadline reminders. Marking reminder {} as SKIPPED.",
                            reminder.getStudentId(), reminder.getId());
                    reminder.markSkipped();
                    reminderRepository.save(reminder);
                    processedCount++;
                    continue;
                }

                // 2. Fetch application details with fallback
                Optional<ApplicationClientDto> appOpt = applicationServiceClient.getApplicationById(reminder.getApplicationId());
                String scholarshipTitle = appOpt
                        .map(ApplicationClientDto::scholarshipTitle)
                        .filter(s -> !s.isBlank())
                        .orElse("Scholarship Application");

                String humanWindow = formatHumanWindow(reminder.getReminderWindow());
                String title = "Deadline Reminder: " + scholarshipTitle;
                String message = String.format(
                        "Your application deadline is approaching in %s (due on %s). Please complete and submit your application.",
                        humanWindow, reminder.getDeadlineDate()
                );

                String idempotencyKey = "REMINDER:" + reminder.getId();

                // 3. Dispatch in-app notification
                notificationService.createNotification(
                        reminder.getStudentId(),
                        NotificationType.DEADLINE_REMINDER,
                        title,
                        message,
                        "APPLICATION",
                        reminder.getApplicationId().toString(),
                        idempotencyKey
                );

                // 4. Mark reminder as DISPATCHED
                reminder.markDispatched(now);
                reminderRepository.save(reminder);
                processedCount++;

                log.info("Successfully dispatched reminder {} for application {}", reminder.getId(), reminder.getApplicationId());
            } catch (Exception e) {
                log.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage(), e);
                reminder.markFailed();
                reminderRepository.save(reminder);
            }
        }

        return processedCount;
    }

    private String formatHumanWindow(String windowCode) {
        if ("1_DAY_BEFORE".equalsIgnoreCase(windowCode)) {
            return "1 day";
        } else if ("7_DAYS_BEFORE".equalsIgnoreCase(windowCode)) {
            return "7 days";
        } else if ("30_DAYS_BEFORE".equalsIgnoreCase(windowCode)) {
            return "30 days";
        }
        return windowCode.replace("_", " ").toLowerCase();
    }
}
