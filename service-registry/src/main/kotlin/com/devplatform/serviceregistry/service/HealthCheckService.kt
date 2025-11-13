package com.devplatform.serviceregistry.service

import com.devplatform.serviceregistry.domain.dto.HealthCheckDTO
import com.devplatform.serviceregistry.domain.dto.HealthCheckSummaryDTO
import com.devplatform.serviceregistry.domain.dto.ServiceHealthDTO
import com.devplatform.serviceregistry.domain.dto.InstanceHealthDTO
import com.devplatform.serviceregistry.domain.entity.HealthCheck
import com.devplatform.serviceregistry.domain.entity.HealthStatus
import com.devplatform.serviceregistry.domain.entity.InstanceStatus
import com.devplatform.serviceregistry.domain.entity.ServiceStatus
import com.devplatform.serviceregistry.repository.HealthCheckRepository
import com.devplatform.serviceregistry.repository.ServiceInstanceRepository
import com.devplatform.serviceregistry.repository.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional
class HealthCheckService(
    private val healthCheckRepository: HealthCheckRepository,
    private val serviceInstanceRepository: ServiceInstanceRepository,
    private val serviceRepository: ServiceRepository,
    private val webClient: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(HealthCheckService::class.java)

    companion object {
        private const val DEFAULT_HEALTH_ENDPOINT = "/actuator/health"
        private val DEFAULT_TIMEOUT = Duration.ofSeconds(5)
    }

    fun performHealthCheck(instanceId: Long): HealthCheckDTO {
        logger.debug("Performing health check for instance: $instanceId")

        val instance = serviceInstanceRepository.findById(instanceId)
            .orElseThrow { IllegalArgumentException("Instance not found: $instanceId") }

        val healthCheckUrl = buildHealthCheckUrl(instance.host, instance.port)
        val startTime = System.currentTimeMillis()

        try {
            val client = webClient.baseUrl(healthCheckUrl).build()

            val response = client.get()
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorResume { error ->
                    logger.debug("Health check failed for ${instance.instanceId}: ${error.message}")
                    Mono.just("ERROR")
                }
                .block()

            val responseTime = (System.currentTimeMillis() - startTime).toInt()
            val status = if (response != "ERROR") HealthStatus.HEALTHY else HealthStatus.UNHEALTHY

            val healthCheck = HealthCheck(
                status = status,
                responseTimeMs = responseTime,
                details = mapOf("response" to (response ?: ""))
            )

            instance.addHealthCheck(healthCheck)

            // Update instance status based on health check
            if (status == HealthStatus.HEALTHY && instance.status != InstanceStatus.UP) {
                instance.markAsUp()
            } else if (status != HealthStatus.HEALTHY && instance.status == InstanceStatus.UP) {
                instance.markAsDown()
            }

            instance.updateHeartbeat()
            serviceInstanceRepository.save(instance)
            val saved = healthCheckRepository.save(healthCheck)

            logger.debug("Health check completed for ${instance.instanceId}: $status (${responseTime}ms)")

            return HealthCheckDTO.from(saved)

        } catch (e: WebClientResponseException) {
            val responseTime = (System.currentTimeMillis() - startTime).toInt()
            val healthCheck = HealthCheck(
                status = HealthStatus.UNHEALTHY,
                responseTimeMs = responseTime,
                errorMessage = "HTTP ${e.statusCode}: ${e.message}"
            )

            instance.addHealthCheck(healthCheck)
            instance.markAsDown()
            serviceInstanceRepository.save(instance)
            val saved = healthCheckRepository.save(healthCheck)

            logger.warn("Health check failed for ${instance.instanceId}: HTTP ${e.statusCode}")

            return HealthCheckDTO.from(saved)

        } catch (e: Exception) {
            val responseTime = (System.currentTimeMillis() - startTime).toInt()
            val status = when {
                responseTime >= DEFAULT_TIMEOUT.toMillis() -> HealthStatus.TIMEOUT
                else -> HealthStatus.ERROR
            }

            val healthCheck = HealthCheck(
                status = status,
                responseTimeMs = responseTime,
                errorMessage = e.message ?: "Unknown error"
            )

            instance.addHealthCheck(healthCheck)
            instance.markAsDown()
            serviceInstanceRepository.save(instance)
            val saved = healthCheckRepository.save(healthCheck)

            logger.error("Health check error for ${instance.instanceId}: ${e.message}")

            return HealthCheckDTO.from(saved)
        }
    }

    fun performHealthCheckForAll(): Map<String, Int> {
        logger.info("Performing health checks for all instances")

        val instances = serviceInstanceRepository.findAll()
        var healthy = 0
        var unhealthy = 0
        var error = 0

        instances.forEach { instance ->
            try {
                val result = performHealthCheck(instance.id!!)
                when (result.status) {
                    HealthStatus.HEALTHY -> healthy++
                    HealthStatus.UNHEALTHY -> unhealthy++
                    else -> error++
                }
            } catch (e: Exception) {
                logger.error("Failed to perform health check for instance ${instance.id}", e)
                error++
            }
        }

        updateServiceStatuses()

        logger.info("Health checks completed: $healthy healthy, $unhealthy unhealthy, $error errors")

        return mapOf(
            "total" to instances.size,
            "healthy" to healthy,
            "unhealthy" to unhealthy,
            "error" to error
        )
    }

    @Transactional(readOnly = true)
    fun getHealthCheckHistory(instanceId: Long, limit: Int = 10): List<HealthCheckDTO> {
        val checks = healthCheckRepository.findByInstanceIdOrderByTimestampDesc(
            instanceId,
            PageRequest.of(0, limit)
        )
        return checks.map { HealthCheckDTO.from(it) }
    }

    @Transactional(readOnly = true)
    fun getHealthCheckSummary(instanceId: Long, sinceDays: Int = 7): HealthCheckSummaryDTO {
        val since = LocalDateTime.now().minusDays(sinceDays.toLong())

        val recentChecks = healthCheckRepository.findRecentByInstanceId(instanceId, since)
        val totalChecks = recentChecks.size.toLong()
        val healthyChecks = recentChecks.count { it.status == HealthStatus.HEALTHY }.toLong()
        val unhealthyChecks = totalChecks - healthyChecks

        val avgResponseTime = healthCheckRepository.calculateAverageResponseTime(instanceId, since)
        val uptime = healthCheckRepository.calculateUptimePercentage(instanceId, since)

        val latestCheck = healthCheckRepository.findLatestByInstanceId(instanceId)

        return HealthCheckSummaryDTO(
            instanceId = instanceId,
            totalChecks = totalChecks,
            healthyChecks = healthyChecks,
            unhealthyChecks = unhealthyChecks,
            averageResponseTime = avgResponseTime,
            uptime = uptime,
            lastCheckTime = latestCheck?.timestamp,
            lastCheckStatus = latestCheck?.status
        )
    }

    @Transactional(readOnly = true)
    fun getServiceHealth(serviceId: Long): ServiceHealthDTO {
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { IllegalArgumentException("Service not found: $serviceId") }

        val instances = serviceInstanceRepository.findByServiceId(serviceId)

        val instanceHealthList = instances.map { instance ->
            val recentChecks = healthCheckRepository.findByInstanceIdOrderByTimestampDesc(
                instance.id!!,
                PageRequest.of(0, 5)
            ).map { HealthCheckDTO.from(it) }

            InstanceHealthDTO(
                instanceId = instance.id,
                instanceName = instance.instanceId,
                host = instance.host,
                port = instance.port,
                status = instance.status,
                lastHeartbeat = instance.lastHeartbeat,
                recentChecks = recentChecks
            )
        }

        val healthyCount = instances.count { it.status == InstanceStatus.UP }
        val downCount = instances.count { it.status == InstanceStatus.DOWN }
        val degradedCount = instances.count { it.status == InstanceStatus.STARTING }

        val overallStatus = when {
            instances.isEmpty() -> "NO_INSTANCES"
            healthyCount == instances.size -> "HEALTHY"
            healthyCount > 0 -> "DEGRADED"
            else -> "DOWN"
        }

        return ServiceHealthDTO(
            serviceId = serviceId,
            serviceName = service.name,
            overallStatus = overallStatus,
            totalInstances = instances.size,
            healthyInstances = healthyCount,
            degradedInstances = degradedCount,
            downInstances = downCount,
            instances = instanceHealthList
        )
    }

    fun cleanupOldHealthChecks(retentionDays: Int = 30): Int {
        val threshold = LocalDateTime.now().minusDays(retentionDays.toLong())
        val deleted = healthCheckRepository.deleteOlderThan(threshold)
        logger.info("Cleaned up $deleted old health check records (older than $retentionDays days)")
        return deleted
    }

    private fun updateServiceStatuses() {
        val services = serviceRepository.findByDeletedAtIsNull()

        services.forEach { service ->
            val instances = serviceInstanceRepository.findByServiceId(service.id!!)

            val newStatus = when {
                instances.isEmpty() -> ServiceStatus.UNKNOWN
                instances.all { it.status == InstanceStatus.UP } -> ServiceStatus.UP
                instances.all { it.status == InstanceStatus.DOWN } -> ServiceStatus.DOWN
                instances.any { it.status == InstanceStatus.UP } -> ServiceStatus.DEGRADED
                else -> ServiceStatus.UNKNOWN
            }

            if (service.status != newStatus) {
                logger.info("Updating service ${service.name} status: ${service.status} -> $newStatus")
                service.status = newStatus
                serviceRepository.save(service)
            }
        }
    }

    private fun buildHealthCheckUrl(host: String, port: Int): String {
        val protocol = if (port == 443) "https" else "http"
        return "$protocol://$host:$port$DEFAULT_HEALTH_ENDPOINT"
    }
}
