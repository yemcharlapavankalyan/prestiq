package org.prestiq.notification.repository;

import org.prestiq.notification.domain.entity.DeadlineReminder;
import org.prestiq.notification.domain.entity.ReminderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadlineReminderRepository extends JpaRepository<DeadlineReminder, UUID> {

    Optional<DeadlineReminder> findByApplicationIdAndReminderWindowAndDeadlineDate(
            UUID applicationId,
            String reminderWindow,
            LocalDate deadlineDate
    );

    boolean existsByApplicationIdAndReminderWindowAndDeadlineDate(
            UUID applicationId,
            String reminderWindow,
            LocalDate deadlineDate
    );

    Page<DeadlineReminder> findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
            ReminderStatus status,
            Instant remindAt,
            Pageable pageable
    );

    List<DeadlineReminder> findByApplicationId(UUID applicationId);

    List<DeadlineReminder> findByApplicationIdAndStatus(UUID applicationId, ReminderStatus status);
}
