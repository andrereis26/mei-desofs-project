package com.desofs.project.infrastructure.ratelimit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Wires the Redis-backed Bucket4j store used for distributed auth throttling.
 * Startup fails fast when the cache backend is unavailable.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JedisPool.class)
@ConditionalOnProperty(prefix = "bucket4j", name = "cache-to-use", havingValue = "redis-jedis")
@EnableConfigurationProperties(RateLimitRedisProperties.class)
@Slf4j
public class RedisRateLimitConfiguration {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(2);
    private static final String RATE_LIMITER_CLIENT_NAME = "desofs-rate-limiter";

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(JedisPool.class)
    public JedisPool bucket4jJedisPool(RateLimitRedisProperties redisProperties) {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        // Avoid JMX name collisions when multiple pools are present.
        poolConfig.setJmxEnabled(false);

        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(toMillis(redisProperties.getConnectTimeout(), DEFAULT_CONNECT_TIMEOUT))
                .socketTimeoutMillis(toMillis(redisProperties.getTimeout(), DEFAULT_SOCKET_TIMEOUT))
                .database(redisProperties.getDatabase())
                .clientName(RATE_LIMITER_CLIENT_NAME)
                .ssl(redisProperties.isSslEnabled());

        if (StringUtils.hasText(redisProperties.getUsername())) {
            clientConfig.user(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            clientConfig.password(redisProperties.getPassword());
        }

        JedisPool jedisPool = new JedisPool(
                poolConfig,
                new HostAndPort(redisProperties.getHost(), redisProperties.getPort()),
                clientConfig.build()
        );

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        } catch (RuntimeException ex) {
            log.error("Failed to connect to Redis rate-limit store at {}:{}.",
                    redisProperties.getHost(), redisProperties.getPort(), ex);
            jedisPool.close();
            throw new IllegalStateException("Unable to connect to the configured Redis rate-limit store", ex);
        }

        return jedisPool;
    }

    private int toMillis(Duration configuredTimeout, Duration fallbackTimeout) {
        Duration timeout = configuredTimeout != null ? configuredTimeout : fallbackTimeout;
        return Math.toIntExact(timeout.toMillis());
    }
}
