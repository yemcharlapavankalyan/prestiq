package org.prestiq.scholarship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.scholarship.domain.entity.AwardStructure;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.dto.request.ScholarshipArchiveRequest;
import org.prestiq.scholarship.dto.request.ScholarshipCreateRequest;
import org.prestiq.scholarship.dto.request.ScholarshipRejectRequest;
import org.prestiq.scholarship.dto.request.ScholarshipSourceRequest;
import org.prestiq.scholarship.dto.request.ScholarshipUpdateRequest;
import org.prestiq.scholarship.repository.ScholarshipRepository;
import org.prestiq.scholarship.repository.ScholarshipSourceRepository;
import org.prestiq.scholarship.util.TestJwtHelper;
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
public class ScholarshipLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScholarshipRepository scholarshipRepository;

    @Autowired
    private ScholarshipSourceRepository sourceRepository;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        sourceRepository.deleteAll();
        scholarshipRepository.deleteAll();

        adminToken = TestJwtHelper.generateToken(UUID.randomUUID(), "admin@prestiq.org", List.of("ROLE_ADMIN"));
        studentToken = TestJwtHelper.generateToken(UUID.randomUUID(), "student@berkeley.edu", List.of("ROLE_STUDENT"));
    }

    private ScholarshipCreateRequest buildValidCreateRequest(String title, String slug, LocalDate deadline) {
        return new ScholarshipCreateRequest(
                title,
                slug,
                "National Science Foundation",
                "Prestigious merit scholarship for undergraduate STEM students.",
                "https://www.nsf.gov/awards/stem-2026",
                "https://www.nsf.gov/apply/stem-2026",
                SourceType.GOVERNMENT,
                new BigDecimal("5000.00"),
                new BigDecimal("10000.00"),
                "USD",
                AwardStructure.ANNUAL,
                true,
                LocalDate.now().minusDays(30),
                deadline != null ? deadline : LocalDate.now().plusDays(90),
                "2026-2027",
                List.of("US", "CA"),
                List.of("CA", "NY"),
                List.of("UNDERGRADUATE", "POSTGRADUATE"),
                List.of("UC Berkeley", "Stanford"),
                List.of("Computer Science", "Engineering"),
                new BigDecimal("3.50"),
                new BigDecimal("4.00"),
                new BigDecimal("80000.00"),
                false,
                false,
                false,
                List.of(Map.of("code", "MIN_GPA", "threshold", 3.5)),
                "NSF Official Scraper"
        );
    }

    @Test
    @DisplayName("1. POST /api/v1/admin/scholarships - Successfully create a draft scholarship")
    void testScholarshipCreation_Success() throws Exception {
        ScholarshipCreateRequest request = buildValidCreateRequest(
                "National STEM Leaders 2026",
                "national-stem-leaders-2026",
                LocalDate.now().plusMonths(3)
        );

        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("National STEM Leaders 2026"))
                .andExpect(jsonPath("$.slug").value("national-stem-leaders-2026"))
                .andExpect(jsonPath("$.providerName").value("National Science Foundation"))
                .andExpect(jsonPath("$.verificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.publicationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.awardAmountMin").value(5000.00))
                .andExpect(jsonPath("$.awardAmountMax").value(10000.00))
                .andExpect(jsonPath("$.eligibleCountries", hasItems("US", "CA")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. PUT /api/v1/admin/scholarships/{id} - Successfully update scholarship")
    void testScholarshipUpdate_Success() throws Exception {
        // Create draft first
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Original Title", "orig-slug", LocalDate.now().plusMonths(2)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // Update title and award amount
        ScholarshipUpdateRequest updateRequest = new ScholarshipUpdateRequest(
                "Updated STEM Grant 2026",
                "updated-stem-grant-2026",
                "Global STEM Foundation",
                "Updated description.",
                "https://www.nsf.gov/updated",
                null,
                SourceType.PRIVATE_FOUNDATION,
                new BigDecimal("6000.00"),
                new BigDecimal("12000.00"),
                "USD",
                AwardStructure.ANNUAL,
                true,
                null,
                LocalDate.now().plusMonths(4),
                "2026-2027",
                List.of("US"),
                null,
                List.of("UNDERGRADUATE"),
                null,
                null,
                new BigDecimal("3.70"),
                new BigDecimal("4.00"),
                null,
                null,
                null,
                null,
                null,
                "Updated Source"
        );

        mockMvc.perform(put("/api/v1/admin/scholarships/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated STEM Grant 2026"))
                .andExpect(jsonPath("$.slug").value("updated-stem-grant-2026"))
                .andExpect(jsonPath("$.providerName").value("Global STEM Foundation"))
                .andExpect(jsonPath("$.sourceType").value("PRIVATE_FOUNDATION"))
                .andExpect(jsonPath("$.awardAmountMin").value(6000.00))
                .andExpect(jsonPath("$.awardAmountMax").value(12000.00));
    }

    @Test
    @DisplayName("3. Validation - Reject invalid inputs: Blank required fields")
    void testInvalidInputs_BlankTitleAndProvider_Returns400() throws Exception {
        ScholarshipCreateRequest invalidRequest = new ScholarshipCreateRequest(
                "",
                "invalid-slug",
                "   ",
                "",
                "not-a-url",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams").isArray());
    }

    @Test
    @DisplayName("4. Validation - Reject invalid dates: Opening date after deadline")
    void testInvalidInputs_StartDateAfterDeadline_Returns400() throws Exception {
        ScholarshipCreateRequest request = new ScholarshipCreateRequest(
                "Temporal Paradox Scholarship",
                "temporal-paradox",
                "Time Foundation",
                "Description",
                "https://www.example.com",
                null,
                SourceType.PRIVATE_FOUNDATION,
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                "USD",
                AwardStructure.ONE_TIME,
                false,
                LocalDate.now().plusDays(60), // Start date is AFTER deadline
                LocalDate.now().plusDays(30), // Deadline
                "2026",
                null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Scholarship Data"))
                .andExpect(jsonPath("$.detail", containsString("cannot be after deadline")));
    }

    @Test
    @DisplayName("5. Validation - Reject invalid amounts: Min amount exceeds max amount")
    void testInvalidInputs_MinAmountExceedsMaxAmount_Returns400() throws Exception {
        ScholarshipCreateRequest request = new ScholarshipCreateRequest(
                "Inverted Award Scholarship",
                "inverted-award",
                "Math Foundation",
                "Description",
                "https://www.example.com",
                null,
                SourceType.PRIVATE_FOUNDATION,
                new BigDecimal("10000.00"), // Min > Max
                new BigDecimal("2000.00"),
                "USD",
                AwardStructure.ONE_TIME,
                false,
                null,
                LocalDate.now().plusDays(30),
                "2026",
                null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Scholarship Data"))
                .andExpect(jsonPath("$.detail", containsString("cannot exceed maximum award amount")));
    }

    @Test
    @DisplayName("6. Validation - Reject duplicate slug (409 Conflict)")
    void testDuplicateSlug_Returns409() throws Exception {
        ScholarshipCreateRequest req1 = buildValidCreateRequest("Scholarship A", "shared-slug", LocalDate.now().plusDays(30));
        ScholarshipCreateRequest req2 = buildValidCreateRequest("Scholarship B", "shared-slug", LocalDate.now().plusDays(30));

        // First creation succeeds
        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second creation with exact same slug fails with 409 Conflict
        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Slug"));
    }

    @Test
    @DisplayName("7. Lifecycle - Submit for review, verify, and reject transitions")
    void testLifecycle_VerificationAndRejectionTransitions() throws Exception {
        // Create draft
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Review Test", "review-test", LocalDate.now().plusDays(60)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // 1. Submit for review -> PENDING_REVIEW
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/submit-review")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_REVIEW"));

        // 2. Reject with reason -> REJECTED
        ScholarshipRejectRequest rejectRequest = new ScholarshipRejectRequest("Source guidelines unverified or outdated.");
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Source guidelines unverified or outdated."));

        // 3. Verify -> VERIFIED
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    @DisplayName("8. Lifecycle - Cannot publish an unverified scholarship (409 Conflict)")
    void testLifecycle_PublicationRules_CannotPublishUnverifiedScholarship() throws Exception {
        // Create draft (unverified)
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Unverified", "unverified-pub", LocalDate.now().plusDays(40)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // Attempt to publish unverified draft -> fails with 409
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Illegal Lifecycle Transition"))
                .andExpect(jsonPath("$.detail", containsString("must be VERIFIED before publication")));
    }

    @Test
    @DisplayName("9. Lifecycle - Successfully publish and archive a verified scholarship")
    void testLifecycle_PublishAndArchiveVerifiedScholarship() throws Exception {
        // Create draft
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Publish Test", "publish-test", LocalDate.now().plusDays(40)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // Verify first
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        // Publish -> PUBLISHED
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        // Archive -> ARCHIVED
        ScholarshipArchiveRequest archiveReq = new ScholarshipArchiveRequest("Cycle completed for 2026.");
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/archive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty())
                .andExpect(jsonPath("$.archivedReason").value("Cycle completed for 2026."));
    }

    @Test
    @DisplayName("10. Public Visibility - Drafts, pending review, and archived records are NOT visible on public endpoints (404)")
    void testPublicVisibility_DraftsAndArchivedNotVisible_Returns404() throws Exception {
        // Create draft
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Hidden Draft", "hidden-draft", LocalDate.now().plusDays(40)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // Public access to draft by ID -> 404
        mockMvc.perform(get("/api/v1/scholarships/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Scholarship Not Found"));

        // Public access to draft by slug -> 404
        mockMvc.perform(get("/api/v1/scholarships/hidden-draft"))
                .andExpect(status().isNotFound());

        // Verify and publish
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/verify").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/publish").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        // Now public access succeeds!
        mockMvc.perform(get("/api/v1/scholarships/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hidden Draft"));

        // Archive it
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/archive").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        // Public access to archived record -> 404 again!
        mockMvc.perform(get("/api/v1/scholarships/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Public Search - Keyword query and multi-criteria filters")
    void testPublicSearch_KeywordsAndFilters_Success() throws Exception {
        // Create 2 scholarships
        createAndPublishScholarship("California STEM Award", "ca-stem", SourceType.GOVERNMENT, new BigDecimal("5000"), new BigDecimal("10000"), List.of("US"), List.of("UNDERGRADUATE"), LocalDate.now().plusMonths(2));
        createAndPublishScholarship("Oxford Global Humanities Grant", "oxford-humanities", SourceType.UNIVERSITY, new BigDecimal("2000"), new BigDecimal("4000"), List.of("GB"), List.of("POSTGRADUATE"), LocalDate.now().plusMonths(3));

        // 1. Search by keyword "STEM"
        mockMvc.perform(get("/api/v1/scholarships?query=STEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("ca-stem"));

        // 2. Search by education level "POSTGRADUATE"
        mockMvc.perform(get("/api/v1/scholarships?educationLevel=POSTGRADUATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("oxford-humanities"));

        // 3. Search by country "US"
        mockMvc.perform(get("/api/v1/scholarships?country=US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("ca-stem"));

        // 4. Search by min amount 6000 (ca-stem max is 10000, oxford max is 4000)
        mockMvc.perform(get("/api/v1/scholarships?minAmount=6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("ca-stem"));
    }

    @Test
    @DisplayName("12. Pagination & Sorting - Valid sorting through allowlist")
    void testPaginationAndSorting_AllowedFields_Success() throws Exception {
        createAndPublishScholarship("Alpha Award", "alpha-award", SourceType.CORPORATE, new BigDecimal("1000"), new BigDecimal("2000"), List.of("US"), List.of("UNDERGRADUATE"), LocalDate.now().plusDays(20));
        createAndPublishScholarship("Beta Award", "beta-award", SourceType.CORPORATE, new BigDecimal("3000"), new BigDecimal("8000"), List.of("US"), List.of("UNDERGRADUATE"), LocalDate.now().plusDays(50));

        // Sort by deadline,asc
        mockMvc.perform(get("/api/v1/scholarships?sort=deadline,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("alpha-award"))
                .andExpect(jsonPath("$.content[1].slug").value("beta-award"));

        // Sort by amount,desc
        mockMvc.perform(get("/api/v1/scholarships?sort=amount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("beta-award"))
                .andExpect(jsonPath("$.content[1].slug").value("alpha-award"));
    }

    @Test
    @DisplayName("13. Pagination & Sorting - Reject forbidden sort fields (400 Bad Request)")
    void testPaginationAndSorting_ForbiddenSortField_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/scholarships?sort=password,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Sort Field"))
                .andExpect(jsonPath("$.detail", containsString("Allowed sort fields are:")));
    }

    @Test
    @DisplayName("14. Deadline Behavior - Past deadlines excluded by default, included with flag")
    void testDeadlineBehavior_PastDeadlineExcludedByDefault_IncludedWithFlag() throws Exception {
        // 1 active, 1 expired
        createAndPublishScholarship("Active Scholarship", "active-schol", SourceType.GOVERNMENT, new BigDecimal("1000"), new BigDecimal("2000"), List.of("US"), List.of("UNDERGRADUATE"), LocalDate.now().plusDays(30));
        createAndPublishScholarship("Expired Scholarship", "expired-schol", SourceType.GOVERNMENT, new BigDecimal("1000"), new BigDecimal("2000"), List.of("US"), List.of("UNDERGRADUATE"), LocalDate.now().minusDays(10));

        // Default: past deadline excluded
        mockMvc.perform(get("/api/v1/scholarships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("active-schol"));

        // includeExpired=true: returns both
        mockMvc.perform(get("/api/v1/scholarships?includeExpired=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("15. Provenance Sources - Add, retrieve, and delete provenance sources")
    void testSourceProvenance_Management_Success() throws Exception {
        // Create draft scholarship
        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Provenance Test", "prov-test", LocalDate.now().plusDays(30)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        // Add provenance source
        ScholarshipSourceRequest sourceReq = new ScholarshipSourceRequest(
                "https://www.nsf.gov/programs/stem-guidelines.pdf",
                "Official NSF PDF Guidelines",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", // 64 hex
                200
        );

        String sourceJson = mockMvc.perform(post("/api/v1/admin/scholarships/" + id + "/sources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sourceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceName").value("Official NSF PDF Guidelines"))
                .andExpect(jsonPath("$.contentHash").value("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
                .andReturn().getResponse().getContentAsString();

        Long sourceId = objectMapper.readTree(sourceJson).get("id").asLong();

        // List sources
        mockMvc.perform(get("/api/v1/admin/scholarships/" + id + "/sources")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceUrl").value("https://www.nsf.gov/programs/stem-guidelines.pdf"));

        // Delete source
        mockMvc.perform(delete("/api/v1/admin/scholarships/" + id + "/sources/" + sourceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify list is now empty
        mockMvc.perform(get("/api/v1/admin/scholarships/" + id + "/sources")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("16. Security - Unauthenticated admin request returns 401")
    void testSecurity_UnauthorizedAdminRequest_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Sec Test", "sec-test", LocalDate.now().plusDays(30)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("17. Security - Student role accessing admin endpoint returns 403 Forbidden")
    void testSecurity_ForbiddenStudentAccessToAdmin_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Sec Test", "sec-test", LocalDate.now().plusDays(30)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @DisplayName("18. Security - Untrusted client sending X-User-* header without internal key is rejected")
    void testSecurity_HeaderSpoofingRejected_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest("Spoof Test", "spoof-test", LocalDate.now().plusDays(30)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    private void createAndPublishScholarship(
            String title,
            String slug,
            SourceType sourceType,
            BigDecimal minAmt,
            BigDecimal maxAmt,
            List<String> countries,
            List<String> levels,
            LocalDate deadline
    ) throws Exception {
        ScholarshipCreateRequest req = new ScholarshipCreateRequest(
                title,
                slug,
                "University Provider",
                "Description for " + title,
                "https://www.example.edu/" + slug,
                null,
                sourceType,
                minAmt,
                maxAmt,
                "USD",
                AwardStructure.ONE_TIME,
                false,
                LocalDate.now().minusDays(10),
                deadline,
                "2026",
                countries,
                null,
                levels,
                null,
                null,
                new BigDecimal("3.00"),
                new BigDecimal("4.00"),
                null,
                false,
                false,
                false,
                null,
                "Test Ingest"
        );

        String createJson = mockMvc.perform(post("/api/v1/admin/scholarships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createJson).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/verify").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/scholarships/" + id + "/publish").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }
}
