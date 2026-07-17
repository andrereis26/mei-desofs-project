package com.desofs.project.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void generateToken_ForUsername_CreatesParseableTokenWithTokenIdAndExpiration() {
        JwtService service = jwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 60000);

        service.validateSecret();
        String token = service.generateToken("alice");
        Claims claims = service.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(service.getExpiration()).isEqualTo(60000);
    }

    @Test
    void generateToken_ForUserDetails_UsesUsername() {
        JwtService service = jwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 60000);
        User user = new User("bob", "pw", java.util.List.of());

        assertThat(service.parseToken(service.generateToken(user)).getSubject()).isEqualTo("bob");
    }

    @Test
    void validateSecret_WhenSecretIsTooShort_RejectsConfiguration() {
        JwtService service = jwtService("short", 60000);

        assertThatThrownBy(service::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private static JwtService jwtService(String secret, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", secret);
        ReflectionTestUtils.setField(service, "expiration", expiration);
        return service;
    }
}
