-- ==============================================================================
-- Prestiq Application Service: V1 Initial Schema
-- Database: prestiq_application_db
-- ==============================================================================

-- 1. Scholarship Applications
CREATE TABLE IF NOT EXISTS scholarship_applications (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    scholarship_id BIGINT NOT NULL,
    scholarship_title VARCHAR(255) NOT NULL,
    scholarship_provider VARCHAR(255) NULL,
    current_status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    external_portal_reference VARCHAR(255) NULL,
    submission_deadline DATE NOT NULL,
    actual_submission_date DATE NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NULL,
    award_amount_received NUMERIC(12,2) NULL,
    progress_percentage INT NOT NULL DEFAULT 0,
    notes TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Composite index for student and scholarship lookup
CREATE INDEX IF NOT EXISTS idx_applications_student_schol 
    ON scholarship_applications(student_id, scholarship_id);

CREATE INDEX IF NOT EXISTS idx_applications_student_id ON scholarship_applications(student_id);
CREATE INDEX IF NOT EXISTS idx_applications_student_status ON scholarship_applications(student_id, current_status);
CREATE INDEX IF NOT EXISTS idx_applications_deadline ON scholarship_applications(student_id, submission_deadline);
CREATE INDEX IF NOT EXISTS idx_applications_scholarship_id ON scholarship_applications(scholarship_id);

-- 2. Application Status Transition History (Audit Trail)
CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGSERIAL PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES scholarship_applications(id) ON DELETE CASCADE,
    from_status VARCHAR(50) NULL,
    to_status VARCHAR(50) NOT NULL,
    changed_by_user_id UUID NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_status_history_application_id ON application_status_history(application_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON application_status_history(changed_at);

-- 3. Saved Scholarships (Bookmarks / Favorites)
CREATE TABLE IF NOT EXISTS saved_scholarships (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL,
    scholarship_id BIGINT NOT NULL,
    scholarship_title VARCHAR(255) NULL,
    scholarship_provider VARCHAR(255) NULL,
    application_deadline DATE NULL,
    award_amount_max NUMERIC(12,2) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_student_saved_scholarship UNIQUE(student_id, scholarship_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_scholarships_student_id ON saved_scholarships(student_id);
CREATE INDEX IF NOT EXISTS idx_saved_scholarships_scholarship_id ON saved_scholarships(scholarship_id);
