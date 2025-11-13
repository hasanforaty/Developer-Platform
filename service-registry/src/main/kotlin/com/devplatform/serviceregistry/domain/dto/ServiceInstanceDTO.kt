package com.devplatform.serviceregistry.domain.dto

import com.devplatform.serviceregistry.domain.entity.InstanceStatus
import com.devplatform.serviceregistry.domain.entity.ServiceInstance
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class ServiceInstanceDTO(
    val id: Long? = null,
    val serviceId: Long,
    val serviceName: String,
    val instanceId: String,
    val host: String,
    val port: Int,
    val status: InstanceStatus,
    val metadata: Map<String, Any>? = null,
    val lastHeartbeat: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val recentHealthChecks: List<HealthCheckDTO>? = null
) {
    companion object {
        fun from(instance: ServiceInstance, includeHealthChecks: Boolean = false): ServiceInstanceDTO {
            return ServiceInstanceDTO(
                id = instance.id,
                serviceId = instance.service?.id ?: 0,
                serviceName = instance.service?.name ?: "",
                instanceId = instance.instanceId,
                host = instance.host,
                port = instance.port,
                status = instance.status,
                metadata = instance.metadata,
                lastHeartbeat = instance.lastHeartbeat,
                createdAt = instance.createdAt,
                updatedAt = instance.updatedAt,
                recentHealthChecks = if (includeHealthChecks) {
                    instance.getHealthCheckHistory(5).map { HealthCheckDTO.from(it) }
                } else null
            )
        }
    }
}

data class RegisterInstanceRequest(
    @field:NotBlank(message = "Instance ID is required")
    @field:Size(min = 3, max = 100, message = "Instance ID must be between 3 and 100 characters")
    val instanceId: String,

    @field:NotBlank(message = "Host is required")
    val host: String,

    @field:Min(value = 1, message = "Port must be greater than 0")
    @field:Max(value = 65535, message = "Port must be less than 65536")
    val port: Int,

    val metadata: Map<String, Any>? = null
)

data class UpdateInstanceRequest(
    val status: InstanceStatus? = null,
    val metadata: Map<String, Any>? = null
)

data class HeartbeatRequest(
    val status: InstanceStatus = InstanceStatus.UP,
    val metadata: Map<String, Any>? = null
)
