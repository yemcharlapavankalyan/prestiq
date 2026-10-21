-- ==============================================================================
-- Prestiq Student Service: V1 Initial Student Profile Schema
-- Database: prestiq_student_db
-- ==============================================================================

CREATE TABLE IF NOT EXISTS student_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NULL,
    citizenship_country VARCHAR(2) NOT NULL,
    state_province VARCHAR(100) NULL,
    education_level VARCHAR(50) NOT NULL,
    institution_name VARCHAR(255) NULL,
    major_field_of_study VARCHAR(150) NULL,
    current_year_or_semester VARCHAR(50) NULL,
    cumulative_gpa NUMERIC(4,2) NULL,
    gpa_scale NUMERIC(4,2) NOT NULL DEFAULT 4.00,
    annual_family_income NUMERIC(14,2) NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    first_gen_student BOOLEAN NOT NULL DEFAULT FALSE,
    military_veteran BOOLEAN NOT NULL DEFAULT FALSE,
    disability_status BOOLEAN NOT NULL DEFAULT FALSE,
    additional_attributes TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cumulative_gpa CHECK (cumulative_gpa IS NULL OR (cumulative_gpa >= 0.00 AND cumulative_gpa <= gpa_scale)),
    CONSTRAINT chk_gpa_scale CHECK (gpa_scale > 0.00),
    CONSTRAINT chk_annual_income CHECK (annual_family_income IS NULL OR annual_family_income >= 0.00)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_student_profiles_user_id ON student_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_education_level ON student_profiles(education_level);
