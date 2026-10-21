package org.prestiq.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${prestiq.security.jwt.secret:d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2}")
    private String secret;

    @Value("${prestiq.security.jwt.issuer:prestiq-auth}")
    private String issuer;

    @Value("${prestiq.security.internal-service-key:prestiq-internal-dev-secret-key-2026}")
    private String internalServiceKey;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getInternalServiceKey() {
        return internalServiceKey;
    }

    public void setInternalServiceKey(String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
    }
}
