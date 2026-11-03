package com.evidencechain.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {

    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> configuration.jwtSecretKey(new JwtProperties("", 3600)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be set");
    }

    @Test
    void rejectsDecodedSecretShorterThan32Bytes() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatThrownBy(() -> configuration.jwtSecretKey(new JwtProperties(shortKey, 3600)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void acceptsRandomSecretOf32Bytes() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        SecretKey key = configuration.jwtSecretKey(
                new JwtProperties(Base64.getEncoder().encodeToString(bytes), 3600));

        assertThat(key.getEncoded()).hasSize(32);
    }
}
