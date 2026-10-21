package org.prestiq.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    @DisplayName("Should permit CORS preflight from allowed origin with credentials")
    void shouldPermitAllowedOrigin() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");
        CorsWebFilter filter = corsConfig.corsWebFilter();

        MockServerHttpRequest request = MockServerHttpRequest.options("http://localhost:8080/api/v1/scholarships")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        assertThat(headers.getAccessControlAllowCredentials()).isTrue();
        assertThat(headers.getAccessControlAllowMethods()).contains(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    @DisplayName("Should reject CORS preflight from unauthorized origin")
    void shouldRejectDisallowedOrigin() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");
        CorsWebFilter filter = corsConfig.corsWebFilter();

        MockServerHttpRequest request = MockServerHttpRequest.options("http://localhost:8080/api/v1/scholarships")
                .header(HttpHeaders.ORIGIN, "http://unauthorized-attacker.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getAccessControlAllowOrigin()).isNull();
    }
}
