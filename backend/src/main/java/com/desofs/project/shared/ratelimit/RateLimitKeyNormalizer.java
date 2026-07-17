package com.desofs.project.shared.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component("rateLimitKeyNormalizer")
public class RateLimitKeyNormalizer {

    public boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    public String identity(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
