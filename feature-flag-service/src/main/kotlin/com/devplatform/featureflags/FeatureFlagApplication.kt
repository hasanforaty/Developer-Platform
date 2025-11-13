package com.devplatform.featureflags

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class FeatureFlagApplication

fun main(args: Array<String>) {
    runApplication<FeatureFlagApplication>(*args)
}
