package com.devplatform.featureflags.domain.enums

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
    TEST;

    companion object {
        fun fromString(value: String): Environment {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid environment: $value")
        }
    }
}
