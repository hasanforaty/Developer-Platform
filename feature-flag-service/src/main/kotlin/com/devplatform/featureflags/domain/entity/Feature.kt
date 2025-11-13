package com.devplatform.featureflags.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Feature Flag Entity
 * Represents a feature flag definition with its configuration
 */
@Entity
@Table(name = "features")
class Feature(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Unique key for the feature flag (e.g., "new_checkout_flow")
     * Used by client SDKs to query the flag
     */
    @Column(nullable = false, unique = true, length = 100)
    var key: String,

    /**
     * Human-readable name for the feature
     */
    @Column(nullable = false, length = 200)
    var name: String,

    /**
     * Description of what this feature flag controls
     */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /**
     * Global enabled/disabled state
     * If false, the flag is disabled regardless of rules
     */
    @Column(nullable = false)
    var enabled: Boolean = false,

    /**
     * User who created this feature flag
     */
    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    /**
     * User who last updated this feature flag
     */
    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null,

    /**
     * Timestamp when the feature was created
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when the feature was last updated (auto-updated by trigger)
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Rules associated with this feature flag
     * Cascade all operations to rules
     */
    @OneToMany(mappedBy = "feature", cascade = [CascadeType.ALL], orphanRemoval = true)
    val rules: MutableList<FeatureRule> = mutableListOf(),

    /**
     * Evaluation history for this feature
     * Cascade all operations
     */
    @OneToMany(mappedBy = "feature", cascade = [CascadeType.ALL], orphanRemoval = true)
    val evaluations: MutableList<FeatureEvaluation> = mutableListOf(),

    /**
     * Audit trail for this feature
     * Cascade all operations
     */
    @OneToMany(mappedBy = "feature", cascade = [CascadeType.ALL], orphanRemoval = true)
    val auditLogs: MutableList<FeatureAudit> = mutableListOf()
) {
    /**
     * Add a rule to this feature flag
     */
    fun addRule(rule: FeatureRule) {
        rules.add(rule)
        rule.feature = this
    }

    /**
     * Remove a rule from this feature flag
     */
    fun removeRule(rule: FeatureRule) {
        rules.remove(rule)
        rule.feature = null
    }

    /**
     * Check if feature is enabled for a given environment
     * Returns true if feature is globally enabled AND has at least one enabled rule for the environment
     */
    fun isEnabledForEnvironment(environment: String): Boolean {
        if (!enabled) return false
        return rules.any { it.enabled && it.environment.equals(environment, ignoreCase = true) }
    }

    override fun toString(): String {
        return "Feature(id=$id, key='$key', name='$name', enabled=$enabled)"
    }
}
