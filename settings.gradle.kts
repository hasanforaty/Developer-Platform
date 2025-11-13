pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "developer-platform"

include(
    "gateway",
    "service-registry",
    "log-aggregation",
    "feature-flag-service",
    "rate-limiter",
    "ab-testing-service",
    "common-lib"
)
