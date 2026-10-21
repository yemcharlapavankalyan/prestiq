package org.prestiq.gateway.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalGatewayExceptionHandlerTest {

    private final GlobalGatewayExceptionHandler handler = new GlobalGatewayExceptionHandler();

    @Test
    @DisplayName("Should translate ConnectException to HTTP 503 Service Unavailable")
    void shouldHandleConnectException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/scholarships").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new ConnectException("Connection refused")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("Should translate TimeoutException to HTTP 504 Gateway Timeout")
    void shouldHandleTimeoutException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/assistant/chat").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new TimeoutException()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("Should translate NotFoundException to HTTP 404 Not Found")
    void shouldHandleNotFoundException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/unknown-route").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new NotFoundException("Route not found")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
