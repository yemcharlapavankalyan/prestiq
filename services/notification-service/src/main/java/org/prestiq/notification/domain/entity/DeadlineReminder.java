package org.prestiq.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "deadline_reminders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_deadline_reminders",
                        columnNames = {"application_id", "reminder_window", "deadline_date"}
                )
        }
)
public class DeadlineReminder {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "reminder_window", nullable = false, length = 50)
    private String reminderWindow;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DeadlineReminder() {
    }

    public DeadlineReminder(
            UUID applicationId,
            UUID studentId,
            LocalDate deadlineDate,
            String reminderWindow,
            Instant remindAt
    ) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.studentId = studentId;
        this.deadlineDate = deadlineDate;
        this.reminderWindow = reminderWindow;
        this.remindAt = remindAt;
        this.status = ReminderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markDispatched(Instant at) {
        this.status = ReminderStatus.DISPATCHED;
        this.dispatchedAt = at != null ? at : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = ReminderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void markSkipped() {
        this.status = ReminderStatus.SKIPPED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = ReminderStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public String getReminderWindow() {
        return reminderWindow;
    }

    public void setReminderWindow(String reminderWindow) {
        this.reminderWindow = reminderWindow;
    }

    public Instant getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(Instant remindAt) {
        this.remindAt = remindAt;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public void setStatus(ReminderStatus status) {
        this.status = status;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeadlineReminder that = (DeadlineReminder) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
