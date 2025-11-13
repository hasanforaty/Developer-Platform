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

    // Async processing
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Full-text search support
    implementation("org.hibernate.search:hibernate-search-mapper-orm:7.0.0.Final")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:7.0.0.Final")
}
