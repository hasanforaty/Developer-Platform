package com.devplatform.serviceregistry.service

import com.devplatform.serviceregistry.domain.entity.*
import com.devplatform.serviceregistry.repository.AuditLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {

    fun logServiceCreated(service: com.devplatform.serviceregistry.domain.entity.Service, performedBy: String?) {
        val auditLog = AuditLog(
            entityType = "SERVICE",
            entityId = service.id!!,
            action = "CREATED",
            performedBy = performedBy,
            newValue = mapOf(
                "name" to service.name,
                "baseUrl" to service.baseUrl,
                "status" to service.status.name
            )
        )
        auditLogRepository.save(auditLog)
    }

    fun logServiceUpdated(service: com.devplatform.serviceregistry.domain.entity.Service, performedBy: String?, oldValue: Map<String, Any?>, newValue: Map<String, Any?>) {
        val auditLog = AuditLog(
            entityType = "SERVICE",
            entityId = service.id!!,
            action = "UPDATED",
            performedBy = performedBy,
            oldValue = oldValue,
            newValue = newValue
        )
        auditLogRepository.save(auditLog)
    }

    fun logServiceDeleted(service: com.devplatform.serviceregistry.domain.entity.Service, performedBy: String?) {
        val auditLog = AuditLog(
            entityType = "SERVICE",
            entityId = service.id!!,
            action = "DELETED",
            performedBy = performedBy,
            oldValue = mapOf(
                "name" to service.name,
                "status" to service.status.name
            )
        )
        auditLogRepository.save(auditLog)
    }

    fun logServiceStatusChanged(service: com.devplatform.serviceregistry.domain.entity.Service, performedBy: String?, oldStatus: ServiceStatus, newStatus: ServiceStatus) {
        val auditLog = AuditLog(
            entityType = "SERVICE",
            entityId = service.id!!,
            action = "STATUS_CHANGED",
            performedBy = performedBy,
            oldValue = mapOf("status" to oldStatus.name),
            newValue = mapOf("status" to newStatus.name)
        )
        auditLogRepository.save(auditLog)
    }

    fun logInstanceCreated(instance: ServiceInstance, performedBy: String?) {
        val auditLog = AuditLog(
            entityType = "SERVICE_INSTANCE",
            entityId = instance.id!!,
            action = "CREATED",
            performedBy = performedBy,
            newValue = mapOf(
                "instanceId" to instance.instanceId,
                "host" to instance.host,
                "port" to instance.port,
                "status" to instance.status.name
            )
        )
        auditLogRepository.save(auditLog)
    }

    fun logInstanceUpdated(instance: ServiceInstance, performedBy: String?) {
        val auditLog = AuditLog(
            entityType = "SERVICE_INSTANCE",
            entityId = instance.id!!,
            action = "UPDATED",
            performedBy = performedBy,
            newValue = mapOf(
                "instanceId" to instance.instanceId,
                "status" to instance.status.name
            )
        )
        auditLogRepository.save(auditLog)
    }

    fun logInstanceDeleted(instance: ServiceInstance, performedBy: String?) {
        val auditLog = AuditLog(
            entityType = "SERVICE_INSTANCE",
            entityId = instance.id!!,
            action = "DELETED",
            performedBy = performedBy,
            oldValue = mapOf(
                "instanceId" to instance.instanceId,
                "host" to instance.host,
                "port" to instance.port
            )
        )
        auditLogRepository.save(auditLog)
    }

    fun logInstanceStatusChanged(instance: ServiceInstance, performedBy: String?, oldStatus: InstanceStatus, newStatus: InstanceStatus) {
        val auditLog = AuditLog(
            entityType = "SERVICE_INSTANCE",
            entityId = instance.id!!,
            action = "STATUS_CHANGED",
            performedBy = performedBy,
            oldValue = mapOf("status" to oldStatus.name),
            newValue = mapOf("status" to newStatus.name)
        )
        auditLogRepository.save(auditLog)
    }
}
