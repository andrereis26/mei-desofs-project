package com.desofs.project.infrastructure.ratelimit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "app.rate-limit.redis")
public class RateLimitRedisProperties {

    @NotBlank
    private String host = "localhost";

    @Min(1)
    private int port = 6379;

    private String username;

    private String password;

    private int database = 0;

    private Duration timeout = Duration.ofSeconds(2);

    private Duration connectTimeout = Duration.ofSeconds(1);

    private boolean sslEnabled;
}
