package org.prestiq.gateway.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.client.enabled=false",
        "AUTH_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "STUDENT_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "SCHOLARSHIP_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "ELIGIBILITY_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "RECOMMENDATION_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "APPLICATION_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "NOTIFICATION_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "AI_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "INGESTION_SERVICE_URL=http://localhost:${wiremock.server.port}"
})
class EndToEndIntegrationTest {

    private static final String JWT_SECRET = "d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2";
    private static final String JWT_ISSUER = "prestiq-auth";

    @Autowired
    private WebTestClient webTestClient;

    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        this.signingKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer(JWT_ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("1. Authentication through the Gateway (Login & Token Receipt)")
    void flow1_authenticationThroughGateway() {
        stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"tokenType\":\"Bearer\",\"accessToken\":\"jwt-sample-token\",\"expiresInSeconds\":900}")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"student@university.edu\",\"password\":\"Password123!\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.accessToken").isEqualTo("jwt-sample-token")
                .jsonPath("$.expiresInSeconds").isEqualTo(900);

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/login")));
    }

    @Test
    @DisplayName("2. Student Profile Access with Correct Ownership and Header Sanitization")
    void flow2_studentProfileAccessWithCorrectOwnership() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(get(urlEqualTo("/api/v1/students/me/profile"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"userId\":\"" + studentId + "\",\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"cumulativeGpa\":3.85}")));

        // Attempt spoofing headers while presenting valid JWT
        webTestClient.get()
                .uri("/api/v1/students/me/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "spoofed-user-id")
                .header("X-Internal-Service-Key", "forged-secret-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(studentId.toString())
                .jsonPath("$.firstName").isEqualTo("Jane");

        // Verify downstream received the request with sanitized headers
        verify(getRequestedFor(urlEqualTo("/api/v1/students/me/profile"))
                .withoutHeader("X-User-Id")
                .withoutHeader("X-Internal-Service-Key")
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token)));
    }

    @Test
    @DisplayName("3. Scholarship Creation and Verification Workflow")
    void flow3_scholarshipCreationAndVerificationWorkflow() {
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String adminToken = createToken(adminId, "admin@prestiq.test", List.of("ROLE_ADMIN"));

        stubFor(post(urlEqualTo("/api/v1/admin/scholarships"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":101,\"title\":\"STEM Excellence Award\",\"verificationStatus\":\"DRAFT\"}")));

        stubFor(put(urlEqualTo("/api/v1/admin/scholarships/101/verify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":101,\"title\":\"STEM Excellence Award\",\"verificationStatus\":\"VERIFIED\"}")));

        // Create scholarship
        webTestClient.post()
                .uri("/api/v1/admin/scholarships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"STEM Excellence Award\",\"providerName\":\"National Science Foundation\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(101)
                .jsonPath("$.verificationStatus").isEqualTo("DRAFT");

        // Verify scholarship
        webTestClient.put()
                .uri("/api/v1/admin/scholarships/101/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verificationStatus").isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("4. Deterministic Eligibility Evaluation")
    void flow4_deterministicEligibilityEvaluation() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(get(urlEqualTo("/api/v1/eligibility/check/101"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"scholarshipId\":101,\"verdict\":\"ELIGIBLE\",\"matchPercentage\":100.0}")));

        webTestClient.get()
                .uri("/api/v1/eligibility/check/101")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.scholarshipId").isEqualTo(101)
                .jsonPath("$.verdict").isEqualTo("ELIGIBLE")
                .jsonPath("$.matchPercentage").isEqualTo(100.0);

        verify(getRequestedFor(urlEqualTo("/api/v1/eligibility/check/101")));
    }

    @Test
    @DisplayName("5. Recommendation Retrieval")
    void flow5_recommendationRetrieval() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(get(urlEqualTo("/api/v1/recommendations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"scholarshipId\":101,\"matchScore\":95.5,\"eligibilityVerdict\":\"ELIGIBLE\"}]")));

        webTestClient.get()
                .uri("/api/v1/recommendations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].scholarshipId").isEqualTo(101)
                .jsonPath("$[0].matchScore").isEqualTo(95.5)
                .jsonPath("$[0].eligibilityVerdict").isEqualTo("ELIGIBLE");

        verify(getRequestedFor(urlEqualTo("/api/v1/recommendations")));
    }

    @Test
    @DisplayName("6. Saved Scholarship and Application Creation")
    void flow6_savedScholarshipAndApplicationCreation() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(post(urlEqualTo("/api/v1/saved-scholarships/101"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"studentId\":\"" + studentId + "\",\"scholarshipId\":101}")));

        stubFor(post(urlEqualTo("/api/v1/applications"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"11111111-1111-1111-1111-111111111111\",\"studentId\":\"" + studentId + "\",\"scholarshipId\":101,\"currentStatus\":\"PLANNING\"}")));

        // Bookmark scholarship
        webTestClient.post()
                .uri("/api/v1/saved-scholarships/101")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.scholarshipId").isEqualTo(101);

        // Track application
        webTestClient.post()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"scholarshipId\":101,\"scholarshipTitle\":\"STEM Excellence Award\",\"submissionDeadline\":\"2026-12-01\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.currentStatus").isEqualTo("PLANNING");
    }

    @Test
    @DisplayName("7. Notification Creation and Retrieval")
    void flow7_notificationCreationAndRetrieval() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(get(urlEqualTo("/api/v1/notifications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"id\":\"22222222-2222-2222-2222-222222222222\",\"type\":\"DEADLINE_REMINDER\",\"title\":\"Upcoming Deadline\",\"isRead\":false}]")));

        webTestClient.get()
                .uri("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].type").isEqualTo("DEADLINE_REMINDER")
                .jsonPath("$[0].title").isEqualTo("Upcoming Deadline");

        verify(getRequestedFor(urlEqualTo("/api/v1/notifications")));
    }

    @Test
    @DisplayName("8. AI Request Routing and Source Citation Response")
    void flow8_aiRequestRoutingAndSourceCitationResponse() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = createToken(studentId, "student@university.edu", List.of("ROLE_STUDENT"));

        stubFor(post(urlEqualTo("/api/v1/assistant/chat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"answer\":\"Applicants must be enrolled full-time.\",\"confidenceScore\":0.95,\"citations\":[{\"sourceName\":\"Official FAQ\",\"pageNumber\":1}]}")));

        webTestClient.post()
                .uri("/api/v1/assistant/chat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"scholarshipId\":101,\"question\":\"Is full-time enrollment required?\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Applicants must be enrolled full-time.")
                .jsonPath("$.confidenceScore").isEqualTo(0.95)
                .jsonPath("$.citations[0].sourceName").isEqualTo("Official FAQ");

        verify(postRequestedFor(urlEqualTo("/api/v1/assistant/chat")));
    }

    @Test
    @DisplayName("9. Ingestion Candidate Retrieval via Ingestion Service Route")
    void flow9_ingestionCandidateRetrieval() {
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String adminToken = createToken(adminId, "admin@prestiq.test", List.of("ROLE_ADMIN"));

        stubFor(get(urlEqualTo("/api/v1/ingestion/candidates"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"id\":501,\"title\":\"National Merit Scholarship 2026\",\"status\":\"PENDING_REVIEW\"}]")));

        webTestClient.get()
                .uri("/api/v1/ingestion/candidates")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(501)
                .jsonPath("$[0].title").isEqualTo("National Merit Scholarship 2026")
                .jsonPath("$[0].status").isEqualTo("PENDING_REVIEW");

        verify(getRequestedFor(urlEqualTo("/api/v1/ingestion/candidates")));
    }

    @Test
    @DisplayName("10. Rejection of Unauthorized and Internal-Only Routes (RFC 7807 403 Forbidden)")
    void flow10_rejectionOfUnauthorizedAndInternalOnlyRoutes() {
        // Direct attempt to access internal service API via public gateway
        webTestClient.get()
                .uri("/internal/v1/students/00000000-0000-0000-0000-000000000001/profile")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ERR_INTERNAL_ROUTE_PROHIBITED")
                .jsonPath("$.status").isEqualTo(403);

        // Attempting to access nested internal path
        webTestClient.post()
                .uri("/api/v1/assistant/internal/index")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ERR_INTERNAL_ROUTE_PROHIBITED")
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    @DisplayName("11. Unavailable Downstream Service Behavior (RFC 7807 503 / 404)")
    void flow11_unavailableDownstreamServiceBehavior() {
        // Request to path that has no WireMock stub or server mapping
        webTestClient.get()
                .uri("/api/v1/unknown-service/endpoint")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    @DisplayName("12. Duplicate Requests and Idempotency Propagation")
    void flow12_duplicateRequestsAndIdempotencyPropagation() {
        String clientTraceId = "client-trace-" + UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/v1/scholarships/101"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":101,\"title\":\"STEM Excellence Award\"}")));

        // First invocation
        webTestClient.get()
                .uri("/api/v1/scholarships/101")
                .header("X-Request-Id", clientTraceId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(101);

        // Duplicate invocation with identical trace ID
        webTestClient.get()
                .uri("/api/v1/scholarships/101")
                .header("X-Request-Id", clientTraceId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(101);

        // Verify downstream received exactly the client's X-Request-Id both times
        verify(2, getRequestedFor(urlEqualTo("/api/v1/scholarships/101"))
                .withHeader("X-Request-Id", equalTo(clientTraceId)));
    }
}
