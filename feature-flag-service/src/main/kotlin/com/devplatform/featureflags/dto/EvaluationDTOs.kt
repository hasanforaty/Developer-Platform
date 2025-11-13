package com.devplatform.featureflags.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.LocalDateTime

/**
 * Request DTO for evaluating a feature flag
 */
data class EvaluationRequest(
    @field:NotBlank(message = "Feature key is required")
    val featureKey: String,

    val userId: String? = null,

    @field:NotBlank(message = "Environment is required")
    @field:Pattern(regexp = "^(development|staging|production|test)$", message = "Invalid environment")
    val environment: String,

    val userAttributes: Map<String, Any> = emptyMap(),

    val metadata: Map<String, Any>? = null
)

/**
 * Batch evaluation request for multiple features
 */
data class BatchEvaluationRequest(
    val featureKeys: List<String>,

    val userId: String? = null,

    @field:NotBlank(message = "Environment is required")
    @field:Pattern(regexp = "^(development|staging|production|test)$", message = "Invalid environment")
    val environment: String,

    val userAttributes: Map<String, Any> = emptyMap()
)

/**
 * Response DTO for feature evaluation
 */
data class EvaluationResponse(
    val featureKey: String,
    val enabled: Boolean,
    val ruleId: Long? = null,
    val ruleName: String? = null,
    val reason: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Batch evaluation response
 */
data class BatchEvaluationResponse(
    val results: Map<String, EvaluationResponse>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Response DTO for evaluation statistics
 */
data class EvaluationStatsResponse(
    val featureId: Long,
    val featureKey: String,
    val totalEvaluations: Long,
    val enabledCount: Long,
    val disabledCount: Long,
    val enabledPercentage: Double,
    val period: String
)
