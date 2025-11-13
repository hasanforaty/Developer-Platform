package com.devplatform.serviceregistry.repository

import com.devplatform.serviceregistry.domain.entity.HealthCheck
import com.devplatform.serviceregistry.domain.entity.HealthStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HealthCheckRepository : JpaRepository<HealthCheck, Long> {

    fun findByInstanceIdOrderByTimestampDesc(instanceId: Long, pageable: Pageable): List<HealthCheck>

    @Query("""
        SELECT h FROM HealthCheck h
        WHERE h.instance.id = :instanceId
        AND h.timestamp >= :since
        ORDER BY h.timestamp DESC
    """)
    fun findRecentByInstanceId(
        @Param("instanceId") instanceId: Long,
        @Param("since") since: LocalDateTime
    ): List<HealthCheck>

    @Query("""
        SELECT COUNT(h) FROM HealthCheck h
        WHERE h.instance.id = :instanceId
        AND h.status = :status
    """)
    fun countByInstanceIdAndStatus(
        @Param("instanceId") instanceId: Long,
        @Param("status") status: HealthStatus
    ): Long

    @Query("""
        SELECT AVG(h.responseTimeMs) FROM HealthCheck h
        WHERE h.instance.id = :instanceId
        AND h.responseTimeMs IS NOT NULL
        AND h.timestamp >= :since
    """)
    fun calculateAverageResponseTime(
        @Param("instanceId") instanceId: Long,
        @Param("since") since: LocalDateTime
    ): Double?

    @Query("""
        SELECT h FROM HealthCheck h
        WHERE h.instance.service.id = :serviceId
        ORDER BY h.timestamp DESC
    """)
    fun findByServiceId(
        @Param("serviceId") serviceId: Long,
        pageable: Pageable
    ): List<HealthCheck>

    @Query("""
        SELECT (CAST(COUNT(CASE WHEN h.status = 'HEALTHY' THEN 1 END) AS double) / COUNT(*)) * 100
        FROM HealthCheck h
        WHERE h.instance.id = :instanceId
        AND h.timestamp >= :since
    """)
    fun calculateUptimePercentage(
        @Param("instanceId") instanceId: Long,
        @Param("since") since: LocalDateTime
    ): Double?

    @Modifying
    @Query("DELETE FROM HealthCheck h WHERE h.timestamp < :threshold")
    fun deleteOlderThan(@Param("threshold") threshold: LocalDateTime): Int

    @Query("""
        SELECT h FROM HealthCheck h
        WHERE h.instance.id = :instanceId
        ORDER BY h.timestamp DESC
        LIMIT 1
    """)
    fun findLatestByInstanceId(@Param("instanceId") instanceId: Long): HealthCheck?
}
