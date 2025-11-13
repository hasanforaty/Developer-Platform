package com.devplatform.featureflags.domain.entity

import com.devplatform.featureflags.domain.enums.Environment
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Feature Rule Entity
 * Represents targeting and rollout rules for feature flags
 */
@Entity
@Table(name = "feature_rules")
class FeatureRule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * The feature flag this rule belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    var feature: Feature? = null,

    /**
     * Name of this rule (e.g., "Production Rollout 50%")
     */
    @Column(nullable = false, length = 200)
    var name: String,

    /**
     * Environment this rule applies to
     */
    @Column(nullable = false, length = 50)
    var environment: String = Environment.PRODUCTION.name.lowercase(),

    /**
     * Percentage of users to enable (0-100)
     * Used for gradual rollout
     */
    @Column(name = "rollout_percentage", nullable = false)
    var rolloutPercentage: Int = 0,

    /**
     * User segment targeting criteria (stored as JSON)
     * Example: {"country": ["US", "CA"], "userType": "premium"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_segment", columnDefinition = "jsonb")
    var userSegment: Map<String, Any>? = null,

    /**
     * Whether this rule is currently enabled
     */
    @Column(nullable = false)
    var enabled: Boolean = true,

    /**
     * Priority for rule evaluation (higher number = higher priority)
     * Rules are evaluated in descending priority order
     */
    @Column(nullable = false)
    var priority: Int = 0,

    /**
     * Timestamp when the rule was created
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when the rule was last updated (auto-updated by trigger)
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get environment as enum
     */
    fun getEnvironmentEnum(): Environment {
        return Environment.fromString(environment)
    }

    /**
     * Set environment from enum
     */
    fun setEnvironmentEnum(env: Environment) {
        this.environment = env.name.lowercase()
    }

    /**
     * Check if this rule matches a user segment
     */
    fun matchesUserSegment(userAttributes: Map<String, Any>): Boolean {
        if (userSegment == null) return true // No segment = matches everyone

        return userSegment!!.all { (key, value) ->
            val userValue = userAttributes[key]
            when (value) {
                is List<*> -> value.contains(userValue)
                else -> value == userValue
            }
        }
    }

    /**
     * Check if user ID falls within rollout percentage
     * Uses consistent hashing to ensure same user always gets same result
     */
    fun matchesRolloutPercentage(userId: String, featureKey: String): Boolean {
        if (rolloutPercentage == 100) return true
        if (rolloutPercentage == 0) return false

        // Use consistent hashing to determine if user is in rollout
        val hash = "$featureKey:$userId".hashCode()
        val bucket = (hash and 0x7FFFFFFF) % 100
        return bucket < rolloutPercentage
    }

    override fun toString(): String {
        return "FeatureRule(id=$id, name='$name', environment='$environment', rolloutPercentage=$rolloutPercentage, priority=$priority, enabled=$enabled)"
    }
}
