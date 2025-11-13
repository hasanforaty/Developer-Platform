package com.devplatform.featureflags.service

import com.devplatform.featureflags.domain.entity.Feature
import com.devplatform.featureflags.domain.enums.AuditAction
import com.devplatform.featureflags.dto.CreateFeatureRequest
import com.devplatform.featureflags.dto.UpdateFeatureRequest
import com.devplatform.featureflags.repository.FeatureRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing feature flags
 */
@Service
@Transactional
class FeatureManagementService(
    private val featureRepository: FeatureRepository,
    private val auditService: FeatureAuditService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new feature flag
     */
    @CacheEvict(value = ["features"], allEntries = true)
    fun createFeature(request: CreateFeatureRequest): Feature {
        logger.info("Creating feature: ${request.key}")

        // Check if feature already exists
        if (featureRepository.existsByKey(request.key)) {
            throw IllegalArgumentException("Feature with key '${request.key}' already exists")
        }

        val feature = Feature(
            key = request.key,
            name = request.name,
            description = request.description,
            enabled = request.enabled,
            createdBy = request.createdBy
        )

        val savedFeature = featureRepository.save(feature)

        // Create audit log
        auditService.logFeatureChange(
            feature = savedFeature,
            action = AuditAction.CREATED,
            changedBy = request.createdBy ?: "system",
            newValue = mapOf(
                "key" to savedFeature.key,
                "name" to savedFeature.name,
                "enabled" to savedFeature.enabled
            )
        )

        logger.info("Feature created: ${savedFeature.key} (ID: ${savedFeature.id})")
        return savedFeature
    }

    /**
     * Get feature by ID
     */
    @Cacheable(value = ["features"], key = "#id")
    @Transactional(readOnly = true)
    fun getFeatureById(id: Long): Feature {
        return featureRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Feature with ID $id not found")
    }

    /**
     * Get feature by key
     */
    @Cacheable(value = ["features"], key = "#key")
    @Transactional(readOnly = true)
    fun getFeatureByKey(key: String): Feature {
        return featureRepository.findByKey(key).orElseThrow {
            NoSuchElementException("Feature with key '$key' not found")
        }
    }

    /**
     * Get feature by key with rules eagerly loaded
     */
    @Cacheable(value = ["features"], key = "'with_rules_' + #key")
    @Transactional(readOnly = true)
    fun getFeatureByKeyWithRules(key: String): Feature {
        return featureRepository.findByKeyWithRules(key).orElseThrow {
            NoSuchElementException("Feature with key '$key' not found")
        }
    }

    /**
     * Get all features
     */
    @Cacheable(value = ["features"], key = "'all'")
    @Transactional(readOnly = true)
    fun getAllFeatures(): List<Feature> {
        return featureRepository.findAll()
    }

    /**
     * Get all features with rules
     */
    @Cacheable(value = ["features"], key = "'all_with_rules'")
    @Transactional(readOnly = true)
    fun getAllFeaturesWithRules(): List<Feature> {
        return featureRepository.findAllWithRules()
    }

    /**
     * Search features by text
     */
    @Transactional(readOnly = true)
    fun searchFeatures(searchText: String): List<Feature> {
        return featureRepository.searchFeatures(searchText)
    }

    /**
     * Update a feature flag
     */
    @CacheEvict(value = ["features"], allEntries = true)
    fun updateFeature(id: Long, request: UpdateFeatureRequest): Feature {
        logger.info("Updating feature ID: $id")

        val feature = getFeatureById(id)

        // Capture old values for audit
        val oldValue = mapOf(
            "name" to feature.name,
            "description" to (feature.description ?: ""),
            "enabled" to feature.enabled
        )

        // Apply updates
        request.name?.let { feature.name = it }
        request.description?.let { feature.description = it }
        request.enabled?.let {
            if (feature.enabled != it) {
                feature.enabled = it
                // Log separate enable/disable action
                auditService.logFeatureChange(
                    feature = feature,
                    action = if (it) AuditAction.ENABLED else AuditAction.DISABLED,
                    changedBy = request.updatedBy ?: "system"
                )
            }
        }
        request.updatedBy?.let { feature.updatedBy = it }

        val updatedFeature = featureRepository.save(feature)

        // Create audit log for update
        auditService.logFeatureChange(
            feature = updatedFeature,
            action = AuditAction.UPDATED,
            changedBy = request.updatedBy ?: "system",
            oldValue = oldValue,
            newValue = mapOf(
                "name" to updatedFeature.name,
                "description" to (updatedFeature.description ?: ""),
                "enabled" to updatedFeature.enabled
            )
        )

        logger.info("Feature updated: ${updatedFeature.key} (ID: ${updatedFeature.id})")
        return updatedFeature
    }

    /**
     * Delete a feature flag
     */
    @CacheEvict(value = ["features"], allEntries = true)
    fun deleteFeature(id: Long, deletedBy: String = "system") {
        logger.info("Deleting feature ID: $id")

        val feature = getFeatureById(id)

        // Create audit log before deletion
        auditService.logFeatureChange(
            feature = feature,
            action = AuditAction.DELETED,
            changedBy = deletedBy,
            oldValue = mapOf(
                "key" to feature.key,
                "name" to feature.name,
                "enabled" to feature.enabled
            )
        )

        featureRepository.delete(feature)

        logger.info("Feature deleted: ${feature.key} (ID: ${feature.id})")
    }

    /**
     * Toggle feature enabled state
     */
    @CacheEvict(value = ["features"], allEntries = true)
    fun toggleFeature(id: Long, changedBy: String = "system"): Feature {
        val feature = getFeatureById(id)
        feature.enabled = !feature.enabled
        feature.updatedBy = changedBy

        val updatedFeature = featureRepository.save(feature)

        auditService.logFeatureChange(
            feature = updatedFeature,
            action = if (updatedFeature.enabled) AuditAction.ENABLED else AuditAction.DISABLED,
            changedBy = changedBy
        )

        logger.info("Feature ${updatedFeature.key} ${if (updatedFeature.enabled) "enabled" else "disabled"}")
        return updatedFeature
    }

    /**
     * Get enabled features for a specific environment
     */
    @Transactional(readOnly = true)
    fun getEnabledFeaturesForEnvironment(environment: String): List<Feature> {
        return featureRepository.findEnabledFeaturesForEnvironment(environment)
    }
}
