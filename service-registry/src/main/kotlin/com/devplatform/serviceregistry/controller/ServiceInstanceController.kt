package com.devplatform.serviceregistry.controller

import com.devplatform.serviceregistry.domain.dto.*
import com.devplatform.serviceregistry.service.ServiceInstanceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/services/{serviceId}/instances")
class ServiceInstanceController(
    private val serviceInstanceService: ServiceInstanceService
) {

    @PostMapping
    fun registerInstance(
        @PathVariable serviceId: Long,
        @Valid @RequestBody request: RegisterInstanceRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<ServiceInstanceDTO>> {
        val instance = serviceInstanceService.registerInstance(serviceId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(instance, "Instance registered successfully"))
    }

    @GetMapping
    fun getInstances(@PathVariable serviceId: Long): ResponseEntity<ApiResponse<List<ServiceInstanceDTO>>> {
        val instances = serviceInstanceService.getInstancesByService(serviceId)
        return ResponseEntity.ok(ApiResponse.success(instances))
    }

    @GetMapping("/{instanceId}")
    fun getInstance(@PathVariable instanceId: Long): ResponseEntity<ApiResponse<ServiceInstanceDTO>> {
        val instance = serviceInstanceService.getInstance(instanceId)
        return ResponseEntity.ok(ApiResponse.success(instance))
    }

    @PutMapping("/{instanceId}")
    fun updateInstance(
        @PathVariable serviceId: Long,
        @PathVariable instanceId: Long,
        @Valid @RequestBody request: UpdateInstanceRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<ServiceInstanceDTO>> {
        val instance = serviceInstanceService.updateInstance(instanceId, request, userId)
        return ResponseEntity.ok(ApiResponse.success(instance, "Instance updated successfully"))
    }

    @DeleteMapping("/{instanceId}")
    fun deregisterInstance(
        @PathVariable serviceId: Long,
        @PathVariable instanceId: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        serviceInstanceService.deregisterInstance(instanceId, userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Instance deregistered successfully"))
    }

    @PostMapping("/{instanceId}/heartbeat")
    fun sendHeartbeat(
        @PathVariable serviceId: Long,
        @PathVariable instanceId: Long,
        @Valid @RequestBody request: HeartbeatRequest
    ): ResponseEntity<ApiResponse<ServiceInstanceDTO>> {
        // Find instance by DB id first
        val instance = serviceInstanceService.getInstance(instanceId)
        val updated = serviceInstanceService.sendHeartbeat(instance.instanceId, request)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }
}

@RestController
@RequestMapping("/api/v1/instances")
class InstanceController(
    private val serviceInstanceService: ServiceInstanceService
) {

    @PostMapping("/heartbeat/{instanceId}")
    fun sendHeartbeat(
        @PathVariable instanceId: String,
        @Valid @RequestBody request: HeartbeatRequest
    ): ResponseEntity<ApiResponse<ServiceInstanceDTO>> {
        val instance = serviceInstanceService.sendHeartbeat(instanceId, request)
        return ResponseEntity.ok(ApiResponse.success(instance))
    }
}
