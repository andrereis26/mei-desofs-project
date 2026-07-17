package com.desofs.project.infrastructure.tokenrevocation;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TokenRevocationRedisConfigurationTest {

    private final TokenRevocationRedisConfiguration configuration = new TokenRevocationRedisConfiguration();

    @Test
    void redisConnectionFactory_UsesConfiguredStandaloneProperties() {
        TokenRevocationRedisProperties properties = new TokenRevocationRedisProperties();
        properties.setHost("redis.local");
        properties.setPort(6380);
        properties.setUsername("user");
        properties.setPassword("secret");
        properties.setDatabase(2);
        properties.setTimeout(Duration.ofMillis(500));
        properties.setSslEnabled(true);

        RedisConnectionFactory factory = configuration.redisConnectionFactory(properties);

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
    }

    @Test
    void tokenRevocationRedisTemplate_UsesProvidedConnectionFactory() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        StringRedisTemplate template = configuration.tokenRevocationRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
    }
}
