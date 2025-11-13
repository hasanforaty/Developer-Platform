plugins {
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common-lib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Redis for rate limiting
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
}
