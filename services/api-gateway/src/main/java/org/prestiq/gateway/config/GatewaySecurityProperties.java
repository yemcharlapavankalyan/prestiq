package org.prestiq.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "prestiq.security")
public class GatewaySecurityProperties {

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

    public static class Jwt {
        private String secret = "d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2";
        private String issuer = "prestiq-auth";
        private long expirationSeconds = 900;

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

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }
}
