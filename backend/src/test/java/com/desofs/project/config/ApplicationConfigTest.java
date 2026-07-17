package com.desofs.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigTest {

    @Test
    void passwordEncoder_ReturnsBcryptEncoder() {
        PasswordEncoder encoder = new ApplicationConfig().passwordEncoder();

        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }
}
