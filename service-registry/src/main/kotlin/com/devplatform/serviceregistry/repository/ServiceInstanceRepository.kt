package com.devplatform.serviceregistry.repository

import com.devplatform.serviceregistry.domain.entity.InstanceStatus
import com.devplatform.serviceregistry.domain.entity.ServiceInstance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ServiceInstanceRepository : JpaRepository<ServiceInstance, Long> {

    fun findByInstanceId(instanceId: String): ServiceInstance?

    fun findByServiceId(serviceId: Long): List<ServiceInstance>

    fun findByServiceIdAndStatus(serviceId: Long, status: InstanceStatus): List<ServiceInstance>

    fun existsByInstanceId(instanceId: String): Boolean

    @Query("""
        SELECT i FROM ServiceInstance i
        WHERE i.service.id = :serviceId
        ORDER BY i.createdAt DESC
    """)
    fun findByServiceIdOrderByCreatedAtDesc(@Param("serviceId") serviceId: Long): List<ServiceInstance>

    @Query("""
        SELECT COUNT(i) FROM ServiceInstance i
        WHERE i.service.id = :serviceId
    """)
    fun countByServiceId(@Param("serviceId") serviceId: Long): Long

    @Query("""
        SELECT COUNT(i) FROM ServiceInstance i
        WHERE i.service.id = :serviceId
        AND i.status = :status
    """)
    fun countByServiceIdAndStatus(
        @Param("serviceId") serviceId: Long,
        @Param("status") status: InstanceStatus
    ): Long

    @Query("""
        SELECT COUNT(i) FROM ServiceInstance i
        WHERE i.status = :status
    """)
    fun countByStatus(@Param("status") status: InstanceStatus): Long

    @Query("""
        SELECT i FROM ServiceInstance i
        WHERE i.lastHeartbeat < :threshold
        OR i.lastHeartbeat IS NULL
    """)
    fun findInstancesWithStaleHeartbeat(@Param("threshold") threshold: LocalDateTime): List<ServiceInstance>

    @Query("""
        SELECT i FROM ServiceInstance i
        LEFT JOIN FETCH i.healthChecks h
        WHERE i.id = :id
    """)
    fun findByIdWithHealthChecks(@Param("id") id: Long): ServiceInstance?
}
