package com.devplatform.featureflags.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Cache configuration for Feature Flag service
 * Uses Redis for distributed caching
 */
@Configuration
@EnableCaching
class CacheConfig {

    /**
     * Configure Redis cache manager with custom TTL settings
     */
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations = mapOf(
            "features" to createCacheConfiguration(Duration.ofMinutes(5)),
            "rules" to createCacheConfiguration(Duration.ofMinutes(5)),
            "evaluations" to createCacheConfiguration(Duration.ofSeconds(30))
        )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(createCacheConfiguration(Duration.ofMinutes(5)))
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    /**
     * Create cache configuration with custom TTL
     */
    private fun createCacheConfiguration(ttl: Duration): RedisCacheConfiguration {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues()
    }
}
