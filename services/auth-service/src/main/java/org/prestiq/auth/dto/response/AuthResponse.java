package org.prestiq.auth.dto.response;

public class AuthResponse {

    private String tokenType = "Bearer";
    private String accessToken;
    private long expiresInSeconds;
    private UserDto user;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, long expiresInSeconds, UserDto user) {
        this.tokenType = "Bearer";
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
