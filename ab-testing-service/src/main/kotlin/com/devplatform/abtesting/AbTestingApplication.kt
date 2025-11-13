package com.devplatform.abtesting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AbTestingApplication

fun main(args: Array<String>) {
    runApplication<AbTestingApplication>(*args)
}
