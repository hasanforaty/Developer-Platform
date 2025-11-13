package com.devplatform.serviceregistry.controller

import com.devplatform.serviceregistry.domain.dto.*
import com.devplatform.serviceregistry.domain.entity.ServiceStatus
import com.devplatform.serviceregistry.service.ServiceRegistryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/services")
class ServiceController(
    private val serviceRegistryService: ServiceRegistryService
) {

    @PostMapping
    fun registerService(
        @Valid @RequestBody request: CreateServiceRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<ServiceDTO>> {
        val service = serviceRegistryService.registerService(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(service, "Service registered successfully"))
    }

    @GetMapping
    fun getAllServices(): ResponseEntity<ApiResponse<List<ServiceDTO>>> {
        val services = serviceRegistryService.getAllServices()
        return ResponseEntity.ok(ApiResponse.success(services))
    }

    @GetMapping("/{id}")
    fun getService(@PathVariable id: Long): ResponseEntity<ApiResponse<ServiceDTO>> {
        val service = serviceRegistryService.getService(id)
        return ResponseEntity.ok(ApiResponse.success(service))
    }

    @GetMapping("/name/{name}")
    fun getServiceByName(@PathVariable name: String): ResponseEntity<ApiResponse<ServiceDTO>> {
        val service = serviceRegistryService.getServiceByName(name)
        return ResponseEntity.ok(ApiResponse.success(service))
    }

    @GetMapping("/search")
    fun searchServices(@RequestParam("q") searchTerm: String): ResponseEntity<ApiResponse<List<ServiceDTO>>> {
        val services = serviceRegistryService.searchServices(searchTerm)
        return ResponseEntity.ok(ApiResponse.success(services))
    }

    @PutMapping("/{id}")
    fun updateService(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateServiceRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<ServiceDTO>> {
        val service = serviceRegistryService.updateService(id, request, userId)
        return ResponseEntity.ok(ApiResponse.success(service, "Service updated successfully"))
    }

    @DeleteMapping("/{id}")
    fun deleteService(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        serviceRegistryService.deleteService(id, userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Service deleted successfully"))
    }

    @PatchMapping("/{id}/status")
    fun updateServiceStatus(
        @PathVariable id: Long,
        @RequestParam status: ServiceStatus,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        serviceRegistryService.updateServiceStatus(id, status, userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Service status updated"))
    }

    @GetMapping("/summary")
    fun getServiceSummary(): ResponseEntity<ApiResponse<ServiceSummaryDTO>> {
        val summary = serviceRegistryService.getServiceSummary()
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
}
