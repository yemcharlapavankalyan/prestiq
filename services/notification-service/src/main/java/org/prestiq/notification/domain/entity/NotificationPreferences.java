package org.prestiq.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "deadline_reminders_enabled", nullable = false)
    private boolean deadlineRemindersEnabled = true;

    @Column(name = "application_status_enabled", nullable = false)
    private boolean applicationStatusEnabled = true;

    @Column(name = "marketing_announcements_enabled", nullable = false)
    private boolean marketingAnnouncementsEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public NotificationPreferences() {
    }

    public NotificationPreferences(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.emailEnabled = false;
        this.inAppEnabled = true;
        this.deadlineRemindersEnabled = true;
        this.applicationStatusEnabled = true;
        this.marketingAnnouncementsEnabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isDeadlineRemindersEnabled() {
        return deadlineRemindersEnabled;
    }

    public void setDeadlineRemindersEnabled(boolean deadlineRemindersEnabled) {
        this.deadlineRemindersEnabled = deadlineRemindersEnabled;
    }

    public boolean isApplicationStatusEnabled() {
        return applicationStatusEnabled;
    }

    public void setApplicationStatusEnabled(boolean applicationStatusEnabled) {
        this.applicationStatusEnabled = applicationStatusEnabled;
    }

    public boolean isMarketingAnnouncementsEnabled() {
        return marketingAnnouncementsEnabled;
    }

    public void setMarketingAnnouncementsEnabled(boolean marketingAnnouncementsEnabled) {
        this.marketingAnnouncementsEnabled = marketingAnnouncementsEnabled;
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
        NotificationPreferences that = (NotificationPreferences) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
