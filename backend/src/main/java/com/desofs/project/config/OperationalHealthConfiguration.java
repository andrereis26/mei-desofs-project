package com.desofs.project.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPool;
import lombok.extern.slf4j.Slf4j;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class OperationalHealthConfiguration {

    @Bean
    public HealthIndicator rateLimitRedis(
            ObjectProvider<JedisPool> jedisPoolProvider,
            @Value("${bucket4j.cache-to-use:redis-jedis}") String cacheBackend
    ) {
        return () -> {
            JedisPool jedisPool = jedisPoolProvider.getIfAvailable();
            if (jedisPool == null || !"redis-jedis".equalsIgnoreCase(cacheBackend)) {
                return Health.up().withDetail("backend", cacheBackend).build();
            }

            try (var jedis = jedisPool.getResource()) {
                String ping = jedis.ping();
                if ("PONG".equalsIgnoreCase(ping)) {
                    return Health.up().withDetail("backend", "redis-jedis").build();
                }
                log.warn("Rate-limit Redis health check returned unexpected ping response: {}", ping);
                return Health.down().withDetail("backend", "redis-jedis").withDetail("ping", ping).build();
            } catch (RuntimeException ex) {
                log.warn("Rate-limit Redis health check failed.", ex);
                return Health.down(ex).withDetail("backend", "redis-jedis").build();
            }
        };
    }

    @Bean
    public HealthIndicator tokenRevocationStore(
            ObjectProvider<StringRedisTemplate> tokenRevocationRedisTemplateProvider,
            @Value("${app.token-revocation.store:redis}") String tokenRevocationStore
    ) {
        return () -> {
            StringRedisTemplate tokenRevocationRedisTemplate = tokenRevocationRedisTemplateProvider.getIfAvailable();
            if (tokenRevocationRedisTemplate == null || "in-memory".equalsIgnoreCase(tokenRevocationStore)) {
                return Health.up().withDetail("backend", tokenRevocationStore).build();
            }

            try {
                String ping = tokenRevocationRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
                if ("PONG".equalsIgnoreCase(ping)) {
                    return Health.up().withDetail("backend", "redis").build();
                }
                log.warn("Token revocation Redis health check returned unexpected ping response: {}", ping);
                return Health.down().withDetail("backend", "redis").withDetail("ping", ping).build();
            } catch (RuntimeException ex) {
                log.warn("Token revocation Redis health check failed.", ex);
                return Health.down(ex).withDetail("backend", "redis").build();
            }
        };
    }
}
