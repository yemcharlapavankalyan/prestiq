package org.prestiq.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.application.client.NotificationServiceClient;
import org.prestiq.application.client.ScholarshipServiceClient;
import org.prestiq.application.config.JwtProperties;
import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.domain.entity.ScholarshipApplication;
import org.prestiq.application.dto.client.ScholarshipClientDto;
import org.prestiq.application.dto.request.CreateApplicationRequest;
import org.prestiq.application.dto.request.SaveScholarshipRequest;
import org.prestiq.application.dto.request.UpdateApplicationRequest;
import org.prestiq.application.dto.request.UpdateStatusRequest;
import org.prestiq.application.repository.ApplicationStatusHistoryRepository;
import org.prestiq.application.repository.SavedScholarshipRepository;
import org.prestiq.application.repository.ScholarshipApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Application Service Comprehensive Integration Tests")
class ApplicationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ScholarshipApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SavedScholarshipRepository savedScholarshipRepository;

    @MockBean
    private ScholarshipServiceClient scholarshipServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    private SecretKey signingKey;
    private final UUID studentAId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID studentBId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID adminId = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        statusHistoryRepository.deleteAll();
        applicationRepository.deleteAll();
        savedScholarshipRepository.deleteAll();
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
    @DisplayName("1. Application creation captures scholarship snapshot and records initial status history")
    void testCreateApplicationSuccess() throws Exception {
        LocalDate deadline = LocalDate.now().plusDays(25);
        ScholarshipClientDto mockScholarship = new ScholarshipClientDto(
                101L, "stem-fellowship", "National STEM Fellowship", "STEM Foundation",
                new BigDecimal("10000"), new BigDecimal("10000"), "USD",
                deadline, false, "VERIFIED", "PUBLISHED"
        );
        when(scholarshipServiceClient.fetchScholarshipById(101L)).thenReturn(Optional.of(mockScholarship));

        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));

        CreateApplicationRequest request = new CreateApplicationRequest(
                101L, null, null, null, "https://portal.stem.org/app/101", "Brainstormed essay topics"
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.studentId", is(studentAId.toString())))
                .andExpect(jsonPath("$.scholarshipId", is(101)))
                .andExpect(jsonPath("$.scholarshipTitle", is("National STEM Fellowship")))
                .andExpect(jsonPath("$.scholarshipProvider", is("STEM Foundation")))
                .andExpect(jsonPath("$.currentStatus", is("PLANNING")))
                .andExpect(jsonPath("$.submissionDeadline", is(deadline.toString())))
                .andExpect(jsonPath("$.externalPortalReference", is("https://portal.stem.org/app/101")))
                .andExpect(jsonPath("$.progressPercentage", is(0)))
                .andExpect(jsonPath("$.notes", is("Brainstormed essay topics")));

        // Verify database persistence
        List<ScholarshipApplication> list = applicationRepository.findAll();
        assertEquals(1, list.size());
        assertEquals("PLANNING", list.get(0).getCurrentStatus().name());

        // Verify status history audit
        assertEquals(1, statusHistoryRepository.count());
    }

    @Test
    @DisplayName("2. Duplicate application rule rejects concurrent active application with 409 Conflict")
    void testCreateApplicationDuplicateActiveConflict() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                101L, "Tech Grant", "Tech Trust", LocalDate.now().plusDays(30), null, null
        );

        // First application succeeds
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second application for same scholarship while first is active fails with 409 Conflict
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Duplicate Application Conflict")))
                .andExpect(jsonPath("$.type", is("https://api.prestiq.org/errors/duplicate-application")));
    }

    @Test
    @DisplayName("3. Duplicate application rule allows new application after earlier application was WITHDRAWN")
    void testCreateApplicationAfterWithdrawnAllowed() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                101L, "Tech Grant", "Tech Trust", LocalDate.now().plusDays(30), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Withdraw the first application
        mockMvc.perform(post("/api/v1/applications/" + idStr + "/withdraw")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("WITHDRAWN")));

        // Creating a new application for the same scholarship now succeeds!
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStatus", is("PLANNING")));
    }

    @Test
    @DisplayName("4. Duplicate application rule allows new application after earlier application was REJECTED")
    void testCreateApplicationAfterRejectedAllowed() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                102L, "Research Fellowship", "Research Council", LocalDate.now().plusDays(15), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Transition through lifecycle: PLANNING -> IN_PROGRESS -> READY_TO_SUBMIT -> SUBMITTED -> REJECTED
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.IN_PROGRESS, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.READY_TO_SUBMIT, null, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.SUBMITTED, null, LocalDate.now(), null))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.REJECTED, "Not selected this cycle", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("REJECTED")));

        // New application for next cycle is allowed!
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("5. Student ownership and cross-user isolation: Student B cannot access Student A's application")
    void testCrossUserAccessDenied() throws Exception {
        String tokenA = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        String tokenB = generateToken(studentBId, "student.b@prestiq.org", List.of("ROLE_STUDENT"));

        CreateApplicationRequest request = new CreateApplicationRequest(
                105L, "Community Grant", "Community Org", LocalDate.now().plusDays(20), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Student A can read it
        mockMvc.perform(get("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Student B attempting to read Student A's application gets 403 Forbidden
        mockMvc.perform(get("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")));

        // Student B attempting to update Student A's application gets 403 Forbidden
        mockMvc.perform(patch("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationRequest(50, "Hacked notes", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Admin role can access any student's application")
    void testAdminCanAccessAnyApplication() throws Exception {
        String tokenA = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        String adminToken = generateToken(adminId, "admin@prestiq.org", List.of("ROLE_ADMIN"));

        CreateApplicationRequest request = new CreateApplicationRequest(
                106L, "Global Grant", "Global Trust", LocalDate.now().plusDays(20), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Admin can access
        mockMvc.perform(get("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idStr)));
    }

    @Test
    @DisplayName("7. Progress and notes update with validation limits")
    void testUpdateApplicationProgressAndValidation() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                107L, "Art Grant", "Art Trust", LocalDate.now().plusDays(40), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Valid progress update
        mockMvc.perform(patch("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationRequest(65, "Essay drafted and reviewed", "REF-9988", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage", is(65)))
                .andExpect(jsonPath("$.notes", is("Essay drafted and reviewed")))
                .andExpect(jsonPath("$.externalPortalReference", is("REF-9988")));

        // Invalid progress (> 100) rejected with 400 Bad Request
        mockMvc.perform(patch("/api/v1/applications/" + idStr)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationRequest(150, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Failed")));
    }

    @Test
    @DisplayName("8. Complete lifecycle state transitions and audit history trail")
    void testLifecycleTransitionsAndHistory() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                108L, "Science Prize", "Science Society", LocalDate.now().plusDays(30), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // 1. PLANNING -> IN_PROGRESS
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.IN_PROGRESS, "Started essays", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("IN_PROGRESS")));

        // 2. IN_PROGRESS -> READY_TO_SUBMIT
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.READY_TO_SUBMIT, "All docs ready", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("READY_TO_SUBMIT")));

        // 3. READY_TO_SUBMIT -> SUBMITTED (explicit action, sets submittedAt and actualSubmissionDate)
        LocalDate submitDate = LocalDate.now();
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.SUBMITTED, "Submitted on portal", submitDate, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("SUBMITTED")))
                .andExpect(jsonPath("$.actualSubmissionDate", is(submitDate.toString())))
                .andExpect(jsonPath("$.submittedAt", notNullValue()))
                .andExpect(jsonPath("$.progressPercentage", is(100)));

        // 4. SUBMITTED -> UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.UNDER_REVIEW, "Provider reviewing", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("UNDER_REVIEW")));

        // 5. UNDER_REVIEW -> AWARDED
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.AWARDED, "Won $7,500!", null, new BigDecimal("7500.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus", is("AWARDED")))
                .andExpect(jsonPath("$.awardAmountReceived", is(7500.0)));

        // Verify status history endpoint
        mockMvc.perform(get("/api/v1/applications/" + idStr + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6))) // Initial PLANNING + 5 transitions
                .andExpect(jsonPath("$[0].toStatus", is("PLANNING")))
                .andExpect(jsonPath("$[1].fromStatus", is("PLANNING")))
                .andExpect(jsonPath("$[1].toStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$[5].toStatus", is("AWARDED")));
    }

    @Test
    @DisplayName("9. Invalid status transitions rejected with 409 Conflict")
    void testInvalidStatusTransitionThrows409Conflict() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        CreateApplicationRequest request = new CreateApplicationRequest(
                109L, "Math Award", "Math Foundation", LocalDate.now().plusDays(30), null, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Attempting direct jump PLANNING -> AWARDED is illegal
        mockMvc.perform(patch("/api/v1/applications/" + idStr + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(ApplicationStatus.AWARDED, "Illegal jump", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Invalid Status Transition")))
                .andExpect(jsonPath("$.type", is("https://api.prestiq.org/errors/invalid-status-transition")))
                .andExpect(jsonPath("$.fromStatus", is("PLANNING")))
                .andExpect(jsonPath("$.toStatus", is("AWARDED")));
    }

    @Test
    @DisplayName("10. Upcoming active deadlines query returns upcoming applications sorted by deadline ASC")
    void testUpcomingDeadlinesQuery() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        LocalDate today = LocalDate.now();

        // Create app 1: deadline in 5 days
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                110L, "Imminent Schol", "Org 1", today.plusDays(5), null, null
                        ))))
                .andExpect(status().isCreated());

        // Create app 2: deadline in 20 days
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                111L, "Approaching Schol", "Org 2", today.plusDays(20), null, null
                        ))))
                .andExpect(status().isCreated());

        // Create app 3: deadline in 60 days
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                112L, "Distant Schol", "Org 3", today.plusDays(60), null, null
                        ))))
                .andExpect(status().isCreated());

        // Query deadlines within 30 days -> should return app 1 and app 2 only, ordered by deadline ascending
        mockMvc.perform(get("/api/v1/applications/deadlines?daysAhead=30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].scholarshipId", is(110)))
                .andExpect(jsonPath("$[1].scholarshipId", is(111)));
    }

    @Test
    @DisplayName("11. Pagination and filtering on applications list")
    void testPaginationAndFiltering() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                120L, "Schol A", "Org A", today.plusDays(10), null, null
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                121L, "Schol B", "Org B", today.plusDays(20), null, null
                        ))))
                .andExpect(status().isCreated());

        // Page 0, Size 1
        mockMvc.perform(get("/api/v1/applications?page=0&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(1)))
                .andExpect(jsonPath("$.isFirst", is(true)))
                .andExpect(jsonPath("$.isLast", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        // Page 1, Size 1
        mockMvc.perform(get("/api/v1/applications?page=1&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFirst", is(false)))
                .andExpect(jsonPath("$.isLast", is(true)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        // Filter by status PLANNING
        mockMvc.perform(get("/api/v1/applications?status=PLANNING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        // Filter by status AWARDED (0 records)
        mockMvc.perform(get("/api/v1/applications?status=AWARDED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("12. Saved scholarships bookmark lifecycle: save, list, and remove")
    void testSavedScholarshipsLifecycle() throws Exception {
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));

        ScholarshipClientDto mockDto = new ScholarshipClientDto(
                201L, "ai-fellowship", "AI Fellowship", "AI Institute",
                new BigDecimal("8000"), new BigDecimal("8000"), "USD",
                LocalDate.now().plusDays(40), false, "VERIFIED", "PUBLISHED"
        );
        when(scholarshipServiceClient.fetchScholarshipById(201L)).thenReturn(Optional.of(mockDto));

        // Save scholarship
        SaveScholarshipRequest saveReq = new SaveScholarshipRequest("Consider applying next week", null, null, null, null);
        mockMvc.perform(post("/api/v1/saved-scholarships/201")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scholarshipId", is(201)))
                .andExpect(jsonPath("$.scholarshipTitle", is("AI Fellowship")))
                .andExpect(jsonPath("$.notes", is("Consider applying next week")));

        // List saved scholarships
        mockMvc.perform(get("/api/v1/saved-scholarships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].scholarshipId", is(201)));

        // Remove saved scholarship
        mockMvc.perform(delete("/api/v1/saved-scholarships/201")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify empty
        mockMvc.perform(get("/api/v1/saved-scholarships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("13. Unauthenticated requests are rejected with 401 ProblemDetail")
    void testUnauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")));
    }

    @Test
    @DisplayName("14. Arbitrary X-User-Id header rejected without internal key")
    void testArbitraryHeadersRejected() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .header("X-User-Id", studentAId.toString())
                        .header("X-User-Roles", "ROLE_STUDENT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("15. Protected internal endpoints allow access with verified X-Internal-Service-Key")
    void testInternalEndpointsProtected() throws Exception {
        // Create an application first
        String token = generateToken(studentAId, "student.a@prestiq.org", List.of("ROLE_STUDENT"));
        MvcResult res = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                301L, "Internal Test Schol", "Internal Org", LocalDate.now().plusDays(10), null, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // Access internal endpoint with valid key -> 200 OK
        mockMvc.perform(get("/internal/v1/applications/" + idStr)
                        .header("X-Internal-Service-Key", jwtProperties.getInternalServiceKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idStr)));

        // Access internal endpoint without key -> 401 Unauthorized
        mockMvc.perform(get("/internal/v1/applications/" + idStr))
                .andExpect(status().isUnauthorized());
    }
}
