package com.devplatform.featureflags.service

import com.devplatform.featureflags.domain.entity.Feature
import com.devplatform.featureflags.domain.entity.FeatureAudit
import com.devplatform.featureflags.domain.enums.AuditAction
import com.devplatform.featureflags.repository.FeatureAuditRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for managing feature flag audit logs
 */
@Service
@Transactional
class FeatureAuditService(
    private val auditRepository: FeatureAuditRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Log a feature flag change
     */
    fun logFeatureChange(
        feature: Feature,
        action: AuditAction,
        changedBy: String,
        oldValue: Map<String, Any>? = null,
        newValue: Map<String, Any>? = null,
        reason: String? = null
    ): FeatureAudit {
        val audit = FeatureAudit(
            feature = feature,
            action = action,
            changedBy = changedBy,
            oldValue = oldValue,
            newValue = newValue,
            reason = reason
        )

        val savedAudit = auditRepository.save(audit)
        logger.debug("Audit log created: ${action.name} for feature ${feature.key} by $changedBy")

        return savedAudit
    }

    /**
     * Get audit logs for a feature
     */
    @Transactional(readOnly = true)
    fun getAuditLogsForFeature(featureId: Long): List<FeatureAudit> {
        return auditRepository.findByFeatureId(featureId)
    }

    /**
     * Get audit logs for a feature by key
     */
    @Transactional(readOnly = true)
    fun getAuditLogsForFeatureKey(featureKey: String): List<FeatureAudit> {
        return auditRepository.findByFeatureKey(featureKey)
    }

    /**
     * Get recent audit logs for a feature
     */
    @Transactional(readOnly = true)
    fun getRecentAuditLogsForFeature(featureId: Long, limit: Int = 10): List<FeatureAudit> {
        return auditRepository.findRecentByFeatureId(featureId, limit)
    }

    /**
     * Get audit logs by user
     */
    @Transactional(readOnly = true)
    fun getAuditLogsByUser(changedBy: String): List<FeatureAudit> {
        return auditRepository.findByChangedBy(changedBy)
    }

    /**
     * Get audit logs by action type
     */
    @Transactional(readOnly = true)
    fun getAuditLogsByAction(action: AuditAction): List<FeatureAudit> {
        return auditRepository.findByAction(action)
    }

    /**
     * Get audit logs within a time range
     */
    @Transactional(readOnly = true)
    fun getAuditLogsBetween(from: LocalDateTime, to: LocalDateTime): List<FeatureAudit> {
        return auditRepository.findByTimestampBetween(from, to)
    }

    /**
     * Get all audit logs
     */
    @Transactional(readOnly = true)
    fun getAllAuditLogs(): List<FeatureAudit> {
        return auditRepository.findAllOrderByTimestampDesc()
    }
}
