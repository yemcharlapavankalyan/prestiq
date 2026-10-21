package org.prestiq.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.prestiq.auth.dto.request.LoginRequest;
import org.prestiq.auth.dto.request.RegisterRequest;
import org.prestiq.auth.dto.response.AuthResponse;
import org.prestiq.auth.dto.response.UserDto;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request, String ipAddress, String userAgent, HttpServletResponse response);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent, HttpServletResponse response);

    AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response);

    void logout(String rawRefreshToken, String ipAddress, String userAgent, HttpServletResponse response);

    UserDto getCurrentUser(UUID userId);
}
