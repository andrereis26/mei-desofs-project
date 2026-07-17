package com.desofs.project.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitConfigurationTest {

    @Test
    void bucket4jJedisPool_WhenRedisAvailable_ReturnsConfiguredPool() {
        RateLimitRedisProperties properties = new RateLimitRedisProperties();
        properties.setHost("redis.example.test");
        properties.setPort(6380);
        properties.setUsername("user");
        properties.setPassword("password");
        properties.setDatabase(2);
        properties.setConnectTimeout(Duration.ofMillis(25));
        properties.setTimeout(Duration.ofMillis(50));
        properties.setSslEnabled(true);

        Jedis jedis = mock(Jedis.class);
        when(jedis.ping()).thenReturn("PONG");

        try (MockedConstruction<JedisPool> mockedPools = mockConstruction(
                JedisPool.class,
                (pool, context) -> when(pool.getResource()).thenReturn(jedis)
        )) {
            JedisPool result = new RedisRateLimitConfiguration().bucket4jJedisPool(properties);

            assertThat(result).isSameAs(mockedPools.constructed().get(0));
            assertThat(mockedPools.constructed()).hasSize(1);
            verify(jedis).ping();
            verify(result, never()).close();
        }
    }

    @Test
    void bucket4jJedisPool_WhenOptionalPropertiesAreBlank_UsesDefaultsAndReturnsPool() {
        RateLimitRedisProperties properties = new RateLimitRedisProperties();
        properties.setHost("redis.example.test");
        properties.setPort(6380);
        properties.setUsername(" ");
        properties.setPassword("");
        properties.setConnectTimeout(null);
        properties.setTimeout(null);

        Jedis jedis = mock(Jedis.class);
        when(jedis.ping()).thenReturn("PONG");

        try (MockedConstruction<JedisPool> mockedPools = mockConstruction(
                JedisPool.class,
                (pool, context) -> when(pool.getResource()).thenReturn(jedis)
        )) {
            JedisPool result = new RedisRateLimitConfiguration().bucket4jJedisPool(properties);

            assertThat(result).isSameAs(mockedPools.constructed().get(0));
            verify(jedis).ping();
            verify(result, never()).close();
        }
    }

    @Test
    void bucket4jJedisPool_WhenRedisUnavailable_FailsFastAndClosesPool() {
        RateLimitRedisProperties properties = new RateLimitRedisProperties();
        properties.setHost("redis.example.test");
        properties.setPort(6380);

        RuntimeException connectionFailure = new RuntimeException("redis unavailable");

        try (MockedConstruction<JedisPool> mockedPools = mockConstruction(
                JedisPool.class,
                (pool, context) -> when(pool.getResource()).thenThrow(connectionFailure)
        )) {
            assertThatThrownBy(() -> new RedisRateLimitConfiguration().bucket4jJedisPool(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unable to connect")
                    .hasCause(connectionFailure);

            verify(mockedPools.constructed().get(0)).close();
        }
    }
}
