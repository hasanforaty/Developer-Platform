plugins {
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common-lib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Statistical analysis
    implementation("org.apache.commons:commons-math3:3.6.1")
}
