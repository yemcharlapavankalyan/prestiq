package org.prestiq.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.auth.domain.entity.RefreshToken;
import org.prestiq.auth.domain.entity.Role;
import org.prestiq.auth.dto.request.LoginRequest;
import org.prestiq.auth.dto.request.RegisterRequest;
import org.prestiq.auth.repository.AuthAuditEventRepository;
import org.prestiq.auth.repository.RefreshTokenRepository;
import org.prestiq.auth.repository.RoleRepository;
import org.prestiq.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthAuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        auditEventRepository.deleteAll();

        if (roleRepository.findByName(Role.ROLE_STUDENT).isEmpty()) {
            roleRepository.save(new Role(1, Role.ROLE_STUDENT, "Student role"));
        }
        if (roleRepository.findByName(Role.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(2, Role.ROLE_ADMIN, "Admin role"));
        }
    }

    @Test
    @DisplayName("POST /api/auth/register successfully creates user and sets HttpOnly refresh cookie")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "alex.rivera@university.edu",
                "StrongP@ssw0rd!",
                "Alex",
                "Rivera"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("alex.rivera@university.edu"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT"))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Verify sensitive data exclusion
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).doesNotContain("passwordHash");
        assertThat(responseContent).doesNotContain("StrongP@ssw0rd!");

        // Verify cookie is HttpOnly
        Cookie cookie = result.getResponse().getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("POST /api/auth/register rejects duplicate email with 409 Conflict")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@university.edu",
                "StrongP@ssw0rd!",
                "First",
                "User"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("An account with this email address already exists."));
    }

    @Test
    @DisplayName("POST /api/auth/register rejects invalid inputs with RFC 9457 Problem Details")
    void shouldRejectInvalidRegistrationInputs() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "not-an-email",
                "short", // too short, no uppercase, no symbol
                "",
                ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/login succeeds with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        RegisterRequest register = new RegisterRequest("login.user@university.edu", "StrongP@ssw0rd!", "Login", "User");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        LoginRequest login = new LoginRequest("login.user@university.edu", "StrongP@ssw0rd!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login.user@university.edu"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns neutral 401 on wrong password (prevent user enumeration)")
    void shouldFailLoginOnWrongPassword() throws Exception {
        RegisterRequest register = new RegisterRequest("enum.test@university.edu", "StrongP@ssw0rd!", "Enum", "Test");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        LoginRequest loginWrongPassword = new LoginRequest("enum.test@university.edu", "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWrongPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        LoginRequest loginUnknownEmail = new LoginRequest("nonexistent@university.edu", "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUnknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns authenticated user with valid Bearer token")
    void shouldAccessProtectedMeEndpoint() throws Exception {
        RegisterRequest register = new RegisterRequest("me.user@university.edu", "StrongP@ssw0rd!", "Me", "User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        JsonNode json = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = json.get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me.user@university.edu"))
                .andExpect(jsonPath("$.firstName").value("Me"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 Unauthorized when token is missing")
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh rotates refresh token and invalidates old token")
    void shouldRotateRefreshTokenSuccessfully() throws Exception {
        RegisterRequest register = new RegisterRequest("refresh.user@university.edu", "StrongP@ssw0rd!", "Ref", "User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        Cookie originalCookie = regResult.getResponse().getCookie("refreshToken");
        assertThat(originalCookie).isNotNull();
        String originalRefreshToken = originalCookie.getValue();

        // Perform first refresh
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie newCookie = refreshResult.getResponse().getCookie("refreshToken");
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isNotEqualTo(originalRefreshToken);

        // REUSE DETECTION: Attempting to use the old, already-rotated refresh token
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", originalRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Token compromise detected")));

        // Verify that all refresh tokens for this user are now revoked
        assertThat(refreshTokenRepository.findAll()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("POST /api/auth/logout revokes active refresh token and clears cookie")
    void shouldLogoutSuccessfully() throws Exception {
        RegisterRequest register = new RegisterRequest("logout.user@university.edu", "StrongP@ssw0rd!", "Logout", "User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        Cookie cookie = regResult.getResponse().getCookie("refreshToken");
        assertThat(cookie).isNotNull();

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn();

        // Verify cookie is cleared (maxAge: 0)
        Cookie clearedCookie = logoutResult.getResponse().getCookie("refreshToken");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isEqualTo(0);

        // Verify token is revoked in database
        assertThat(refreshTokenRepository.findAll()).allMatch(RefreshToken::isRevoked);
    }
}
