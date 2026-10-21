package org.prestiq.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalGatewayExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String code;
        String detail;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = "ERR_ROUTE_NOT_FOUND";
            detail = "The requested resource could not be found on the gateway or downstream service.";
            log.warn("Route not found for path: {}", exchange.getRequest().getPath().value());
        } else if (ex instanceof TimeoutException || ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "ERR_GATEWAY_TIMEOUT";
            detail = "Downstream service failed to respond within the configured timeout window.";
            log.error("Gateway timeout for path: {} - {}", exchange.getRequest().getPath().value(), ex.getMessage());
        } else if (ex instanceof ConnectException || (ex.getCause() != null && ex.getCause() instanceof ConnectException)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "ERR_SERVICE_UNAVAILABLE";
            detail = "Downstream service is temporarily unavailable or unreachable. Core scholarship services remain protected.";
            log.error("Service connection refused for path: {} - {}", exchange.getRequest().getPath().value(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = "ERR_HTTP_" + status.value();
            detail = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            log.warn("Response status exception for path: {} - HTTP {}", exchange.getRequest().getPath().value(), status.value());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "ERR_GATEWAY_INTERNAL";
            detail = "An internal gateway error occurred while processing the request.";
            log.error("Unexpected gateway exception for path: {}", exchange.getRequest().getPath().value(), ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://api.prestiq.org/errors/" + code.toLowerCase().replace('_', '-'));
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", exchange.getRequest().getPath().value());
        problem.put("code", code);
        problem.put("serviceId", "api-gateway");
        problem.put("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(problem).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"detail\":\"" + detail + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
