package com.desofs.project.infrastructure.tokenrevocation;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenRevocationStoreTest {

    @Test
    void inMemoryStore_TracksActiveRevocationsAndExpiresOldEntries() {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

        store.revoke("active", Instant.now().plusSeconds(60), "alice");
        store.revoke("expired", Instant.now().minusSeconds(1), "alice");

        assertThat(store.isRevoked(null)).isTrue();
        assertThat(store.isRevoked("active")).isTrue();
        assertThat(store.isRevoked("expired")).isFalse();
        assertThat(store.isRevoked("missing")).isFalse();
    }

    @Test
    void inMemoryStore_IgnoresInvalidRevocations() {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

        store.revoke(null, Instant.now().plusSeconds(60), "alice");
        store.revoke("token", null, "alice");

        assertThat(store.isRevoked("token")).isFalse();
    }

    @Test
    void redisStore_SetsPrefixedKeyWithUsernameAndTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        TokenRevocationRedisProperties properties = new TokenRevocationRedisProperties();
        properties.setKeyPrefix("revoked:");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisTokenRevocationStore store = new RedisTokenRevocationStore(redisTemplate, properties);

        store.revoke("token-1", Instant.now().plusSeconds(60), "alice");

        verify(valueOperations).set(eq("revoked:token-1"), eq("alice"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void redisStore_IgnoresInvalidOrExpiredRevocationsAndChecksKeys() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        TokenRevocationRedisProperties properties = new TokenRevocationRedisProperties();
        RedisTokenRevocationStore store = new RedisTokenRevocationStore(redisTemplate, properties);
        when(redisTemplate.hasKey("revoked-token:token-1")).thenReturn(true);

        store.revoke(" ", Instant.now().plusSeconds(60), "alice");
        store.revoke("expired", Instant.now().minusSeconds(1), "alice");

        verify(redisTemplate, never()).opsForValue();
        assertThat(store.isRevoked(" ")).isTrue();
        assertThat(store.isRevoked("token-1")).isTrue();
    }
}
