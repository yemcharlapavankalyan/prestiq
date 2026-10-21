package org.prestiq.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RequestHeaderSanitizationFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestHeaderSanitizationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Check and strip untrusted identity and internal service headers (case-insensitive)
        java.util.Set<String> headersToRemove = request.getHeaders().keySet().stream()
                .filter(name -> {
                    String lower = name.toLowerCase();
                    return lower.startsWith("x-user-")
                            || lower.startsWith("x-student-")
                            || lower.equals("x-internal-service-key");
                })
                .collect(java.util.stream.Collectors.toSet());

        if (!headersToRemove.isEmpty()) {
            log.warn("Stripped untrusted identity or internal keys from incoming request from IP: {} - headers: {}",
                    request.getRemoteAddress(), headersToRemove);
        }

        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Mutate request: strip untrusted identity and internal keys, set clean X-Request-Id
        String finalRequestId = requestId;
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    for (String headerName : headersToRemove) {
                        headers.remove(headerName);
                    }
                    headers.set("X-Request-Id", finalRequestId);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
