package com.desofs.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class RateLimitTestCacheConfiguration {

    private static final String CACHE_PROVIDER = "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider";
    private static final String RATE_LIMIT_BUCKET_CACHE = "rate-limit-buckets";

    @Bean(destroyMethod = "close")
    public CacheManager rateLimitCacheManager() {
        CacheManager cacheManager = Caching.getCachingProvider(CACHE_PROVIDER).getCacheManager();
        if (cacheManager.getCache(RATE_LIMIT_BUCKET_CACHE) == null) {
            MutableConfiguration<String, byte[]> configuration = new MutableConfiguration<String, byte[]>()
                    .setTypes(String.class, byte[].class)
                    .setStoreByValue(false);
            cacheManager.createCache(RATE_LIMIT_BUCKET_CACHE, configuration);
        }
        return cacheManager;
    }
}
