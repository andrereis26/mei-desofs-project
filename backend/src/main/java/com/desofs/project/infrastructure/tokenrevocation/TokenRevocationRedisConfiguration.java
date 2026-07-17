package com.desofs.project.infrastructure.tokenrevocation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.token-revocation", name = "store", havingValue = "redis", matchIfMissing = true)
@EnableConfigurationProperties(TokenRevocationRedisProperties.class)
public class TokenRevocationRedisConfiguration {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(TokenRevocationRedisProperties properties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(properties.getHost());
        standalone.setPort(properties.getPort());
        standalone.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getUsername())) {
            standalone.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            standalone.setPassword(properties.getPassword());
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig =
                LettuceClientConfiguration.builder().commandTimeout(properties.getTimeout());
        if (properties.isSslEnabled()) {
            clientConfig.useSsl();
        }

        return new LettuceConnectionFactory(standalone, clientConfig.build());
    }

    @Bean
    public StringRedisTemplate tokenRevocationRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
