package org.prestiq.application.config;

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
        private String secret = "d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2";
        private String issuer = "prestiq-auth";

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
