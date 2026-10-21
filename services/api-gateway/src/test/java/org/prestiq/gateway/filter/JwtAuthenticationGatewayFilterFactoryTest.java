package org.prestiq.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.gateway.config.GatewaySecurityProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGatewayFilterFactoryTest {

    private static final String TEST_SECRET = "d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2";
    private JwtAuthenticationGatewayFilterFactory filterFactory;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        GatewaySecurityProperties props = new GatewaySecurityProperties();
        props.getJwt().setSecret(TEST_SECRET);
        props.setInternalServiceKey("internal-secret-2026");
        filterFactory = new JwtAuthenticationGatewayFilterFactory(props);

        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private String createToken(UUID userId, String email, List<String> roles, long expirySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer("prestiq-auth")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Should validate valid JWT, preserve Authorization, and inject verified headers")
    void shouldValidateAndInjectVerifiedHeaders() {
        UUID userId = UUID.randomUUID();
        String token = createToken(userId, "student@prestiq.org", List.of("ROLE_STUDENT"), 900);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/students/me/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        AtomicReference<ServerHttpRequest> forwardedRef = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwardedRef.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerHttpRequest forwarded = forwardedRef.get();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(forwarded.getHeaders().getFirst("X-User-Email")).isEqualTo("student@prestiq.org");
        assertThat(forwarded.getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_STUDENT");
        assertThat(forwarded.getHeaders().getFirst("X-Internal-Service-Key")).isEqualTo("internal-secret-2026");
        assertThat(forwarded.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for expired JWT")
    void shouldRejectExpiredToken() {
        UUID userId = UUID.randomUUID();
        String expiredToken = createToken(userId, "student@prestiq.org", List.of("ROLE_STUDENT"), -10);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/students/me/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user lacks required role")
    void shouldEnforceRequiredRoles() {
        UUID userId = UUID.randomUUID();
        String studentToken = createToken(userId, "student@prestiq.org", List.of("ROLE_STUDENT"), 900);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/scholarships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        config.setRequiredRoles(List.of("ROLE_ADMIN"));
        GatewayFilter filter = filterFactory.apply(config);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
