package com.devplatform.serviceregistry.service

import com.devplatform.serviceregistry.domain.dto.*
import com.devplatform.serviceregistry.domain.entity.InstanceStatus
import com.devplatform.serviceregistry.domain.entity.Service
import com.devplatform.serviceregistry.domain.entity.ServiceStatus
import com.devplatform.serviceregistry.exception.DuplicateServiceException
import com.devplatform.serviceregistry.exception.ServiceNotFoundException
import com.devplatform.serviceregistry.repository.ServiceInstanceRepository
import com.devplatform.serviceregistry.repository.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service as SpringService
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringService
@Transactional
class ServiceRegistryService(
    private val serviceRepository: ServiceRepository,
    private val serviceInstanceRepository: ServiceInstanceRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(ServiceRegistryService::class.java)

    fun registerService(request: CreateServiceRequest, performedBy: String? = null): ServiceDTO {
        logger.info("Registering new service: ${request.name}")

        if (serviceRepository.existsByNameAndDeletedAtIsNull(request.name)) {
            throw DuplicateServiceException("Service with name '${request.name}' already exists")
        }

        val service = Service(
            name = request.name,
            description = request.description,
            baseUrl = request.baseUrl,
            version = request.version,
            status = ServiceStatus.UNKNOWN,
            createdBy = performedBy
        )

        val savedService = serviceRepository.save(service)

        auditService.logServiceCreated(savedService, performedBy)

        logger.info("Service registered successfully: ${savedService.name} (ID: ${savedService.id})")

        return ServiceDTO.from(savedService)
    }

    @Transactional(readOnly = true)
    fun getService(id: Long): ServiceDTO {
        val service = findServiceOrThrow(id)

        val instanceCount = serviceInstanceRepository.countByServiceId(id).toInt()
        val healthyCount = serviceInstanceRepository.countByServiceIdAndStatus(id, InstanceStatus.UP).toInt()

        return ServiceDTO.from(service, instanceCount, healthyCount)
    }

    @Transactional(readOnly = true)
    fun getServiceByName(name: String): ServiceDTO {
        val service = serviceRepository.findByName(name)
            ?: throw ServiceNotFoundException("Service not found: $name")

        if (service.isDeleted()) {
            throw ServiceNotFoundException("Service has been deleted: $name")
        }

        val instanceCount = serviceInstanceRepository.countByServiceId(service.id!!).toInt()
        val healthyCount = serviceInstanceRepository.countByServiceIdAndStatus(service.id, InstanceStatus.UP).toInt()

        return ServiceDTO.from(service, instanceCount, healthyCount)
    }

    @Transactional(readOnly = true)
    fun getAllServices(): List<ServiceDTO> {
        logger.debug("Fetching all active services")

        val services = serviceRepository.findByDeletedAtIsNullOrderByNameAsc()

        return services.map { service ->
            val instanceCount = serviceInstanceRepository.countByServiceId(service.id!!).toInt()
            val healthyCount = serviceInstanceRepository.countByServiceIdAndStatus(service.id, InstanceStatus.UP).toInt()
            ServiceDTO.from(service, instanceCount, healthyCount)
        }
    }

    @Transactional(readOnly = true)
    fun searchServices(searchTerm: String): List<ServiceDTO> {
        logger.debug("Searching services with term: $searchTerm")

        val services = serviceRepository.searchByName(searchTerm)

        return services.map { service ->
            val instanceCount = serviceInstanceRepository.countByServiceId(service.id!!).toInt()
            val healthyCount = serviceInstanceRepository.countByServiceIdAndStatus(service.id, InstanceStatus.UP).toInt()
            ServiceDTO.from(service, instanceCount, healthyCount)
        }
    }

    fun updateService(id: Long, request: UpdateServiceRequest, performedBy: String? = null): ServiceDTO {
        logger.info("Updating service: $id")

        val service = findServiceOrThrow(id)
        val oldValue = mapOf(
            "description" to service.description,
            "baseUrl" to service.baseUrl,
            "version" to service.version,
            "status" to service.status.name
        )

        request.description?.let { service.description = it }
        request.baseUrl?.let { service.baseUrl = it }
        request.version?.let { service.version = it }
        request.status?.let { service.status = it }

        val updatedService = serviceRepository.save(service)

        val newValue = mapOf(
            "description" to service.description,
            "baseUrl" to service.baseUrl,
            "version" to service.version,
            "status" to service.status.name
        )

        auditService.logServiceUpdated(updatedService, performedBy, oldValue, newValue)

        logger.info("Service updated successfully: ${updatedService.name}")

        val instanceCount = serviceInstanceRepository.countByServiceId(id).toInt()
        val healthyCount = serviceInstanceRepository.countByServiceIdAndStatus(id, InstanceStatus.UP).toInt()

        return ServiceDTO.from(updatedService, instanceCount, healthyCount)
    }

    fun deleteService(id: Long, performedBy: String? = null) {
        logger.info("Deleting service: $id")

        val service = findServiceOrThrow(id)
        service.softDelete()
        serviceRepository.save(service)

        auditService.logServiceDeleted(service, performedBy)

        logger.info("Service deleted successfully: ${service.name}")
    }

    fun updateServiceStatus(id: Long, status: ServiceStatus, performedBy: String? = null) {
        logger.debug("Updating service status: $id -> $status")

        val service = findServiceOrThrow(id)
        val oldStatus = service.status
        service.status = status
        serviceRepository.save(service)

        if (oldStatus != status) {
            auditService.logServiceStatusChanged(service, performedBy, oldStatus, status)
        }
    }

    @Transactional(readOnly = true)
    fun getServiceSummary(): ServiceSummaryDTO {
        val totalServices = serviceRepository.countActive()
        val upServices = serviceRepository.countByStatus(ServiceStatus.UP)
        val downServices = serviceRepository.countByStatus(ServiceStatus.DOWN)
        val degradedServices = serviceRepository.countByStatus(ServiceStatus.DEGRADED)
        val totalInstances = serviceInstanceRepository.count()
        val healthyInstances = serviceInstanceRepository.countByStatus(InstanceStatus.UP)

        return ServiceSummaryDTO(
            totalServices = totalServices,
            upServices = upServices,
            downServices = downServices,
            degradedServices = degradedServices,
            totalInstances = totalInstances,
            healthyInstances = healthyInstances
        )
    }

    fun cleanupStaleServices(thresholdMinutes: Long = 60) {
        val threshold = LocalDateTime.now().minusMinutes(thresholdMinutes)
        val staleServices = serviceRepository.findStaleServices(threshold)

        staleServices.forEach { service ->
            logger.warn("Marking stale service as UNKNOWN: ${service.name}")
            service.status = ServiceStatus.UNKNOWN
            serviceRepository.save(service)
        }
    }

    private fun findServiceOrThrow(id: Long): Service {
        val service = serviceRepository.findById(id)
            .orElseThrow { ServiceNotFoundException("Service not found with id: $id") }

        if (service.isDeleted()) {
            throw ServiceNotFoundException("Service has been deleted: $id")
        }

        return service
    }
}
