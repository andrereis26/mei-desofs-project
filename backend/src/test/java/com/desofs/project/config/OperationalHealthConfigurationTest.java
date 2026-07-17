package com.desofs.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalHealthConfigurationTest {

    private final OperationalHealthConfiguration configuration = new OperationalHealthConfiguration();

    @Test
    void rateLimitRedis_WhenPoolUnavailable_ReturnsUpForConfiguredBackend() {
        ObjectProvider<JedisPool> provider = objectProvider(null);

        Health health = configuration.rateLimitRedis(provider, "in-memory").health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("backend", "in-memory");
    }

    @Test
    void rateLimitRedis_WhenRedisRespondsWithPong_ReturnsUp() {
        JedisPool pool = mock(JedisPool.class);
        Jedis jedis = mock(Jedis.class);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        Health health = configuration.rateLimitRedis(objectProvider(pool), "redis-jedis").health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("backend", "redis-jedis");
    }

    @Test
    void rateLimitRedis_WhenRedisRespondsUnexpectedly_ReturnsDown() {
        JedisPool pool = mock(JedisPool.class);
        Jedis jedis = mock(Jedis.class);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("NOPE");

        Health health = configuration.rateLimitRedis(objectProvider(pool), "redis-jedis").health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("backend", "redis-jedis")
                .containsEntry("ping", "NOPE");
    }

    @Test
    void rateLimitRedis_WhenRedisThrows_ReturnsDown() {
        JedisPool pool = mock(JedisPool.class);
        when(pool.getResource()).thenThrow(new IllegalStateException("redis unavailable"));

        Health health = configuration.rateLimitRedis(objectProvider(pool), "redis-jedis").health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("backend", "redis-jedis");
    }

    @Test
    void tokenRevocationStore_WhenTemplateUnavailable_ReturnsUpForConfiguredBackend() {
        ObjectProvider<StringRedisTemplate> provider = objectProvider(null);

        Health health = configuration.tokenRevocationStore(provider, "in-memory").health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("backend", "in-memory");
    }

    @Test
    void tokenRevocationStore_WhenRedisRespondsWithPong_ReturnsUp() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(anyRedisCallback())).thenReturn("PONG");

        Health health = configuration.tokenRevocationStore(objectProvider(template), "redis").health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("backend", "redis");
    }

    @Test
    void tokenRevocationStore_WhenRedisRespondsUnexpectedly_ReturnsDown() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(anyRedisCallback())).thenReturn("NOPE");

        Health health = configuration.tokenRevocationStore(objectProvider(template), "redis").health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("backend", "redis")
                .containsEntry("ping", "NOPE");
    }

    @Test
    void tokenRevocationStore_WhenRedisThrows_ReturnsDown() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(anyRedisCallback())).thenThrow(new IllegalStateException("redis unavailable"));

        Health health = configuration.tokenRevocationStore(objectProvider(template), "redis").health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("backend", "redis");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static RedisCallback<String> anyRedisCallback() {
        return any(RedisCallback.class);
    }
}
