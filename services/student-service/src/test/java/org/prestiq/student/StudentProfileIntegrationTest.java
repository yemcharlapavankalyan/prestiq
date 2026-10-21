package org.prestiq.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.student.domain.entity.EducationLevel;
import org.prestiq.student.dto.request.StudentProfileRequest;
import org.prestiq.student.repository.StudentProfileRepository;
import org.prestiq.student.util.TestJwtHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StudentProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    private StudentProfileRequest buildValidProfileRequest() {
        return new StudentProfileRequest(
                "Ada",
                "Lovelace",
                LocalDate.of(2003, 5, 15),
                "US",
                "California",
                EducationLevel.UNDERGRADUATE,
                "University of California, Berkeley",
                "Computer Science",
                "Junior",
                new BigDecimal("3.85"),
                new BigDecimal("4.00"),
                new BigDecimal("55000.00"),
                "USD",
                true,
                false,
                false,
                Map.of("researchInterests", "Quantum Computing", "honorsStudent", true)
        );
    }

    @Test
    @DisplayName("1. POST /api/students/me - Successfully create a student profile")
    void testCreateProfile_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "ada@berkeley.edu", List.of("ROLE_STUDENT"));
        StudentProfileRequest request = buildValidProfileRequest();

        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.educationLevel").value("UNDERGRADUATE"))
                .andExpect(jsonPath("$.cumulativeGpa").value(3.85))
                .andExpect(jsonPath("$.gpaScale").value(4.00))
                .andExpect(jsonPath("$.citizenshipCountry").value("US"))
                .andExpect(jsonPath("$.firstGenStudent").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. GET /api/students/me - Retrieve authenticated student's profile")
    void testGetAuthenticatedProfile_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "ada@berkeley.edu", List.of("ROLE_STUDENT"));
        StudentProfileRequest request = buildValidProfileRequest();

        // Create profile first
        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Retrieve own profile
        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.majorFieldOfStudy").value("Computer Science"));
    }

    @Test
    @DisplayName("3. GET /api/students/me - Return 404 when profile does not exist yet")
    void testGetAuthenticatedProfile_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "newbie@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Profile Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("4. PUT /api/students/me - Update existing profile")
    void testUpdateProfile_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "ada@berkeley.edu", List.of("ROLE_STUDENT"));

        // Initial creation
        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfileRequest())))
                .andExpect(status().isCreated());

        // Update request with higher GPA and new year
        StudentProfileRequest updateRequest = new StudentProfileRequest(
                "Ada",
                "Lovelace-Byron",
                LocalDate.of(2003, 5, 15),
                "US",
                "California",
                EducationLevel.POSTGRADUATE,
                "University of California, Berkeley",
                "Artificial Intelligence",
                "Master Year 1",
                new BigDecimal("3.98"),
                new BigDecimal("4.00"),
                new BigDecimal("60000.00"),
                "USD",
                true,
                false,
                false,
                Map.of("specialization", "Machine Learning")
        );

        mockMvc.perform(put("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Lovelace-Byron"))
                .andExpect(jsonPath("$.educationLevel").value("POSTGRADUATE"))
                .andExpect(jsonPath("$.cumulativeGpa").value(3.98))
                .andExpect(jsonPath("$.currentYearOrSemester").value("Master Year 1"))
                .andExpect(jsonPath("$.majorFieldOfStudy").value("Artificial Intelligence"));
    }

    @Test
    @DisplayName("5. PUT /api/students/me - Upsert profile when it does not yet exist")
    void testUpsertProfile_CreatesWhenNotExists() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "fresh@berkeley.edu", List.of("ROLE_STUDENT"));
        StudentProfileRequest request = buildValidProfileRequest();

        mockMvc.perform(put("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }

    @Test
    @DisplayName("6. POST /api/students/me - Duplicate profile creation returns 409 Conflict")
    void testDuplicateProfileCreation_Returns409Conflict() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "ada@berkeley.edu", List.of("ROLE_STUDENT"));
        StudentProfileRequest request = buildValidProfileRequest();

        // First creation succeeds
        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation attempt for same user fails with 409 Conflict
        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Profile Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("7. Validation - Reject invalid input: Blank first/last name")
    void testInvalidInput_BlankNames_Returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "student@test.edu", List.of("ROLE_STUDENT"));

        StudentProfileRequest invalidRequest = new StudentProfileRequest(
                "",
                "   ",
                LocalDate.of(2004, 1, 1),
                "US",
                "New York",
                EducationLevel.UNDERGRADUATE,
                "NYU",
                "Economics",
                "Sophomore",
                new BigDecimal("3.50"),
                new BigDecimal("4.00"),
                null,
                "USD",
                false,
                false,
                false,
                null
        );

        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[*].field", hasItems("firstName", "lastName")));
    }

    @Test
    @DisplayName("8. Validation - Reject invalid academic data: Cumulative GPA exceeding GPA scale")
    void testInvalidInput_GpaExceedsScale_Returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "student@test.edu", List.of("ROLE_STUDENT"));

        // GPA 4.50 on 4.00 scale is impossible
        StudentProfileRequest invalidRequest = new StudentProfileRequest(
                "Grace",
                "Hopper",
                LocalDate.of(2003, 12, 9),
                "US",
                "Virginia",
                EducationLevel.UNDERGRADUATE,
                "Yale",
                "Computer Science",
                "Senior",
                new BigDecimal("4.50"),
                new BigDecimal("4.00"),
                null,
                "USD",
                false,
                false,
                false,
                null
        );

        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Academic Data"))
                .andExpect(jsonPath("$.detail", containsString("cannot exceed the grading scale")));
    }

    @Test
    @DisplayName("9. Validation - Reject invalid input: Negative annual income")
    void testInvalidInput_NegativeIncome_Returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "student@test.edu", List.of("ROLE_STUDENT"));

        StudentProfileRequest invalidRequest = new StudentProfileRequest(
                "Grace",
                "Hopper",
                LocalDate.of(2003, 12, 9),
                "US",
                "Virginia",
                EducationLevel.UNDERGRADUATE,
                "Yale",
                "Computer Science",
                "Senior",
                new BigDecimal("3.90"),
                new BigDecimal("4.00"),
                new BigDecimal("-5000.00"),
                "USD",
                false,
                false,
                false,
                null
        );

        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("10. Validation - Reject invalid input: Invalid country code (not 2-letter ISO)")
    void testInvalidInput_InvalidCountryCode_Returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.generateToken(userId, "student@test.edu", List.of("ROLE_STUDENT"));

        StudentProfileRequest invalidRequest = new StudentProfileRequest(
                "Grace",
                "Hopper",
                LocalDate.of(2003, 12, 9),
                "USA", // Invalid: 3 characters instead of 2
                "Virginia",
                EducationLevel.UNDERGRADUATE,
                "Yale",
                "Computer Science",
                "Senior",
                new BigDecimal("3.90"),
                new BigDecimal("4.00"),
                new BigDecimal("40000.00"),
                "USD",
                false,
                false,
                false,
                null
        );

        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("11. Security - Missing token returns 401 Unauthorized")
    void testUnauthorizedAccess_MissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("12. Security - Expired token returns 401 Unauthorized")
    void testUnauthorizedAccess_ExpiredToken_Returns401() throws Exception {
        String expiredToken = TestJwtHelper.generateExpiredToken(UUID.randomUUID(), "expired@test.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("13. Security - Attempted cross-user access: Student A cannot access Student B's profile (403 Forbidden)")
    void testAttemptedCrossUserAccess_StudentForbidden_Returns403() throws Exception {
        // Create profile for Student B
        UUID studentBUserId = UUID.randomUUID();
        String studentBToken = TestJwtHelper.generateToken(studentBUserId, "studentB@university.edu", List.of("ROLE_STUDENT"));
        String createResponse = mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfileRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(createResponse).get("id").asText();

        // Student A attempts to access Student B's profile by ID
        UUID studentAUserId = UUID.randomUUID();
        String studentAToken = TestJwtHelper.generateToken(studentAUserId, "studentA@university.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/students/" + profileId)
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail", containsString("permission to view another student's profile")));
    }

    @Test
    @DisplayName("14. Security - Admin user can access any student profile by ID")
    void testAdminAccess_CanViewAnyStudentProfile_Success() throws Exception {
        // Create profile for Student B
        UUID studentBUserId = UUID.randomUUID();
        String studentBToken = TestJwtHelper.generateToken(studentBUserId, "studentB@university.edu", List.of("ROLE_STUDENT"));
        String createResponse = mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfileRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String profileId = objectMapper.readTree(createResponse).get("id").asText();

        // Admin accesses Student B's profile
        UUID adminUserId = UUID.randomUUID();
        String adminToken = TestJwtHelper.generateToken(adminUserId, "admin@prestiq.org", List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/students/" + profileId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.userId").value(studentBUserId.toString()));
    }

    @Test
    @DisplayName("15. Security - Untrusted client sending X-User-Id without credentials is rejected")
    void testHeaderSpoofingRejected_Returns401() throws Exception {
        UUID spoofedUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/students/me")
                        .header("X-User-Id", spoofedUserId.toString())
                        .header("X-User-Roles", "ROLE_STUDENT"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("16. Inter-Service - Trusted internal service request with internal key can access profile")
    void testInternalEndpoint_AuthorizedWithInternalKey() throws Exception {
        UUID studentUserId = UUID.randomUUID();
        String studentToken = TestJwtHelper.generateToken(studentUserId, "student@uni.edu", List.of("ROLE_STUDENT"));

        // Create profile
        mockMvc.perform(post("/api/students/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfileRequest())))
                .andExpect(status().isCreated());

        // Internal service queries profile via /internal/v1/students/{userId}/profile
        mockMvc.perform(get("/internal/v1/students/" + studentUserId + "/profile")
                        .header("X-Internal-Service-Key", "prestiq-internal-dev-secret-key-2026")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ROLE_INTERNAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(studentUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }
}
