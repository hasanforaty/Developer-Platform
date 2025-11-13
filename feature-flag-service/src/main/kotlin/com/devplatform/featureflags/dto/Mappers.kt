package com.devplatform.featureflags.dto

import com.devplatform.featureflags.domain.entity.Feature
import com.devplatform.featureflags.domain.entity.FeatureAudit
import com.devplatform.featureflags.domain.entity.FeatureRule

/**
 * Mapper functions to convert between entities and DTOs
 */
object Mappers {

    /**
     * Convert Feature entity to FeatureResponse DTO
     */
    fun Feature.toResponse(includeRules: Boolean = true): FeatureResponse {
        return FeatureResponse(
            id = this.id!!,
            key = this.key,
            name = this.name,
            description = this.description,
            enabled = this.enabled,
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            rules = if (includeRules) this.rules.map { it.toResponse() } else emptyList(),
            ruleCount = this.rules.size
        )
    }

    /**
     * Convert Feature entity to FeatureSummary DTO
     */
    fun Feature.toSummary(): FeatureSummary {
        return FeatureSummary(
            id = this.id!!,
            key = this.key,
            name = this.name,
            description = this.description,
            enabled = this.enabled,
            ruleCount = this.rules.size,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    /**
     * Convert FeatureRule entity to RuleResponse DTO
     */
    fun FeatureRule.toResponse(): RuleResponse {
        return RuleResponse(
            id = this.id!!,
            name = this.name,
            environment = this.environment,
            rolloutPercentage = this.rolloutPercentage,
            userSegment = this.userSegment,
            enabled = this.enabled,
            priority = this.priority,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    /**
     * Convert FeatureAudit entity to AuditLogResponse DTO
     */
    fun FeatureAudit.toResponse(): AuditLogResponse {
        return AuditLogResponse(
            id = this.id!!,
            featureId = this.feature!!.id!!,
            featureKey = this.feature!!.key,
            featureName = this.feature!!.name,
            action = this.action,
            actionDescription = this.getActionDescription(),
            changedBy = this.changedBy,
            timestamp = this.timestamp,
            oldValue = this.oldValue,
            newValue = this.newValue,
            reason = this.reason
        )
    }
}
