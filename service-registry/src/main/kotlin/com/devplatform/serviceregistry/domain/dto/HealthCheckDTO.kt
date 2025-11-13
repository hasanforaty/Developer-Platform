package com.devplatform.serviceregistry.domain.dto

import com.devplatform.serviceregistry.domain.entity.HealthCheck
import com.devplatform.serviceregistry.domain.entity.HealthStatus
import java.time.LocalDateTime

data class HealthCheckDTO(
    val id: Long? = null,
    val instanceId: Long,
    val timestamp: LocalDateTime,
    val status: HealthStatus,
    val responseTimeMs: Int? = null,
    val errorMessage: String? = null,
    val details: Map<String, Any>? = null
) {
    companion object {
        fun from(healthCheck: HealthCheck): HealthCheckDTO {
            return HealthCheckDTO(
                id = healthCheck.id,
                instanceId = healthCheck.instance?.id ?: 0,
                timestamp = healthCheck.timestamp,
                status = healthCheck.status,
                responseTimeMs = healthCheck.responseTimeMs,
                errorMessage = healthCheck.errorMessage,
                details = healthCheck.details
            )
        }
    }
}

data class HealthCheckSummaryDTO(
    val instanceId: Long,
    val totalChecks: Long,
    val healthyChecks: Long,
    val unhealthyChecks: Long,
    val averageResponseTime: Double? = null,
    val uptime: Double? = null,
    val lastCheckTime: LocalDateTime? = null,
    val lastCheckStatus: HealthStatus? = null
)

data class ServiceHealthDTO(
    val serviceId: Long,
    val serviceName: String,
    val overallStatus: String,
    val totalInstances: Int,
    val healthyInstances: Int,
    val degradedInstances: Int,
    val downInstances: Int,
    val instances: List<InstanceHealthDTO>
)

data class InstanceHealthDTO(
    val instanceId: Long,
    val instanceName: String,
    val host: String,
    val port: Int,
    val status: InstanceStatus,
    val lastHeartbeat: LocalDateTime?,
    val recentChecks: List<HealthCheckDTO>
)
