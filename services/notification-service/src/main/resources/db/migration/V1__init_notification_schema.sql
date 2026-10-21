-- Flyway V1 Migration: PrestIQ Notification Service Schema
-- Tables: notifications, notification_preferences, deadline_reminders

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'DELIVERED',
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notifications_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications (type);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deadline_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    application_status_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_announcements_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_user ON notification_preferences (user_id);

CREATE TABLE IF NOT EXISTS deadline_reminders (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    student_id UUID NOT NULL,
    deadline_date DATE NOT NULL,
    reminder_window VARCHAR(50) NOT NULL,
    remind_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    dispatched_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_deadline_reminders UNIQUE (application_id, reminder_window, deadline_date)
);

CREATE INDEX IF NOT EXISTS idx_reminders_status_remind_at ON deadline_reminders (status, remind_at);
CREATE INDEX IF NOT EXISTS idx_reminders_student_id ON deadline_reminders (student_id);
CREATE INDEX IF NOT EXISTS idx_reminders_application_id ON deadline_reminders (application_id);
