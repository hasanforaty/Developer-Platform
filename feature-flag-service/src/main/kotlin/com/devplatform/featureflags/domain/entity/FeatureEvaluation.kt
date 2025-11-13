package com.devplatform.featureflags.domain.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Feature Evaluation Entity
 * Tracks historical evaluations of feature flags for analytics and debugging
 */
@Entity
@Table(name = "feature_evaluations")
class FeatureEvaluation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * The feature flag that was evaluated
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    var feature: Feature? = null,

    /**
     * User ID for whom the flag was evaluated
     */
    @Column(name = "user_id", length = 100)
    var userId: String? = null,

    /**
     * Environment where the evaluation occurred
     */
    @Column(nullable = false, length = 50)
    var environment: String,

    /**
     * Result of the evaluation (true = enabled, false = disabled)
     */
    @Column(nullable = false)
    var result: Boolean,

    /**
     * The rule that produced this result (if any)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    var rule: FeatureRule? = null,

    /**
     * Timestamp of the evaluation
     */
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * Additional metadata about the evaluation context
     * Example: {"userAgent": "...", "ipAddress": "...", "sessionId": "..."}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null
) {
    override fun toString(): String {
        return "FeatureEvaluation(id=$id, userId='$userId', environment='$environment', result=$result, timestamp=$timestamp)"
    }
}
