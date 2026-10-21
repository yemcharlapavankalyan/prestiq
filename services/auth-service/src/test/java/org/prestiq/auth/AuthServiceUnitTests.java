package org.prestiq.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.auth.config.JwtProperties;
import org.prestiq.auth.domain.entity.Role;
import org.prestiq.auth.domain.entity.User;
import org.prestiq.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceUnitTests {

    @Test
    @DisplayName("Argon2id password encoder hashes and verifies passwords correctly")
    void argon2PasswordEncoderShouldHashAndVerify() {
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String rawPassword = "SecurePassword123!";

        String encodedHash = encoder.encode(rawPassword);

        assertThat(encodedHash).startsWith("$argon2id$");
        assertThat(encoder.matches(rawPassword, encodedHash)).isTrue();
        assertThat(encoder.matches("WrongPassword!", encodedHash)).isFalse();
    }

    @Test
    @DisplayName("JWT Token Provider generates, validates, and extracts claims from token")
    void jwtTokenProviderShouldGenerateAndValidateToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        properties.setExpirationSeconds(300);
        properties.setIssuer("prestiq-auth-unit-test");

        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);

        UUID userId = UUID.randomUUID();
        User user = new User(userId, "student@university.edu", "hash", "Jane", "Doe");
        user.setRoles(Collections.singleton(new Role(1, Role.ROLE_STUDENT, "Student")));

        String token = tokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("student@university.edu");
        assertThat(tokenProvider.getRolesFromToken(token)).contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("JWT Token Provider rejects invalid and tampered tokens")
    void jwtTokenProviderShouldRejectTamperedToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);

        String tamperedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }
}
