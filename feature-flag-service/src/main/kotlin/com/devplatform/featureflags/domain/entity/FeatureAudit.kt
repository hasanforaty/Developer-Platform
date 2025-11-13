package com.devplatform.featureflags.domain.entity

import com.devplatform.featureflags.domain.enums.AuditAction
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Feature Audit Entity
 * Tracks all changes to feature flags for compliance and debugging
 */
@Entity
@Table(name = "feature_audit")
class FeatureAudit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * The feature flag that was changed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    var feature: Feature? = null,

    /**
     * Type of action performed
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var action: AuditAction,

    /**
     * User who made the change
     */
    @Column(name = "changed_by", nullable = false, length = 100)
    var changedBy: String,

    /**
     * Timestamp when the change occurred
     */
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * Previous value before the change (stored as JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    var oldValue: Map<String, Any>? = null,

    /**
     * New value after the change (stored as JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    var newValue: Map<String, Any>? = null,

    /**
     * Reason for the change (optional)
     */
    @Column(columnDefinition = "TEXT")
    var reason: String? = null
) {
    /**
     * Get human-readable description of the action
     */
    fun getActionDescription(): String {
        return action.getDescription()
    }

    override fun toString(): String {
        return "FeatureAudit(id=$id, action=$action, changedBy='$changedBy', timestamp=$timestamp)"
    }
}
