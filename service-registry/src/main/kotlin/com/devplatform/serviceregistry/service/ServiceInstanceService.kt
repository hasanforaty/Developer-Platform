package com.devplatform.serviceregistry.service

import com.devplatform.serviceregistry.domain.dto.*
import com.devplatform.serviceregistry.domain.entity.InstanceStatus
import com.devplatform.serviceregistry.domain.entity.ServiceInstance
import com.devplatform.serviceregistry.exception.DuplicateInstanceException
import com.devplatform.serviceregistry.exception.InstanceNotFoundException
import com.devplatform.serviceregistry.exception.ServiceNotFoundException
import com.devplatform.serviceregistry.repository.ServiceInstanceRepository
import com.devplatform.serviceregistry.repository.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ServiceInstanceService(
    private val instanceRepository: ServiceInstanceRepository,
    private val serviceRepository: ServiceRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(ServiceInstanceService::class.java)

    fun registerInstance(serviceId: Long, request: RegisterInstanceRequest, performedBy: String? = null): ServiceInstanceDTO {
        logger.info("Registering instance for service {}: {}", serviceId, request.instanceId)

        val service = serviceRepository.findById(serviceId)
            .orElseThrow { ServiceNotFoundException("Service not found with id: $serviceId") }

        if (service.isDeleted()) {
            throw ServiceNotFoundException("Cannot register instance for deleted service: $serviceId")
        }

        if (instanceRepository.existsByInstanceId(request.instanceId)) {
            throw DuplicateInstanceException("Instance with ID '${request.instanceId}' already exists")
        }

        val instance = ServiceInstance(
            instanceId = request.instanceId,
            host = request.host,
            port = request.port,
            status = InstanceStatus.STARTING,
            metadata = request.metadata
        )

        service.addInstance(instance)
        val savedInstance = instanceRepository.save(instance)
        savedInstance.updateHeartbeat()
        instanceRepository.save(savedInstance)

        auditService.logInstanceCreated(savedInstance, performedBy)

        logger.info("Instance registered successfully: ${savedInstance.instanceId}")

        return ServiceInstanceDTO.from(savedInstance)
    }

    @Transactional(readOnly = true)
    fun getInstance(id: Long): ServiceInstanceDTO {
        val instance = findInstanceOrThrow(id)
        return ServiceInstanceDTO.from(instance, includeHealthChecks = true)
    }

    @Transactional(readOnly = true)
    fun getInstancesByService(serviceId: Long): List<ServiceInstanceDTO> {
        logger.debug("Fetching instances for service: $serviceId")

        // Verify service exists
        serviceRepository.findById(serviceId)
            .orElseThrow { ServiceNotFoundException("Service not found with id: $serviceId") }

        val instances = instanceRepository.findByServiceIdOrderByCreatedAtDesc(serviceId)

        return instances.map { ServiceInstanceDTO.from(it) }
    }

    fun updateInstance(id: Long, request: UpdateInstanceRequest, performedBy: String? = null): ServiceInstanceDTO {
        logger.info("Updating instance: $id")

        val instance = findInstanceOrThrow(id)

        request.status?.let { instance.status = it }
        request.metadata?.let { instance.metadata = it }

        val updatedInstance = instanceRepository.save(instance)

        auditService.logInstanceUpdated(updatedInstance, performedBy)

        logger.info("Instance updated successfully: ${updatedInstance.instanceId}")

        return ServiceInstanceDTO.from(updatedInstance)
    }

    fun deregisterInstance(id: Long, performedBy: String? = null) {
        logger.info("Deregistering instance: $id")

        val instance = findInstanceOrThrow(id)

        auditService.logInstanceDeleted(instance, performedBy)

        instanceRepository.delete(instance)

        logger.info("Instance deregistered successfully: ${instance.instanceId}")
    }

    fun sendHeartbeat(instanceId: String, request: HeartbeatRequest): ServiceInstanceDTO {
        logger.debug("Received heartbeat from instance: $instanceId")

        val instance = instanceRepository.findByInstanceId(instanceId)
            ?: throw InstanceNotFoundException("Instance not found: $instanceId")

        instance.status = request.status
        instance.updateHeartbeat()
        request.metadata?.let { instance.metadata = it }

        val updatedInstance = instanceRepository.save(instance)

        return ServiceInstanceDTO.from(updatedInstance)
    }

    fun markInstanceAsDown(id: Long, performedBy: String? = null) {
        logger.warn("Marking instance as DOWN: $id")

        val instance = findInstanceOrThrow(id)
        instance.markAsDown()
        instanceRepository.save(instance)

        auditService.logInstanceStatusChanged(instance, performedBy, InstanceStatus.UP, InstanceStatus.DOWN)
    }

    fun checkStaleInstances(heartbeatTimeoutMinutes: Long = 5): Int {
        val threshold = LocalDateTime.now().minusMinutes(heartbeatTimeoutMinutes)
        val staleInstances = instanceRepository.findInstancesWithStaleHeartbeat(threshold)

        var markedDown = 0

        staleInstances.forEach { instance ->
            if (instance.status != InstanceStatus.DOWN) {
                logger.warn("Marking stale instance as DOWN: ${instance.instanceId} (last heartbeat: ${instance.lastHeartbeat})")
                instance.markAsDown()
                instanceRepository.save(instance)
                markedDown++
            }
        }

        if (markedDown > 0) {
            logger.info("Marked $markedDown stale instances as DOWN")
        }

        return markedDown
    }

    private fun findInstanceOrThrow(id: Long): ServiceInstance {
        return instanceRepository.findById(id)
            .orElseThrow { InstanceNotFoundException("Instance not found with id: $id") }
    }
}
