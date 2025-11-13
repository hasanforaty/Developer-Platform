package com.devplatform.serviceregistry.controller

import com.devplatform.serviceregistry.domain.dto.HealthCheckDTO
import com.devplatform.serviceregistry.domain.dto.HealthCheckSummaryDTO
import com.devplatform.serviceregistry.domain.dto.ServiceHealthDTO
import com.devplatform.serviceregistry.service.HealthCheckService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/health")
class HealthCheckController(
    private val healthCheckService: HealthCheckService
) {

    @PostMapping("/check/{instanceId}")
    fun performHealthCheck(@PathVariable instanceId: Long): ResponseEntity<ApiResponse<HealthCheckDTO>> {
        val healthCheck = healthCheckService.performHealthCheck(instanceId)
        return ResponseEntity.ok(ApiResponse.success(healthCheck))
    }

    @PostMapping("/check-all")
    fun performHealthCheckForAll(): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val results = healthCheckService.performHealthCheckForAll()
        return ResponseEntity.ok(ApiResponse.success(results, "Health checks completed"))
    }

    @GetMapping("/history/{instanceId}")
    fun getHealthCheckHistory(
        @PathVariable instanceId: Long,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<List<HealthCheckDTO>>> {
        val history = healthCheckService.getHealthCheckHistory(instanceId, limit)
        return ResponseEntity.ok(ApiResponse.success(history))
    }

    @GetMapping("/summary/{instanceId}")
    fun getHealthCheckSummary(
        @PathVariable instanceId: Long,
        @RequestParam(defaultValue = "7") sinceDays: Int
    ): ResponseEntity<ApiResponse<HealthCheckSummaryDTO>> {
        val summary = healthCheckService.getHealthCheckSummary(instanceId, sinceDays)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }

    @GetMapping("/service/{serviceId}")
    fun getServiceHealth(@PathVariable serviceId: Long): ResponseEntity<ApiResponse<ServiceHealthDTO>> {
        val health = healthCheckService.getServiceHealth(serviceId)
        return ResponseEntity.ok(ApiResponse.success(health))
    }
}
