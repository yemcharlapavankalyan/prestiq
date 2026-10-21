package org.prestiq.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InternalPathBlockFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(InternalPathBlockFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/internal/") || path.contains("/internal/")) {
            log.warn("Blocked public access attempt to internal service path: {} from IP: {}",
                    path, exchange.getRequest().getRemoteAddress());

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

            Map<String, Object> problem = new LinkedHashMap<>();
            problem.put("type", "https://api.prestiq.org/errors/forbidden");
            problem.put("title", "Forbidden");
            problem.put("status", HttpStatus.FORBIDDEN.value());
            problem.put("detail", "Internal service endpoints cannot be accessed through the public API gateway.");
            problem.put("instance", path);
            problem.put("code", "ERR_INTERNAL_ROUTE_PROHIBITED");
            problem.put("serviceId", "api-gateway");
            problem.put("timestamp", Instant.now().toString());

            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsString(problem).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException e) {
                bytes = "{\"detail\":\"Internal route prohibited\"}".getBytes(StandardCharsets.UTF_8);
            }

            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
