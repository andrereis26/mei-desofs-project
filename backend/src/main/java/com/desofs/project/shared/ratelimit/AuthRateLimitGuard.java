package com.desofs.project.shared.ratelimit;

import com.desofs.project.shared.exception.RateLimitExceededException;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import org.springframework.stereotype.Service;

/**
 * Enforces auth-specific identity limits before credential verification or registration logic runs.
 * All failures are converted into a deterministic 429 response via the shared exception handler.
 */
@Service
public class AuthRateLimitGuard {

    @RateLimiting(
            name = "auth-login-identity",
            cacheKey = "@rateLimitKeyNormalizer.identity(#username)",
            executeCondition = "@rateLimitKeyNormalizer.hasText(#username)",
            fallbackMethodName = "onLoginIdentityRateLimitExceeded"
    )
    public void checkLoginIdentityLimit(String username) {
    }

    @RateLimiting(
            name = "auth-register-username",
            cacheKey = "@rateLimitKeyNormalizer.identity(#username)",
            executeCondition = "@rateLimitKeyNormalizer.hasText(#username)",
            fallbackMethodName = "onRegisterUsernameRateLimitExceeded"
    )
    public void checkRegisterUsernameLimit(String username) {
    }

    @RateLimiting(
            name = "auth-register-email",
            cacheKey = "@rateLimitKeyNormalizer.identity(#email)",
            executeCondition = "@rateLimitKeyNormalizer.hasText(#email)",
            fallbackMethodName = "onRegisterEmailRateLimitExceeded"
    )
    public void checkRegisterEmailLimit(String email) {
    }

    public void onLoginIdentityRateLimitExceeded(String username) {
        throw new RateLimitExceededException();
    }

    public void onRegisterUsernameRateLimitExceeded(String username) {
        throw new RateLimitExceededException();
    }

    public void onRegisterEmailRateLimitExceeded(String email) {
        throw new RateLimitExceededException();
    }
}
