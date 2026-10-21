-- ==============================================================================
-- Prestiq Eligibility Service: V1 Initial Schema
-- Database: prestiq_eligibility_db
-- ==============================================================================

-- 1. Versioned Scholarship Rule Sets
CREATE TABLE IF NOT EXISTS scholarship_rule_sets (
    id BIGSERIAL PRIMARY KEY,
    scholarship_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_rule_version UNIQUE(scholarship_id, version)
);

CREATE INDEX IF NOT EXISTS idx_rule_sets_scholarship_id ON scholarship_rule_sets(scholarship_id);
CREATE INDEX IF NOT EXISTS idx_rule_sets_status ON scholarship_rule_sets(status);

-- 2. Structured Eligibility Criteria
CREATE TABLE IF NOT EXISTS eligibility_criteria (
    id BIGSERIAL PRIMARY KEY,
    rule_set_id BIGINT NOT NULL REFERENCES scholarship_rule_sets(id) ON DELETE CASCADE,
    criterion_code VARCHAR(50) NOT NULL,
    target_field VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    target_value TEXT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    weight INT NOT NULL DEFAULT 0,
    display_name VARCHAR(255) NOT NULL,
    pass_explanation_template VARCHAR(500) NULL,
    fail_explanation_template VARCHAR(500) NULL
);

CREATE INDEX IF NOT EXISTS idx_criteria_rule_set_id ON eligibility_criteria(rule_set_id);
CREATE INDEX IF NOT EXISTS idx_criteria_target_field ON eligibility_criteria(target_field);

-- 3. Persisted Evaluation Results & Audit Records
CREATE TABLE IF NOT EXISTS eligibility_evaluations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    scholarship_id BIGINT NOT NULL,
    rule_set_version INT NOT NULL,
    verdict VARCHAR(30) NOT NULL,
    match_percentage NUMERIC(5,2) NOT NULL,
    criteria_breakdown TEXT NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluations_student_id ON eligibility_evaluations(student_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_scholarship_id ON eligibility_evaluations(scholarship_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_verdict ON eligibility_evaluations(verdict);
CREATE INDEX IF NOT EXISTS idx_evaluations_evaluated_at ON eligibility_evaluations(evaluated_at);
