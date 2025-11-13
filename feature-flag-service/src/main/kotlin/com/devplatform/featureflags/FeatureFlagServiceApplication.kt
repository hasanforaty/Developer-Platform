package com.devplatform.featureflags

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Feature Flag Service Application
 * Provides feature flag management with rule-based evaluation, targeting, and rollout
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@EnableAsync
class FeatureFlagServiceApplication

fun main(args: Array<String>) {
    runApplication<FeatureFlagServiceApplication>(*args)
}
