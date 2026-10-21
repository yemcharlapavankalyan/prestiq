package org.prestiq.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.prestiq.auth.config.CookieProperties;
import org.prestiq.auth.config.JwtProperties;
import org.prestiq.auth.domain.entity.RefreshToken;
import org.prestiq.auth.domain.entity.Role;
import org.prestiq.auth.domain.entity.User;
import org.prestiq.auth.dto.request.LoginRequest;
import org.prestiq.auth.dto.request.RegisterRequest;
import org.prestiq.auth.dto.response.AuthResponse;
import org.prestiq.auth.dto.response.UserDto;
import org.prestiq.auth.exception.AccountDisabledException;
import org.prestiq.auth.exception.DuplicateEmailException;
import org.prestiq.auth.exception.InvalidCredentialsException;
import org.prestiq.auth.exception.InvalidTokenException;
import org.prestiq.auth.exception.TokenCompromisedException;
import org.prestiq.auth.repository.RefreshTokenRepository;
import org.prestiq.auth.repository.RoleRepository;
import org.prestiq.auth.repository.UserRepository;
import org.prestiq.auth.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            CookieProperties cookieProperties,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.cookieProperties = cookieProperties;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent, HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            auditLogService.logEvent(null, normalizedEmail, "REGISTER_CONFLICT", ipAddress, userAgent, "Email already registered");
            throw new DuplicateEmailException("An account with this email address already exists.");
        }

        Role studentRole = roleRepository.findByName(Role.ROLE_STUDENT)
                .orElseGet(() -> roleRepository.save(new Role(1, Role.ROLE_STUDENT, "Student role")));

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName().trim(),
                request.getLastName().trim()
        );
        user.addRole(studentRole);
        user = userRepository.save(user);

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = createAndPersistRefreshToken(user.getId());
        attachRefreshTokenCookie(response, rawRefreshToken);

        auditLogService.logEvent(user.getId(), normalizedEmail, "REGISTER_SUCCESS", ipAddress, userAgent, "User registered successfully");
        return new AuthResponse(accessToken, tokenProvider.getExpirationSeconds(), toUserDto(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent, HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    // Constant-time behavior / prevent user enumeration
                    passwordEncoder.matches("dummy-password", "$argon2id$v=19$m=65536,t=3,p=1$c29tZXNhbHQ$qU9N7y8b4L");
                    auditLogService.logEvent(null, normalizedEmail, "LOGIN_FAILURE", ipAddress, userAgent, "Account not found");
                    return new InvalidCredentialsException();
                });

        if (!user.isActive()) {
            auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_FAILURE", ipAddress, userAgent, "Account disabled");
            throw new AccountDisabledException("This account has been deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_FAILURE", ipAddress, userAgent, "Incorrect password");
            throw new InvalidCredentialsException();
        }

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = createAndPersistRefreshToken(user.getId());
        attachRefreshTokenCookie(response, rawRefreshToken);

        auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_SUCCESS", ipAddress, userAgent, "Login successful");
        return new AuthResponse(accessToken, tokenProvider.getExpirationSeconds(), toUserDto(user));
    }

    @Override
    @Transactional(noRollbackFor = TokenCompromisedException.class)
    public AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required.");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));

        // Refresh token reuse detection (Token theft defense)
        if (refreshToken.isRevoked()) {
            log.warn("Security Alert: Replay of revoked refresh token detected for user '{}'. Revoking all active sessions.", refreshToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUserId());
            auditLogService.logEvent(refreshToken.getUserId(), "N/A", "TOKEN_REUSE_DETECTED", ipAddress, userAgent, "Revoked token reused. Terminated all user sessions.");
            throw new TokenCompromisedException("Token compromise detected: all sessions for this account have been terminated. Please log in again.");
        }

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User associated with token no longer exists."));

        if (!user.isActive()) {
            throw new AccountDisabledException("Account has been deactivated.");
        }

        // Single-Use Rotation: Invalidate current token and issue a fresh one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newRawRefreshToken = createAndPersistRefreshToken(user.getId());
        attachRefreshTokenCookie(response, newRawRefreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user);
        auditLogService.logEvent(user.getId(), user.getEmail(), "TOKEN_REFRESH", ipAddress, userAgent, "Rotated refresh token");

        return new AuthResponse(newAccessToken, tokenProvider.getExpirationSeconds(), toUserDto(user));
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                auditLogService.logEvent(token.getUserId(), "N/A", "LOGOUT", ipAddress, userAgent, "Session terminated");
            });
        }
        clearRefreshTokenCookie(response);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        return toUserDto(user);
    }

    private String createAndPersistRefreshToken(UUID userId) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plus(Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays()));
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private void attachRefreshTokenCookie(HttpServletResponse response, String rawRefreshToken) {
        ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), rawRefreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays()))
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserDto toUserDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleNames,
                user.isActive(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }
}
