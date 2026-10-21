package org.prestiq.scholarship.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TestJwtHelper {

    private static final String TEST_SECRET = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        return Jwts.builder()
                .subject(userId != null ? userId.toString() : UUID.randomUUID().toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer("prestiq-auth-test")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public static String generateExpiredToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now().minusSeconds(7200);
        Instant expiry = now.plusSeconds(3600);

        return Jwts.builder()
                .subject(userId != null ? userId.toString() : UUID.randomUUID().toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuer("prestiq-auth-test")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(SIGNING_KEY)
                .compact();
    }
}
