package com.devplatform.featureflags.repository

import com.devplatform.featureflags.domain.entity.FeatureAudit
import com.devplatform.featureflags.domain.enums.AuditAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for FeatureAudit entities
 */
@Repository
interface FeatureAuditRepository : JpaRepository<FeatureAudit, Long> {

    /**
     * Find audit logs for a specific feature
     */
    @Query("""
        SELECT a FROM FeatureAudit a
        WHERE a.feature.id = :featureId
        ORDER BY a.timestamp DESC
    """)
    fun findByFeatureId(@Param("featureId") featureId: Long): List<FeatureAudit>

    /**
     * Find audit logs by user
     */
    fun findByChangedBy(changedBy: String): List<FeatureAudit>

    /**
     * Find audit logs by action
     */
    fun findByAction(action: AuditAction): List<FeatureAudit>

    /**
     * Find audit logs within a time range
     */
    fun findByTimestampBetween(from: LocalDateTime, to: LocalDateTime): List<FeatureAudit>

    /**
     * Find recent audit logs for a feature
     */
    @Query("""
        SELECT a FROM FeatureAudit a
        WHERE a.feature.id = :featureId
        ORDER BY a.timestamp DESC
        LIMIT :limit
    """)
    fun findRecentByFeatureId(
        @Param("featureId") featureId: Long,
        @Param("limit") limit: Int
    ): List<FeatureAudit>

    /**
     * Find audit logs by feature key
     */
    @Query("""
        SELECT a FROM FeatureAudit a
        WHERE a.feature.key = :featureKey
        ORDER BY a.timestamp DESC
    """)
    fun findByFeatureKey(@Param("featureKey") featureKey: String): List<FeatureAudit>

    /**
     * Count audit logs by feature
     */
    fun countByFeatureId(featureId: Long): Long

    /**
     * Find all audit logs ordered by timestamp
     */
    @Query("""
        SELECT a FROM FeatureAudit a
        ORDER BY a.timestamp DESC
    """)
    fun findAllOrderByTimestampDesc(): List<FeatureAudit>
}
