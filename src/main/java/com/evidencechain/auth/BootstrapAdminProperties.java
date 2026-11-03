package com.evidencechain.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evidencechain.bootstrap-admin")
public record BootstrapAdminProperties(String username, String password) {
}
