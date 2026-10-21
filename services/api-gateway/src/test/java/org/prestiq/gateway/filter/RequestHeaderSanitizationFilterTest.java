package org.prestiq.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHeaderSanitizationFilterTest {

    private final RequestHeaderSanitizationFilter filter = new RequestHeaderSanitizationFilter();

    @Test
    @DisplayName("Should strip untrusted identity headers and internal service keys from incoming request")
    void shouldStripUntrustedHeaders() {
        MockServerHttpRequest incomingRequest = MockServerHttpRequest.get("/api/v1/scholarships")
                .header("X-User-Id", "attacker-uuid")
                .header("x-user-roles", "ROLE_ADMIN")
                .header("X-User-Email", "attacker@evil.com")
                .header("x-student-id", "student-uuid-spoof")
                .header("X-Internal-Service-Key", "forged-secret-key")
                .header("Authorization", "Bearer valid-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(incomingRequest);
        AtomicReference<ServerHttpRequest> forwardedRequestRef = new AtomicReference<>();

        WebFilterChain chain = ex -> {
            forwardedRequestRef.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerHttpRequest forwarded = forwardedRequestRef.get();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeaders().containsKey("X-User-Id")).isFalse();
        assertThat(forwarded.getHeaders().containsKey("x-user-roles")).isFalse();
        assertThat(forwarded.getHeaders().containsKey("X-User-Email")).isFalse();
        assertThat(forwarded.getHeaders().containsKey("x-student-id")).isFalse();
        assertThat(forwarded.getHeaders().containsKey("X-Internal-Service-Key")).isFalse();

        // Valid Authorization header and generated X-Request-Id must be present
        assertThat(forwarded.getHeaders().getFirst("Authorization")).isEqualTo("Bearer valid-token");
        assertThat(forwarded.getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("Should preserve existing X-Request-Id if provided by caller")
    void shouldPreserveExistingRequestId() {
        MockServerHttpRequest incomingRequest = MockServerHttpRequest.get("/api/v1/scholarships")
                .header("X-Request-Id", "custom-client-trace-id-12345")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(incomingRequest);
        AtomicReference<ServerHttpRequest> forwardedRequestRef = new AtomicReference<>();

        WebFilterChain chain = ex -> {
            forwardedRequestRef.set(ex.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerHttpRequest forwarded = forwardedRequestRef.get();
        assertThat(forwarded.getHeaders().getFirst("X-Request-Id")).isEqualTo("custom-client-trace-id-12345");
    }
}
