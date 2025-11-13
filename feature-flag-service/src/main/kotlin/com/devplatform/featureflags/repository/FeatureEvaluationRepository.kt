package com.devplatform.featureflags.repository

import com.devplatform.featureflags.domain.entity.FeatureEvaluation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for FeatureEvaluation entities
 */
@Repository
interface FeatureEvaluationRepository : JpaRepository<FeatureEvaluation, Long> {

    /**
     * Find evaluations for a specific feature
     */
    fun findByFeatureId(featureId: Long): List<FeatureEvaluation>

    /**
     * Find evaluations for a specific user
     */
    fun findByUserId(userId: String): List<FeatureEvaluation>

    /**
     * Find evaluations for a feature and user
     */
    fun findByFeatureIdAndUserId(featureId: Long, userId: String): List<FeatureEvaluation>

    /**
     * Find evaluations within a time range
     */
    fun findByTimestampBetween(from: LocalDateTime, to: LocalDateTime): List<FeatureEvaluation>

    /**
     * Count evaluations by feature
     */
    fun countByFeatureId(featureId: Long): Long

    /**
     * Count evaluations by feature and result
     */
    fun countByFeatureIdAndResult(featureId: Long, result: Boolean): Long

    /**
     * Get evaluation statistics for a feature
     */
    @Query("""
        SELECT
            COUNT(e) as totalEvaluations,
            SUM(CASE WHEN e.result = true THEN 1 ELSE 0 END) as enabledCount,
            SUM(CASE WHEN e.result = false THEN 1 ELSE 0 END) as disabledCount
        FROM FeatureEvaluation e
        WHERE e.feature.id = :featureId
        AND e.timestamp >= :since
    """)
    fun getEvaluationStats(
        @Param("featureId") featureId: Long,
        @Param("since") since: LocalDateTime
    ): Map<String, Long>

    /**
     * Find recent evaluations for a feature
     */
    @Query("""
        SELECT e FROM FeatureEvaluation e
        WHERE e.feature.id = :featureId
        ORDER BY e.timestamp DESC
    """)
    fun findRecentEvaluationsByFeature(@Param("featureId") featureId: Long): List<FeatureEvaluation>

    /**
     * Delete old evaluations (for cleanup)
     */
    fun deleteByTimestampBefore(timestamp: LocalDateTime): Long

    /**
     * Find evaluations by environment
     */
    @Query("""
        SELECT e FROM FeatureEvaluation e
        WHERE LOWER(e.environment) = LOWER(:environment)
        AND e.timestamp >= :since
        ORDER BY e.timestamp DESC
    """)
    fun findByEnvironmentSince(
        @Param("environment") environment: String,
        @Param("since") since: LocalDateTime
    ): List<FeatureEvaluation>
}
