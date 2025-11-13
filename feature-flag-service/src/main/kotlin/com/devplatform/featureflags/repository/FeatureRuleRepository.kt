package com.devplatform.featureflags.repository

import com.devplatform.featureflags.domain.entity.FeatureRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for FeatureRule entities
 */
@Repository
interface FeatureRuleRepository : JpaRepository<FeatureRule, Long> {

    /**
     * Find all rules for a specific feature
     */
    fun findByFeatureId(featureId: Long): List<FeatureRule>

    /**
     * Find enabled rules for a feature in a specific environment
     * Ordered by priority (descending)
     */
    @Query("""
        SELECT r FROM FeatureRule r
        WHERE r.feature.id = :featureId
        AND r.enabled = true
        AND LOWER(r.environment) = LOWER(:environment)
        ORDER BY r.priority DESC
    """)
    fun findEnabledRulesByFeatureAndEnvironment(
        @Param("featureId") featureId: Long,
        @Param("environment") environment: String
    ): List<FeatureRule>

    /**
     * Find all rules for a specific environment
     */
    @Query("""
        SELECT r FROM FeatureRule r
        WHERE LOWER(r.environment) = LOWER(:environment)
        AND r.enabled = true
        ORDER BY r.priority DESC
    """)
    fun findByEnvironment(@Param("environment") environment: String): List<FeatureRule>

    /**
     * Count rules by feature
     */
    fun countByFeatureId(featureId: Long): Long

    /**
     * Find rules by feature key and environment
     */
    @Query("""
        SELECT r FROM FeatureRule r
        WHERE r.feature.key = :featureKey
        AND LOWER(r.environment) = LOWER(:environment)
        AND r.enabled = true
        ORDER BY r.priority DESC
    """)
    fun findByFeatureKeyAndEnvironment(
        @Param("featureKey") featureKey: String,
        @Param("environment") environment: String
    ): List<FeatureRule>
}
