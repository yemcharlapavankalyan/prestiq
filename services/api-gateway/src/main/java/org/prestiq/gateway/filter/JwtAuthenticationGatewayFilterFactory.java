package org.prestiq.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.prestiq.gateway.config.GatewaySecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private final GatewaySecurityProperties properties;

    public JwtAuthenticationGatewayFilterFactory(GatewaySecurityProperties properties) {
        super(Config.class);
        this.properties = properties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7).trim();
            SecretKey signingKey = Keys.hmacShaKeyFor(
                    properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
            );

            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired: {}", e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token expired");
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
            }

            // Extract roles
            List<String> userRoles = new ArrayList<>();
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof Collection<?> col) {
                for (Object item : col) {
                    if (item != null) {
                        userRoles.add(item.toString());
                    }
                }
            } else if (rolesObj instanceof String s) {
                for (String part : s.split(",")) {
                    if (!part.isBlank()) {
                        userRoles.add(part.trim());
                    }
                }
            }

            // Validate required roles if configured
            if (config.getRequiredRoles() != null && !config.getRequiredRoles().isEmpty()) {
                boolean hasRole = config.getRequiredRoles().stream().anyMatch(userRoles::contains);
                if (!hasRole) {
                    log.warn("User {} lacking required roles: {}", claims.getSubject(), config.getRequiredRoles());
                    return onError(exchange, HttpStatus.FORBIDDEN, "Forbidden: insufficient role privileges");
                }
            }

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String rolesJoined = String.join(",", userRoles);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Roles", rolesJoined)
                    .header("X-Internal-Service-Key", properties.getInternalServiceKey())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        log.debug("Authentication failed: {} (Status: {})", message, status);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private List<String> requiredRoles;

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }
}

