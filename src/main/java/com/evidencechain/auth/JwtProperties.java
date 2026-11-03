package com.evidencechain.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evidencechain.jwt")
public record JwtProperties(String secretBase64, long expirationSeconds) {
}
