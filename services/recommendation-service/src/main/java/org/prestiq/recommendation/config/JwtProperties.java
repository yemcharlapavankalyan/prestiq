package org.prestiq.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prestiq.security")
public class JwtProperties {

    private Jwt jwt = new Jwt();
    private String internalServiceKey = "prestiq-internal-dev-secret-key-2026";

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public String getInternalServiceKey() {
        return internalServiceKey;
    }

    public void setInternalServiceKey(String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
    }

    public String getSecret() {
        return jwt != null ? jwt.getSecret() : null;
    }

    public String getIssuer() {
        return jwt != null ? jwt.getIssuer() : null;
    }

    public static class Jwt {
        private String secret = "0123456789012345678901234567890123456789012345678901234567890123";
        private String issuer = "prestiq-auth-service";

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
    }
}
