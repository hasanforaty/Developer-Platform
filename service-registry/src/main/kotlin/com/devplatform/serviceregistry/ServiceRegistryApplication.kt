package com.devplatform.serviceregistry

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ServiceRegistryApplication

fun main(args: Array<String>) {
    runApplication<ServiceRegistryApplication>(*args)
}
