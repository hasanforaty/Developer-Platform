plugins {
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common-lib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // HTTP Client
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
