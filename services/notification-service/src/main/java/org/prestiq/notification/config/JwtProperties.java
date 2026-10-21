package org.prestiq.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${prestiq.security.jwt.secret:0123456789012345678901234567890123456789012345678901234567890123}")
    private String secret;

    @Value("${prestiq.security.jwt.issuer:prestiq-auth-service}")
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
