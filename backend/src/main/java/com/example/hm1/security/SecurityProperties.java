package com.example.hm1.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {
    private String secret;
    private long expirationMillis;

    public String getSecret() {
        return secret;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}