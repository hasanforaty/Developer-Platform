package com.devplatform.featureflags.service

import com.devplatform.featureflags.domain.entity.FeatureRule
import com.devplatform.featureflags.domain.enums.AuditAction
import com.devplatform.featureflags.dto.CreateRuleRequest
import com.devplatform.featureflags.dto.UpdateRuleRequest
import com.devplatform.featureflags.repository.FeatureRuleRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing feature flag rules
 */
@Service
@Transactional
class FeatureRuleService(
    private val ruleRepository: FeatureRuleRepository,
    private val featureManagementService: FeatureManagementService,
    private val auditService: FeatureAuditService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new rule for a feature
     */
    @CacheEvict(value = ["features"], allEntries = true)
    fun createRule(featureId: Long, request: CreateRuleRequest, createdBy: String = "system"): FeatureRule {
        logger.info("Creating rule '${request.name}' for feature ID: $featureId")

        val feature = featureManagementService.getFeatureById(featureId)

        val rule = FeatureRule(
            feature = feature,
            name = request.name,
            environment = request.environment,
            rolloutPercentage = request.rolloutPercentage,
            userSegment = request.userSegment,
            enabled = request.enabled,
            priority = request.priority
        )

        feature.addRule(rule)
        val savedRule = ruleRepository.save(rule)

        // Create audit log
        auditService.logFeatureChange(
            feature = feature,
            action = AuditAction.RULE_ADDED,
            changedBy = createdBy,
            newValue = mapOf(
                "ruleId" to savedRule.id!!,
                "ruleName" to savedRule.name,
                "environment" to savedRule.environment,
                "rolloutPercentage" to savedRule.rolloutPercentage
            )
        )

        logger.info("Rule created: ${savedRule.name} (ID: ${savedRule.id})")
        return savedRule
    }

    /**
     * Get rule by ID
     */
    @Transactional(readOnly = true)
    fun getRuleById(id: Long): FeatureRule {
        return ruleRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("Rule with ID $id not found")
    }

    /**
     * Get all rules for a feature
     */
    @Transactional(readOnly = true)
    fun getRulesByFeature(featureId: Long): List<FeatureRule> {
        return ruleRepository.findByFeatureId(featureId)
    }

    /**
     * Get enabled rules for a feature in a specific environment
     */
    @Cacheable(value = ["rules"], key = "#featureId + '_' + #environment")
    @Transactional(readOnly = true)
    fun getEnabledRulesByFeatureAndEnvironment(featureId: Long, environment: String): List<FeatureRule> {
        return ruleRepository.findEnabledRulesByFeatureAndEnvironment(featureId, environment)
    }

    /**
     * Update a rule
     */
    @CacheEvict(value = ["features", "rules"], allEntries = true)
    fun updateRule(id: Long, request: UpdateRuleRequest, updatedBy: String = "system"): FeatureRule {
        logger.info("Updating rule ID: $id")

        val rule = getRuleById(id)

        // Capture old values for audit
        val oldValue = mapOf(
            "name" to rule.name,
            "environment" to rule.environment,
            "rolloutPercentage" to rule.rolloutPercentage,
            "enabled" to rule.enabled,
            "priority" to rule.priority
        )

        // Apply updates
        request.name?.let { rule.name = it }
        request.environment?.let { rule.environment = it }
        request.rolloutPercentage?.let { rule.rolloutPercentage = it }
        request.userSegment?.let { rule.userSegment = it }
        request.enabled?.let { rule.enabled = it }
        request.priority?.let { rule.priority = it }

        val updatedRule = ruleRepository.save(rule)

        // Create audit log
        auditService.logFeatureChange(
            feature = rule.feature!!,
            action = AuditAction.RULE_UPDATED,
            changedBy = updatedBy,
            oldValue = oldValue,
            newValue = mapOf(
                "name" to updatedRule.name,
                "environment" to updatedRule.environment,
                "rolloutPercentage" to updatedRule.rolloutPercentage,
                "enabled" to updatedRule.enabled,
                "priority" to updatedRule.priority
            )
        )

        logger.info("Rule updated: ${updatedRule.name} (ID: ${updatedRule.id})")
        return updatedRule
    }

    /**
     * Delete a rule
     */
    @CacheEvict(value = ["features", "rules"], allEntries = true)
    fun deleteRule(id: Long, deletedBy: String = "system") {
        logger.info("Deleting rule ID: $id")

        val rule = getRuleById(id)
        val feature = rule.feature!!

        // Create audit log before deletion
        auditService.logFeatureChange(
            feature = feature,
            action = AuditAction.RULE_DELETED,
            changedBy = deletedBy,
            oldValue = mapOf(
                "ruleId" to rule.id!!,
                "ruleName" to rule.name,
                "environment" to rule.environment
            )
        )

        feature.removeRule(rule)
        ruleRepository.delete(rule)

        logger.info("Rule deleted: ${rule.name} (ID: ${rule.id})")
    }

    /**
     * Toggle rule enabled state
     */
    @CacheEvict(value = ["features", "rules"], allEntries = true)
    fun toggleRule(id: Long, changedBy: String = "system"): FeatureRule {
        val rule = getRuleById(id)
        rule.enabled = !rule.enabled

        val updatedRule = ruleRepository.save(rule)

        auditService.logFeatureChange(
            feature = rule.feature!!,
            action = AuditAction.RULE_UPDATED,
            changedBy = changedBy,
            newValue = mapOf("enabled" to updatedRule.enabled)
        )

        logger.info("Rule ${updatedRule.name} ${if (updatedRule.enabled) "enabled" else "disabled"}")
        return updatedRule
    }

    /**
     * Count rules for a feature
     */
    @Transactional(readOnly = true)
    fun countRulesByFeature(featureId: Long): Long {
        return ruleRepository.countByFeatureId(featureId)
    }
}
