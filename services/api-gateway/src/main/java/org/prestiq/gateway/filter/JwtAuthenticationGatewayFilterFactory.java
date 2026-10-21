package org.prestiq.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.prestiq.gateway.config.GatewaySecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private final GatewaySecurityProperties securityProperties;
    private final SecretKey signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationGatewayFilterFactory(GatewaySecurityProperties securityProperties) {
        super(Config.class);
        this.securityProperties = securityProperties;
        byte[] keyBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (config.isRequired()) {
                    return writeProblemResponse(exchange, HttpStatus.UNAUTHORIZED,
                            "Missing or malformed Authorization header. Bearer token required.",
                            "ERR_UNAUTHORIZED");
                }
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7).trim();
            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException e) {
                log.debug("JWT token expired: {}", e.getMessage());
                if (config.isRequired()) {
                    return writeProblemResponse(exchange, HttpStatus.UNAUTHORIZED,
                            "Authentication token has expired.", "ERR_TOKEN_EXPIRED");
                }
                return chain.filter(exchange);
            } catch (SecurityException | IllegalArgumentException e) {
                log.warn("Invalid JWT signature or token format: {}", e.getMessage());
                if (config.isRequired()) {
                    return writeProblemResponse(exchange, HttpStatus.UNAUTHORIZED,
                            "Invalid authentication token signature or format.", "ERR_INVALID_TOKEN");
                }
                return chain.filter(exchange);
            }

            // Extract claims
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            List<?> rawRoles = claims.get("roles", List.class);
            List<String> roles = new ArrayList<>();
            if (rawRoles != null) {
                for (Object r : rawRoles) {
                    if (r != null) {
                        roles.add(r.toString());
                    }
                }
            }

            // Role enforcement if specified
            if (!config.getRequiredRoles().isEmpty()) {
                boolean hasRole = config.getRequiredRoles().stream()
                        .anyMatch(r -> roles.contains(r) || roles.contains("ROLE_ADMIN"));
                if (!hasRole) {
                    return writeProblemResponse(exchange, HttpStatus.FORBIDDEN,
                            "Access denied: Insufficient privileges to access this resource.",
                            "ERR_FORBIDDEN");
                }
            }

            String rolesHeader = String.join(",", roles);
            String internalKey = securityProperties.getInternalServiceKey();

            // Mutate request with verified identity headers and internal service key, preserving Authorization
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Roles", rolesHeader)
                    .header("X-Internal-Service-Key", internalKey != null ? internalKey : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> writeProblemResponse(ServerWebExchange exchange, HttpStatus status, String detail, String code) {
        ServerHttpResponse response = exchange.getResponse();
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

    public static class Config {
        private boolean required = true;
        private List<String> requiredRoles = Collections.emptyList();

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }
}
