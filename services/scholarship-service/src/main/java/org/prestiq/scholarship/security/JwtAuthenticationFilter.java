package org.prestiq.scholarship.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.prestiq.scholarship.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenValidator tokenValidator;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator, JwtProperties jwtProperties) {
        this.tokenValidator = tokenValidator;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. Bearer JWT Token validation
            String bearerToken = resolveBearerToken(request);
            if (StringUtils.hasText(bearerToken)) {
                if (tokenValidator.validateToken(bearerToken)) {
                    Claims claims = tokenValidator.getClaims(bearerToken);
                    UUID userId = UUID.fromString(claims.getSubject());
                    String email = claims.get("email", String.class);
                    List<String> roles = tokenValidator.getRolesFromToken(bearerToken);

                    List<SimpleGrantedAuthority> authorities = (roles != null)
                            ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                            : Collections.emptyList();

                    UserPrincipal principal = new UserPrincipal(userId, email, authorities, false);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("Invalid JWT Bearer token presented to scholarship-service from IP: {}", request.getRemoteAddr());
                }
            } else {
                // 2. Trusted Gateway / Inter-Service request validation
                // Protect against impersonation: NEVER trust X-User-* headers unless accompanied
                // by a cryptographically verified internal service key!
                String internalKey = request.getHeader("X-Internal-Service-Key");
                String configuredInternalKey = jwtProperties.getInternalServiceKey();

                if (StringUtils.hasText(internalKey) && StringUtils.hasText(configuredInternalKey) && configuredInternalKey.equals(internalKey)) {
                    String userIdHeader = request.getHeader("X-User-Id");
                    UUID userId = null;
                    if (StringUtils.hasText(userIdHeader)) {
                        try {
                            userId = UUID.fromString(userIdHeader);
                        } catch (IllegalArgumentException e) {
                            log.warn("Malformed UUID in X-User-Id header: {}", userIdHeader);
                        }
                    }

                    String email = request.getHeader("X-User-Email");
                    String rolesHeader = request.getHeader("X-User-Roles");

                    List<String> roles = StringUtils.hasText(rolesHeader)
                            ? Arrays.stream(rolesHeader.split(",")).map(String::trim).toList()
                            : List.of("ROLE_INTERNAL");

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UserPrincipal principal = new UserPrincipal(userId, email, authorities, true);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authenticated internal service request for: {}", email != null ? email : userId);
                }
            }
        } catch (Exception e) {
            log.error("Could not set authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
