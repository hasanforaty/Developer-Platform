package com.devplatform.serviceregistry.domain.dto

import com.devplatform.serviceregistry.domain.entity.Service
import com.devplatform.serviceregistry.domain.entity.ServiceStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ServiceDTO(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val baseUrl: String,
    val version: String? = null,
    val status: ServiceStatus,
    val instanceCount: Int = 0,
    val healthyInstanceCount: Int = 0,
    val uptime: Double? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: String? = null
) {
    companion object {
        fun from(service: Service, instanceCount: Int = 0, healthyInstanceCount: Int = 0, uptime: Double? = null): ServiceDTO {
            return ServiceDTO(
                id = service.id,
                name = service.name,
                description = service.description,
                baseUrl = service.baseUrl,
                version = service.version,
                status = service.status,
                instanceCount = instanceCount,
                healthyInstanceCount = healthyInstanceCount,
                uptime = uptime,
                createdAt = service.createdAt,
                updatedAt = service.updatedAt,
                createdBy = service.createdBy
            )
        }
    }
}

data class CreateServiceRequest(
    @field:NotBlank(message = "Service name is required")
    @field:Size(min = 3, max = 100, message = "Service name must be between 3 and 100 characters")
    @field:Pattern(regexp = "^[a-z0-9-]+$", message = "Service name must be lowercase alphanumeric with hyphens")
    val name: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:NotBlank(message = "Base URL is required")
    @field:Pattern(regexp = "^https?://.*", message = "Base URL must start with http:// or https://")
    val baseUrl: String,

    @field:Size(max = 50, message = "Version must not exceed 50 characters")
    val version: String? = null
)

data class UpdateServiceRequest(
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @field:Pattern(regexp = "^https?://.*", message = "Base URL must start with http:// or https://")
    val baseUrl: String? = null,

    @field:Size(max = 50, message = "Version must not exceed 50 characters")
    val version: String? = null,

    val status: ServiceStatus? = null
)

data class ServiceSummaryDTO(
    val totalServices: Long,
    val upServices: Long,
    val downServices: Long,
    val degradedServices: Long,
    val totalInstances: Long,
    val healthyInstances: Long
)
