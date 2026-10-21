package org.prestiq.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.recommendation.client.EligibilityServiceClient;
import org.prestiq.recommendation.client.ScholarshipServiceClient;
import org.prestiq.recommendation.client.StudentServiceClient;
import org.prestiq.recommendation.config.JwtProperties;
import org.prestiq.recommendation.domain.entity.EligibilityVerdict;
import org.prestiq.recommendation.domain.entity.StudentRecommendation;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;
import org.prestiq.recommendation.dto.request.UpdatePreferencesRequest;
import org.prestiq.recommendation.exception.DownstreamServiceException;
import org.prestiq.recommendation.exception.StudentProfileNotFoundException;
import org.prestiq.recommendation.repository.StudentPreferenceRepository;
import org.prestiq.recommendation.repository.StudentRecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Recommendation Service Comprehensive Integration Tests")
class RecommendationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StudentRecommendationRepository recommendationRepository;

    @Autowired
    private StudentPreferenceRepository preferenceRepository;

    @MockBean
    private StudentServiceClient studentServiceClient;

    @MockBean
    private ScholarshipServiceClient scholarshipServiceClient;

    @MockBean
    private EligibilityServiceClient eligibilityServiceClient;

    private SecretKey signingKey;
    private final UUID studentAId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID studentBId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID adminId = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private StudentProfileClientDto studentAProfile;
    private List<ScholarshipSummaryClientDto> activeScholarships;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        recommendationRepository.deleteAll();
        preferenceRepository.deleteAll();

        studentAProfile = new StudentProfileClientDto(
                UUID.randomUUID(), studentAId, "Alice", "Johnson", "Alice Johnson",
                LocalDate.of(2003, 4, 15), "US", "CA", "UNDERGRADUATE",
                "UC Berkeley", "Computer Science", "Junior",
                new BigDecimal("3.80"), new BigDecimal("4.00"), new BigDecimal("60000"),
                "USD", false, false, false, Map.of()
        );

        LocalDate now = LocalDate.now();

        ScholarshipSummaryClientDto schol1 = new ScholarshipSummaryClientDto(
                101L, "stem-fellowship", "National STEM Fellowship", "STEM Org", "GOVERNMENT",
                new BigDecimal("10000"), new BigDecimal("10000"), "USD", "ONE_TIME",
                false, now.minusDays(10), now.plusDays(20), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("3.50"),
                "VERIFIED", "PUBLISHED"
        );

        ScholarshipSummaryClientDto schol2 = new ScholarshipSummaryClientDto(
                102L, "tech-leaders-grant", "Tech Leaders Grant", "Tech Trust", "FOUNDATION",
                new BigDecimal("5000"), new BigDecimal("5000"), "USD", "ONE_TIME",
                false, now.minusDays(5), now.plusDays(45), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("3.00"),
                "VERIFIED", "PUBLISHED"
        );

        ScholarshipSummaryClientDto schol3 = new ScholarshipSummaryClientDto(
                103L, "humanities-award", "Global Humanities Award", "Humanities Council", "FOUNDATION",
                new BigDecimal("20000"), new BigDecimal("20000"), "USD", "ONE_TIME",
                false, now.minusDays(5), now.plusDays(10), false,
                List.of("US"), List.of("POSTGRADUATE"), new BigDecimal("3.80"),
                "VERIFIED", "PUBLISHED"
        );

        activeScholarships = List.of(schol1, schol2, schol3);
    }

    private String generateToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("1. Authenticated personalized recommendations with strict ineligibility gate")
    void testGetRecommendationsWithIneligibilityGate() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        // Schol 1: ELIGIBLE, Schol 2: NEEDS_VERIFICATION, Schol 3: INELIGIBLE
        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false),
                102L, new BatchEvaluateItemDto(102L, 1, "NEEDS_VERIFICATION", new BigDecimal("75.00"), 3, 4, true),
                103L, new BatchEvaluateItemDto(103L, 1, "INELIGIBLE", new BigDecimal("25.00"), 1, 4, false)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                // Schol 3 (INELIGIBLE) must NEVER be recommended
                .andExpect(jsonPath("$.content[?(@.scholarshipId == 103)]").doesNotExist())
                // Verify Schol 1 ranks top
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)))
                .andExpect(jsonPath("$.content[0].rankPosition", is(1)))
                .andExpect(jsonPath("$.content[0].eligibilityVerdict", is("ELIGIBLE")))
                .andExpect(jsonPath("$.content[0].eligibilityScore", is(100.0)))
                .andExpect(jsonPath("$.content[0].scoreBreakdown.weights.eligibility", is(0.5)))
                // Verify Schol 2 is next
                .andExpect(jsonPath("$.content[1].scholarshipId", is(102)))
                .andExpect(jsonPath("$.content[1].rankPosition", is(2)))
                .andExpect(jsonPath("$.content[1].eligibilityVerdict", is("NEEDS_VERIFICATION")));

        // Verify DB persistence
        List<StudentRecommendation> persisted = recommendationRepository.findByStudentId(studentAId);
        assertEquals(2, persisted.size());
    }

    @Test
    @DisplayName("2. Missing eligibility evaluation is strictly treated as ineligible and not recommended")
    void testMissingEvaluationExclusion() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        // Only evaluate Schol 101. Schol 102 and 103 have missing evaluations
        Map<Long, BatchEvaluateItemDto> partialEvalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("90.00"), 3, 3, false)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(partialEvalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)));
    }

    @Test
    @DisplayName("3. Filtering by includeNeedsVerification=false restricts to ELIGIBLE only")
    void testFilterIncludeNeedsVerificationFalse() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false),
                102L, new BatchEvaluateItemDto(102L, 1, "NEEDS_VERIFICATION", new BigDecimal("75.00"), 3, 4, true)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations?includeNeedsVerification=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)))
                .andExpect(jsonPath("$.content[0].eligibilityVerdict", is("ELIGIBLE")));
    }

    @Test
    @DisplayName("4. Filtering by minScore excludes lower scoring recommendations")
    void testFilterMinScore() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false),
                102L, new BatchEvaluateItemDto(102L, 1, "NEEDS_VERIFICATION", new BigDecimal("40.00"), 2, 4, true)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        // minScore 85.00 should only return Schol 101
        mockMvc.perform(get("/api/v1/recommendations?minScore=85.00")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)));
    }

    @Test
    @DisplayName("5. Dismiss and undismiss recommendation lifecycle")
    void testDismissAndUndismissLifecycle() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false),
                102L, new BatchEvaluateItemDto(102L, 1, "ELIGIBLE", new BigDecimal("90.00"), 4, 4, false)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        // Initial fetch: 2 recommendations
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        // Dismiss scholarship 101
        mockMvc.perform(patch("/api/v1/recommendations/101/dismiss")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Now active list contains only scholarship 102
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(102)));

        // Undismiss scholarship 101
        mockMvc.perform(patch("/api/v1/recommendations/101/undismiss")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Restored: 2 recommendations again
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("6. Stable pagination and ordering")
    void testStablePagination() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false),
                102L, new BatchEvaluateItemDto(102L, 1, "ELIGIBLE", new BigDecimal("85.00"), 4, 4, false)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        // Page 0, Size 1 -> gets first item
        mockMvc.perform(get("/api/v1/recommendations?page=0&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)))
                .andExpect(jsonPath("$.isLast", is(false)));

        // Page 1, Size 1 -> gets second item
        mockMvc.perform(get("/api/v1/recommendations?page=1&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(102)))
                .andExpect(jsonPath("$.isLast", is(true)));
    }

    @Test
    @DisplayName("7. Missing student profile returns 404 ProblemDetail")
    void testMissingStudentProfileError() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId))
                .thenThrow(new StudentProfileNotFoundException("Student profile not found for user ID: " + studentAId));

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Student Profile Not Found")))
                .andExpect(jsonPath("$.type", is("https://api.prestiq.org/errors/student-profile-not-found")));
    }

    @Test
    @DisplayName("8. Downstream service unavailable returns 503 ProblemDetail")
    void testDownstreamServiceUnavailable() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);
        when(eligibilityServiceClient.batchEvaluate(any(), any()))
                .thenThrow(new DownstreamServiceException("eligibility-service", 503, "Eligibility Service connection timed out"));

        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title", is("Downstream Service Error")))
                .andExpect(jsonPath("$.type", is("https://api.prestiq.org/errors/downstream-service-error")))
                .andExpect(jsonPath("$.downstreamService", is("eligibility-service")));
    }

    @Test
    @DisplayName("9. Cross-user isolation: Student A cannot access Student B's recommendations")
    void testCrossUserIsolation() throws Exception {
        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        mockMvc.perform(get("/api/v1/recommendations?studentId=" + studentBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")));
    }

    @Test
    @DisplayName("10. Admin can access any student's recommendations")
    void testAdminCanAccessAnyStudent() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);

        Map<Long, BatchEvaluateItemDto> evalMap = Map.of(
                101L, new BatchEvaluateItemDto(101L, 1, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false)
        );
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(evalMap);

        String adminToken = generateToken(adminId, "admin@prestiq.org", List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/v1/recommendations?studentId=" + studentAId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(101)));
    }

    @Test
    @DisplayName("11. Unauthenticated requests are rejected with 401 ProblemDetail")
    void testUnauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")));
    }

    @Test
    @DisplayName("12. Arbitrary X-User-Id header is rejected without internal service key")
    void testArbitraryHeadersRejected() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("X-User-Id", studentAId.toString())
                        .header("X-User-Roles", "ROLE_STUDENT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("13. Internal endpoint accessible with valid X-Internal-Service-Key")
    void testInternalEndpointWithInternalKey() throws Exception {
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(Map.of());

        mockMvc.perform(post("/internal/v1/recommendations/recalculate/" + studentAId)
                        .header("X-Internal-Service-Key", jwtProperties.getInternalServiceKey()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("14. Internal endpoint rejected without internal key")
    void testInternalEndpointWithoutKeyForbidden() throws Exception {
        mockMvc.perform(post("/internal/v1/recommendations/recalculate/" + studentAId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("15. Student preference configuration CRUD and auto-recalculation")
    void testStudentPreferencesCrud() throws Exception {
        String token = generateToken(studentAId, "alice@berkeley.edu", List.of("ROLE_STUDENT"));

        // GET preferences when none exist returns default empty
        mockMvc.perform(get("/api/v1/recommendations/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", is(studentAId.toString())))
                .andExpect(jsonPath("$.preferredFields", hasSize(0)));

        // Mock dependencies for auto-recalculation on preference update
        when(studentServiceClient.fetchStudentProfile(studentAId)).thenReturn(studentAProfile);
        when(scholarshipServiceClient.fetchActivePublishedScholarships(anyInt())).thenReturn(activeScholarships);
        when(eligibilityServiceClient.batchEvaluate(any(), any())).thenReturn(Map.of());

        // PUT preferences
        UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                List.of("Computer Science", "Artificial Intelligence"),
                List.of("US-CA"),
                new BigDecimal("5000.00")
        );

        mockMvc.perform(put("/api/v1/recommendations/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", is(studentAId.toString())))
                .andExpect(jsonPath("$.preferredFields", containsInAnyOrder("Computer Science", "Artificial Intelligence")))
                .andExpect(jsonPath("$.minAwardPreference", is(5000.0)));
    }
}
