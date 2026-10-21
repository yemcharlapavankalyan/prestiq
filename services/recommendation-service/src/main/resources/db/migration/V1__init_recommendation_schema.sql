-- ==============================================================================
-- Prestiq Recommendation Service: V1 Initial Schema
-- Database: prestiq_recommendation_db
-- ==============================================================================

-- 1. Student Personalized Recommendations
CREATE TABLE IF NOT EXISTS student_recommendations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    scholarship_id BIGINT NOT NULL,
    match_score NUMERIC(5,2) NOT NULL,
    eligibility_verdict VARCHAR(30) NOT NULL,
    eligibility_score NUMERIC(5,2) NOT NULL,
    rank_position INT NOT NULL DEFAULT 0,
    score_breakdown TEXT NOT NULL,
    rule_set_version INT NOT NULL DEFAULT 1,
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_student_schol_rec UNIQUE(student_id, scholarship_id)
);

CREATE INDEX IF NOT EXISTS idx_recommendations_student_id ON student_recommendations(student_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_active_rank ON student_recommendations(student_id, is_dismissed, match_score DESC);
CREATE INDEX IF NOT EXISTS idx_recommendations_scholarship_id ON student_recommendations(scholarship_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_calculated_at ON student_recommendations(calculated_at);

-- 2. Student Personalized Recommendation Preferences
CREATE TABLE IF NOT EXISTS student_preferences (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL UNIQUE,
    preferred_fields TEXT NOT NULL DEFAULT '[]',
    preferred_locations TEXT NOT NULL DEFAULT '[]',
    min_award_preference NUMERIC(12,2) NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_student_preferences_student_id ON student_preferences(student_id);
