package com.devplatform.serviceregistry.scheduler

import com.devplatform.serviceregistry.service.HealthCheckService
import com.devplatform.serviceregistry.service.ServiceInstanceService
import com.devplatform.serviceregistry.service.ServiceRegistryService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableConfigurationProperties(HealthCheckSchedulerProperties::class)
class HealthCheckScheduler(
    private val healthCheckService: HealthCheckService,
    private val serviceInstanceService: ServiceInstanceService,
    private val serviceRegistryService: ServiceRegistryService,
    private val properties: HealthCheckSchedulerProperties
) {
    private val logger = LoggerFactory.getLogger(HealthCheckScheduler::class.java)

    @Scheduled(fixedDelayString = "\${service-registry.health-check.interval:PT30S}")
    fun performScheduledHealthChecks() {
        logger.debug("Starting scheduled health checks")

        try {
            val results = healthCheckService.performHealthCheckForAll()
            logger.info("Scheduled health checks completed: $results")
        } catch (e: Exception) {
            logger.error("Error during scheduled health checks", e)
        }
    }

    @Scheduled(fixedDelayString = "\${service-registry.health-check.stale-check-interval:PT1M}")
    fun checkStaleInstances() {
        logger.debug("Checking for stale instances")

        try {
            val markedDown = serviceInstanceService.checkStaleInstances(5)
            if (markedDown > 0) {
                logger.info("Marked $markedDown stale instances as DOWN")
            }
        } catch (e: Exception) {
            logger.error("Error checking stale instances", e)
        }
    }

    @Scheduled(cron = "\${service-registry.cleanup.cron:0 0 2 * * *}")
    fun cleanupOldData() {
        logger.info("Starting scheduled cleanup of old data")

        try {
            // Clean up old health checks (older than 30 days)
            val deletedHealthChecks = healthCheckService.cleanupOldHealthChecks(30)
            logger.info("Cleanup completed: $deletedHealthChecks health checks deleted")
        } catch (e: Exception) {
            logger.error("Error during cleanup", e)
        }
    }
}

@ConfigurationProperties(prefix = "service-registry.health-check")
data class HealthCheckSchedulerProperties(
    val interval: String = "PT30S",
    val staleCheckInterval: String = "PT1M",
    val timeout: String = "PT5S"
)
