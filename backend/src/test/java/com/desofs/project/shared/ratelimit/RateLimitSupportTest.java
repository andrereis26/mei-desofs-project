package com.desofs.project.shared.ratelimit;

import com.desofs.project.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitSupportTest {

    @Test
    void normalizer_NormalizesIdentitiesAndDetectsBlankValues() {
        RateLimitKeyNormalizer normalizer = new RateLimitKeyNormalizer();

        assertThat(normalizer.hasText(" user@example.com ")).isTrue();
        assertThat(normalizer.hasText(" ")).isFalse();
        assertThat(normalizer.identity(" User@Example.COM ")).isEqualTo("user@example.com");
        assertThat(normalizer.identity(null)).isEqualTo("unknown");
    }

    @Test
    void guardFallbacks_ThrowRateLimitExceeded() {
        AuthRateLimitGuard guard = new AuthRateLimitGuard();

        guard.checkLoginIdentityLimit("user");
        guard.checkRegisterUsernameLimit("user");
        guard.checkRegisterEmailLimit("user@example.com");

        assertThatThrownBy(() -> guard.onLoginIdentityRateLimitExceeded("user"))
                .isInstanceOf(RateLimitExceededException.class);
        assertThatThrownBy(() -> guard.onRegisterUsernameRateLimitExceeded("user"))
                .isInstanceOf(RateLimitExceededException.class);
        assertThatThrownBy(() -> guard.onRegisterEmailRateLimitExceeded("user@example.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
