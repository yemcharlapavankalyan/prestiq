package org.prestiq.scholarship;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Flyway applied V1 migration cleanly and accurately")
    void testFlywayMigration_AppliedSuccessfully() {
        MigrationInfo current = flyway.info().current();

        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(current.getDescription()).containsIgnoringCase("init scholarship schema");
        assertThat(current.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Verify table 'scholarships' and expected columns exist in schema")
    void testScholarshipsTable_ColumnsExist() throws Exception {
        Set<String> columnNames = new HashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "scholarships", null)) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (columnNames.isEmpty()) {
                try (ResultSet rsUpper = metaData.getColumns(null, null, "SCHOLARSHIPS", null)) {
                    while (rsUpper.next()) {
                        columnNames.add(rsUpper.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }

        assertThat(columnNames).contains(
                "id",
                "slug",
                "title",
                "provider_name",
                "description",
                "official_website_url",
                "application_url",
                "source_type",
                "award_amount_min",
                "award_amount_max",
                "currency_code",
                "award_structure",
                "is_renewable",
                "application_start_date",
                "application_deadline",
                "academic_year",
                "eligible_countries",
                "eligible_states",
                "education_levels",
                "eligible_institutions",
                "eligible_fields_of_study",
                "min_gpa",
                "gpa_scale",
                "max_family_income",
                "first_gen_only",
                "veteran_only",
                "disability_only",
                "eligibility_criteria_schema",
                "verification_status",
                "verified_by_user_id",
                "verified_at",
                "rejection_reason",
                "publication_status",
                "published_at",
                "archived_at",
                "archived_reason",
                "source_name",
                "last_verified_at",
                "created_at",
                "updated_at"
        );
    }

    @Test
    @DisplayName("Verify table 'scholarship_sources' and expected columns exist in schema")
    void testScholarshipSourcesTable_ColumnsExist() throws Exception {
        Set<String> columnNames = new HashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "scholarship_sources", null)) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (columnNames.isEmpty()) {
                try (ResultSet rsUpper = metaData.getColumns(null, null, "SCHOLARSHIP_SOURCES", null)) {
                    while (rsUpper.next()) {
                        columnNames.add(rsUpper.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }

        assertThat(columnNames).contains(
                "id",
                "scholarship_id",
                "source_url",
                "source_name",
                "content_hash",
                "last_scraped_at",
                "http_status",
                "created_at"
        );
    }

    @Test
    @DisplayName("Verify database check constraint: chk_award_amount_range rejects min > max")
    void testDatabaseCheckConstraint_AwardAmountRange_Fails() {
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    """
                    INSERT INTO scholarships (
                        slug, title, provider_name, description, official_website_url,
                        source_type, award_amount_min, award_amount_max, application_deadline,
                        created_at, updated_at
                    ) VALUES (
                        'invalid-range', 'Test', 'Provider', 'Desc', 'https://example.com',
                        'GOVERNMENT', 10000.00, 5000.00, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Verify database check constraint: chk_application_dates rejects start date after deadline")
    void testDatabaseCheckConstraint_ApplicationDates_Fails() {
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    """
                    INSERT INTO scholarships (
                        slug, title, provider_name, description, official_website_url,
                        source_type, application_start_date, application_deadline,
                        created_at, updated_at
                    ) VALUES (
                        'invalid-dates', 'Test', 'Provider', 'Desc', 'https://example.com',
                        'GOVERNMENT', '2026-12-01', '2026-11-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                    )
                    """
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
