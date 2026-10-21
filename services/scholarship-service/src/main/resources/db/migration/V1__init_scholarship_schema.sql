-- ==============================================================================
-- Prestiq Scholarship Service: V1 Initial Scholarship Catalog Schema
-- Database: prestiq_scholarship_db
-- ==============================================================================

CREATE TABLE IF NOT EXISTS scholarships (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(200) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    official_website_url VARCHAR(1000) NOT NULL,
    application_url VARCHAR(1000) NULL,
    source_type VARCHAR(50) NOT NULL,
    award_amount_min NUMERIC(12,2) NULL,
    award_amount_max NUMERIC(12,2) NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    award_structure VARCHAR(50) NOT NULL DEFAULT 'ONE_TIME',
    is_renewable BOOLEAN NOT NULL DEFAULT FALSE,
    application_start_date DATE NULL,
    application_deadline DATE NOT NULL,
    academic_year VARCHAR(20) NULL,
    eligible_countries TEXT NOT NULL DEFAULT '[]',
    eligible_states TEXT NOT NULL DEFAULT '[]',
    education_levels TEXT NOT NULL DEFAULT '[]',
    eligible_institutions TEXT NOT NULL DEFAULT '[]',
    eligible_fields_of_study TEXT NOT NULL DEFAULT '[]',
    min_gpa NUMERIC(4,2) NULL,
    gpa_scale NUMERIC(4,2) NOT NULL DEFAULT 4.00,
    max_family_income NUMERIC(14,2) NULL,
    first_gen_only BOOLEAN NOT NULL DEFAULT FALSE,
    veteran_only BOOLEAN NOT NULL DEFAULT FALSE,
    disability_only BOOLEAN NOT NULL DEFAULT FALSE,
    eligibility_criteria_schema TEXT NOT NULL DEFAULT '[]',
    verification_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    verified_by_user_id UUID NULL,
    verified_at TIMESTAMP WITH TIME ZONE NULL,
    rejection_reason TEXT NULL,
    publication_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP WITH TIME ZONE NULL,
    archived_at TIMESTAMP WITH TIME ZONE NULL,
    archived_reason TEXT NULL,
    source_name VARCHAR(255) NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_award_amount CHECK (award_amount_min IS NULL OR award_amount_min >= 0.00),
    CONSTRAINT chk_award_amount_range CHECK (award_amount_min IS NULL OR award_amount_max IS NULL OR award_amount_min <= award_amount_max),
    CONSTRAINT chk_application_dates CHECK (application_start_date IS NULL OR application_start_date <= application_deadline),
    CONSTRAINT chk_scholarship_gpa CHECK (min_gpa IS NULL OR (min_gpa >= 0.00 AND min_gpa <= gpa_scale)),
    CONSTRAINT chk_scholarship_gpa_scale CHECK (gpa_scale > 0.00),
    CONSTRAINT chk_scholarship_max_income CHECK (max_family_income IS NULL OR max_family_income >= 0.00)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_scholarships_slug ON scholarships(slug);
CREATE INDEX IF NOT EXISTS idx_scholarships_verification_status ON scholarships(verification_status);
CREATE INDEX IF NOT EXISTS idx_scholarships_publication_status ON scholarships(publication_status);
CREATE INDEX IF NOT EXISTS idx_scholarships_application_deadline ON scholarships(application_deadline);
CREATE INDEX IF NOT EXISTS idx_scholarships_source_type ON scholarships(source_type);
CREATE INDEX IF NOT EXISTS idx_scholarships_provider_name ON scholarships(provider_name);

CREATE TABLE IF NOT EXISTS scholarship_sources (
    id BIGSERIAL PRIMARY KEY,
    scholarship_id BIGINT NOT NULL REFERENCES scholarships(id) ON DELETE CASCADE,
    source_url VARCHAR(1000) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    last_scraped_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    http_status INT NOT NULL DEFAULT 200,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scholarship_sources_scholarship_id ON scholarship_sources(scholarship_id);
CREATE INDEX IF NOT EXISTS idx_scholarship_sources_content_hash ON scholarship_sources(content_hash);
