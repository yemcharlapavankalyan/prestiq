package org.prestiq.eligibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.eligibility.config.JwtProperties;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.dto.request.BatchEvaluateRequest;
import org.prestiq.eligibility.dto.request.CreateCriterionRequest;
import org.prestiq.eligibility.dto.request.CreateRuleSetRequest;
import org.prestiq.eligibility.dto.request.EvaluateStudentRequest;
import org.prestiq.eligibility.dto.response.EligibilityEvaluationResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.prestiq.eligibility.repository.EligibilityCriterionRepository;
import org.prestiq.eligibility.repository.EligibilityEvaluationRepository;
import org.prestiq.eligibility.repository.ScholarshipRuleSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Eligibility Service End-to-End Integration Tests")
class EligibilityServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ScholarshipRuleSetRepository ruleSetRepository;

    @Autowired
    private EligibilityCriterionRepository criterionRepository;

    @Autowired
    private EligibilityEvaluationRepository evaluationRepository;

    private SecretKey signingKey;
    private final UUID studentAId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID studentBId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID adminId = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        byte[] keyBytes = jwtProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        evaluationRepository.deleteAll();
        criterionRepository.deleteAll();
        ruleSetRepository.deleteAll();
    }

    private String generateJwt(UUID userId, String email, List<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .issuer(jwtProperties.getJwt().getIssuer())
                .signWith(signingKey)
                .compact();
    }

    private StudentProfileDto createStudentProfile(UUID userId, BigDecimal gpa, String country, BigDecimal income) {
        return new StudentProfileDto(
                UUID.randomUUID(),
                userId,
                "Test",
                "Student",
                "Test Student",
                LocalDate.of(2002, 6, 20),
                country,
                "CA",
                "UNDERGRADUATE",
                "UC Berkeley",
                "Computer Science",
                "Junior",
                gpa,
                BigDecimal.valueOf(4.00),
                income,
                "USD",
                true,
                false,
                false,
                Map.of()
        );
    }

    @Test
    @DisplayName("Flyway database migrations should have initialized all required tables")
    void shouldVerifyFlywayMigrationInitializedTables() {
        assertNotNull(ruleSetRepository);
        assertNotNull(criterionRepository);
        assertNotNull(evaluationRepository);
    }

    @Test
    @DisplayName("Admin creates rule set successfully and student is denied creation")
    void shouldAllowAdminAndDenyStudentRuleSetCreation() throws Exception {
        String adminToken = generateJwt(adminId, "admin@prestiq.org", List.of("ROLE_ADMIN"));
        String studentToken = generateJwt(studentAId, "student@prestiq.org", List.of("ROLE_STUDENT"));

        CreateRuleSetRequest request = new CreateRuleSetRequest(
                101L,
                1,
                RuleSetStatus.ACTIVE,
                List.of(
                        new CreateCriterionRequest(
                                "MIN_GPA",
                                "academic.cumulativeGpa",
                                CriterionOperator.GTE,
                                "3.50",
                                true,
                                0,
                                "Minimum GPA of 3.50",
                                null,
                                null
                        ),
                        new CreateCriterionRequest(
                                "CITIZENSHIP",
                                "demographics.citizenshipCountry",
                                CriterionOperator.IN,
                                "[\"US\", \"CA\"]",
                                true,
                                0,
                                "Citizenship in US or CA",
                                null,
                                null
                        )
                )
        );

        // Student attempt should be 403 Forbidden
        mockMvc.perform(post("/api/v1/admin/eligibility/rulesets")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Admin attempt should be 201 Created
        mockMvc.perform(post("/api/v1/admin/eligibility/rulesets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scholarshipId").value(101))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.criteria", hasSize(2)));
    }

    @Test
    @DisplayName("Evaluate student, verify persistence and enforce cross-user access isolation")
    void shouldEvaluateStudentAndEnforceCrossUserIsolation() throws Exception {
        String adminToken = generateJwt(adminId, "admin@prestiq.org", List.of("ROLE_ADMIN"));
        String studentAToken = generateJwt(studentAId, "studentA@prestiq.org", List.of("ROLE_STUDENT"));
        String studentBToken = generateJwt(studentBId, "studentB@prestiq.org", List.of("ROLE_STUDENT"));

        // 1. Admin creates active rule set
        CreateRuleSetRequest ruleReq = new CreateRuleSetRequest(
                202L,
                1,
                RuleSetStatus.ACTIVE,
                List.of(
                        new CreateCriterionRequest(
                                "MIN_GPA",
                                "academic.cumulativeGpa",
                                CriterionOperator.GTE,
                                "3.50",
                                true,
                                0,
                                "Minimum GPA of 3.50",
                                "GPA of {studentValue} meets the required {targetValue}.",
                                "Requires {targetValue}, but found {studentValue}."
                        )
                )
        );

        mockMvc.perform(post("/api/v1/admin/eligibility/rulesets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleReq)))
                .andExpect(status().isCreated());

        // 2. Student A evaluates against scholarship 202
        StudentProfileDto studentAProfile = createStudentProfile(studentAId, BigDecimal.valueOf(3.85), "US", BigDecimal.valueOf(40000.00));
        EvaluateStudentRequest evalReq = new EvaluateStudentRequest(202L, studentAProfile);

        MvcResult result = mockMvc.perform(post("/api/v1/eligibility/evaluate")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.matchPercentage").value(100.0))
                .andExpect(jsonPath("$.criteriaBreakdown", hasSize(1)))
                .andExpect(jsonPath("$.criteriaBreakdown[0].status").value("PASS"))
                .andReturn();

        EligibilityEvaluationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                EligibilityEvaluationResponse.class
        );
        UUID evaluationId = response.id();
        assertNotNull(evaluationId);

        // 3. Student A accesses their own evaluation -> 200 OK
        mockMvc.perform(get("/api/v1/eligibility/evaluations/" + evaluationId)
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evaluationId.toString()))
                .andExpect(jsonPath("$.studentId").value(studentAId.toString()));

        // 4. Student B attempts to access Student A's evaluation -> 403 Forbidden!
        mockMvc.perform(get("/api/v1/eligibility/evaluations/" + evaluationId)
                        .header("Authorization", "Bearer " + studentBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));

        // 5. Admin accesses Student A's evaluation -> 200 OK
        mockMvc.perform(get("/api/v1/eligibility/evaluations/" + evaluationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evaluationId.toString()));
    }

    @Test
    @DisplayName("Internal batch evaluation should be allowed with valid internal key and denied without")
    void shouldProtectInternalBatchEvaluation() throws Exception {
        String adminToken = generateJwt(adminId, "admin@prestiq.org", List.of("ROLE_ADMIN"));

        // Admin creates rule sets for scholarships 301 and 302
        mockMvc.perform(post("/api/v1/admin/eligibility/rulesets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRuleSetRequest(
                                301L, 1, RuleSetStatus.ACTIVE,
                                List.of(new CreateCriterionRequest("GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.0", true, 0, "GPA", null, null))
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/eligibility/rulesets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRuleSetRequest(
                                302L, 1, RuleSetStatus.ACTIVE,
                                List.of(new CreateCriterionRequest("GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.9", true, 0, "GPA", null, null))
                        ))))
                .andExpect(status().isCreated());

        StudentProfileDto studentProfile = createStudentProfile(studentAId, BigDecimal.valueOf(3.50), "US", BigDecimal.valueOf(35000.00));
        BatchEvaluateRequest batchReq = new BatchEvaluateRequest(studentProfile, List.of(301L, 302L));

        // 1. Without any credentials -> 401 Unauthorized
        mockMvc.perform(post("/internal/v1/eligibility/batch-evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isUnauthorized());

        // 2. With student bearer token (authenticated, but lacking ROLE_INTERNAL/ROLE_ADMIN) -> 403 Forbidden
        String studentToken = generateJwt(studentAId, "student@prestiq.org", List.of("ROLE_STUDENT"));
        mockMvc.perform(post("/internal/v1/eligibility/batch-evaluate")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isForbidden());

        // 3. With valid X-Internal-Service-Key -> 200 OK
        mockMvc.perform(post("/internal/v1/eligibility/batch-evaluate")
                        .header("X-Internal-Service-Key", jwtProperties.getInternalServiceKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations", hasSize(2)))
                .andExpect(jsonPath("$.evaluations[0].scholarshipId").value(301))
                .andExpect(jsonPath("$.evaluations[0].verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.evaluations[1].scholarshipId").value(302))
                .andExpect(jsonPath("$.evaluations[1].verdict").value("INELIGIBLE"));
    }

    @Test
    @DisplayName("Untrusted spoofed headers should be rejected when X-Internal-Service-Key is absent")
    void shouldRejectSpoofedHeadersWithoutInternalKey() throws Exception {
        // Attacker attempts to spoof X-User-Roles: ROLE_ADMIN without internal key
        mockMvc.perform(get("/api/v1/admin/eligibility/rulesets/1")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
