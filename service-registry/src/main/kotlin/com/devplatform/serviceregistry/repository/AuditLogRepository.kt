package com.devplatform.serviceregistry.repository

import com.devplatform.serviceregistry.domain.entity.AuditLog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    fun findByEntityTypeAndEntityIdOrderByTimestampDesc(
        entityType: String,
        entityId: Long,
        pageable: Pageable
    ): List<AuditLog>

    fun findByPerformedByOrderByTimestampDesc(
        performedBy: String,
        pageable: Pageable
    ): List<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
        AND a.entityId = :entityId
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
    """)
    fun findRecentByEntity(
        @Param("entityType") entityType: String,
        @Param("entityId") entityId: Long,
        @Param("since") since: LocalDateTime
    ): List<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.timestamp BETWEEN :start AND :end
        ORDER BY a.timestamp DESC
    """)
    fun findByTimestampBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        pageable: Pageable
    ): List<AuditLog>
}
