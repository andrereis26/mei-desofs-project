package com.desofs.project.infrastructure.tokenrevocation;

import com.desofs.project.user.repositories.TokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.token-revocation", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private final StringRedisTemplate redisTemplate;
    private final TokenRevocationRedisProperties properties;

    @Override
    public void revoke(String tokenId, Instant expiresAt, String username) {
        if (!StringUtils.hasText(tokenId) || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        String value = StringUtils.hasText(username) ? username : "revoked";
        redisTemplate.opsForValue().set(key(tokenId), value, ttl);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return true;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
    }

    private String key(String tokenId) {
        return properties.getKeyPrefix() + tokenId;
    }
}
