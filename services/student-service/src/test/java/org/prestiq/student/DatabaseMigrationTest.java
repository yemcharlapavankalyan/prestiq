package org.prestiq.student;

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
import java.util.UUID;

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
        assertThat(current.getDescription()).containsIgnoringCase("init student schema");
        assertThat(current.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Verify table 'student_profiles' and expected columns exist in schema")
    void testSchemaColumns_ExistInDatabase() throws Exception {
        Set<String> columnNames = new HashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "student_profiles", null)) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (columnNames.isEmpty()) {
                try (ResultSet rsUpper = metaData.getColumns(null, null, "STUDENT_PROFILES", null)) {
                    while (rsUpper.next()) {
                        columnNames.add(rsUpper.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }

        assertThat(columnNames).contains(
                "id",
                "user_id",
                "first_name",
                "last_name",
                "date_of_birth",
                "citizenship_country",
                "state_province",
                "education_level",
                "institution_name",
                "major_field_of_study",
                "current_year_or_semester",
                "cumulative_gpa",
                "gpa_scale",
                "annual_family_income",
                "currency_code",
                "first_gen_student",
                "military_veteran",
                "disability_status",
                "additional_attributes",
                "created_at",
                "updated_at"
        );
    }

    @Test
    @DisplayName("Verify database constraint: Check constraint chk_cumulative_gpa rejects GPA exceeding scale")
    void testDatabaseCheckConstraint_CumulativeGpaExceedingScale_Fails() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Attempt direct SQL insertion violating CHECK (cumulative_gpa <= gpa_scale)
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    """
                    INSERT INTO student_profiles (
                        id, user_id, first_name, last_name, citizenship_country, education_level,
                        cumulative_gpa, gpa_scale, created_at, updated_at
                    ) VALUES (?, ?, 'Marie', 'Curie', 'FR', 'POSTGRADUATE', 5.00, 4.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    profileId, userId
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
