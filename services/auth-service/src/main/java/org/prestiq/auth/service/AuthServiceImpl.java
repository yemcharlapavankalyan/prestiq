package org.prestiq.auth.service;

import jakarta.servlet.http.Cookie;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            CookieProperties cookieProperties,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.cookieProperties = cookieProperties;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent, HttpServletResponse response) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            auditLogService.logEvent(null, normalizedEmail, "REGISTER_FAILED_DUPLICATE", ipAddress, userAgent, "Duplicate email attempt");
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName().trim(),
                request.getLastName().trim()
        );

        Role studentRole = roleRepository.findByName(Role.ROLE_STUDENT)
                .orElseGet(() -> roleRepository.save(new Role(1, Role.ROLE_STUDENT, "Standard student applicant")));
        user.getRoles().add(studentRole);

        User savedUser = userRepository.save(user);
        auditLogService.logEvent(savedUser.getId(), normalizedEmail, "USER_REGISTERED", ipAddress, userAgent, "Successful registration");

        return generateAuthResponse(savedUser, response);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent, HttpServletResponse response) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    auditLogService.logEvent(null, normalizedEmail, "LOGIN_FAILED", ipAddress, userAgent, "User not found");
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_FAILED", ipAddress, userAgent, "Bad credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_BLOCKED_INACTIVE", ipAddress, userAgent, "Account disabled");
            throw new AccountDisabledException("Account is disabled. Contact support.");
        }

        auditLogService.logEvent(user.getId(), normalizedEmail, "LOGIN_SUCCESS", ipAddress, userAgent, "Successful login");
        return generateAuthResponse(user, response);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        String hash = sha256Hex(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or invalid"));

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(token.getUserId());
            auditLogService.logEvent(token.getUserId(), null, "TOKEN_COMPROMISED", ipAddress, userAgent, "Compromised token reuse detected");
            throw new TokenCompromisedException("Compromised refresh token reused. All sessions revoked.");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Revoke the used refresh token (rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User for token no longer exists"));

        auditLogService.logEvent(user.getId(), user.getEmail(), "TOKEN_REFRESHED", ipAddress, userAgent, "Successful token rotation");
        return generateAuthResponse(user, response);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = sha256Hex(rawRefreshToken);
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(hash);
            if (tokenOpt.isPresent()) {
                RefreshToken token = tokenOpt.get();
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                auditLogService.logEvent(token.getUserId(), null, "USER_LOGOUT", ipAddress, userAgent, "Session terminated");
            }
        }

        if (response != null) {
            Cookie cookie = new Cookie(cookieProperties.getName(), "");
            cookie.setMaxAge(0);
            cookie.setPath(cookieProperties.getPath());
            cookie.setHttpOnly(true);
            cookie.setSecure(cookieProperties.isSecure());
            response.addCookie(cookie);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found: " + userId));
        return mapToDto(user);
    }

    private AuthResponse generateAuthResponse(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        long expiresInSeconds = jwtProperties.getExpirationSeconds();

        // Issue rotated raw refresh token
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        // Attach HttpOnly cookie
        if (response != null) {
            Cookie cookie = new Cookie(cookieProperties.getName(), rawRefreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(cookieProperties.isSecure());
            cookie.setPath(cookieProperties.getPath());
            cookie.setMaxAge((int) (jwtProperties.getRefreshTokenExpirationDays() * 86400));
            response.addCookie(cookie);
        }

        return new AuthResponse(accessToken, expiresInSeconds, mapToDto(user));
    }

    private UserDto mapToDto(User user) {
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

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
