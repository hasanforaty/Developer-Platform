package com.devplatform.featureflags.repository

import com.devplatform.featureflags.domain.entity.Feature
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for Feature entities
 */
@Repository
interface FeatureRepository : JpaRepository<Feature, Long> {

    /**
     * Find feature by key
     */
    fun findByKey(key: String): Optional<Feature>

    /**
     * Find all enabled features
     */
    fun findByEnabled(enabled: Boolean): List<Feature>

    /**
     * Check if feature exists by key
     */
    fun existsByKey(key: String): Boolean

    /**
     * Find features with enabled rules for a specific environment
     */
    @Query("""
        SELECT DISTINCT f FROM Feature f
        LEFT JOIN FETCH f.rules r
        WHERE f.enabled = true
        AND r.enabled = true
        AND LOWER(r.environment) = LOWER(:environment)
    """)
    fun findEnabledFeaturesForEnvironment(@Param("environment") environment: String): List<Feature>

    /**
     * Find feature by key with rules eagerly loaded
     */
    @Query("""
        SELECT f FROM Feature f
        LEFT JOIN FETCH f.rules
        WHERE f.key = :key
    """)
    fun findByKeyWithRules(@Param("key") key: String): Optional<Feature>

    /**
     * Find all features with their rules
     */
    @Query("""
        SELECT DISTINCT f FROM Feature f
        LEFT JOIN FETCH f.rules
        ORDER BY f.name
    """)
    fun findAllWithRules(): List<Feature>

    /**
     * Search features by name or description
     */
    @Query("""
        SELECT f FROM Feature f
        WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :searchText, '%'))
        OR LOWER(f.description) LIKE LOWER(CONCAT('%', :searchText, '%'))
        ORDER BY f.name
    """)
    fun searchFeatures(@Param("searchText") searchText: String): List<Feature>
}
