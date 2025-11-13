package com.devplatform.featureflags.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Request DTO for creating a new feature flag
 */
data class CreateFeatureRequest(
    @field:NotBlank(message = "Feature key is required")
    @field:Size(min = 3, max = 100, message = "Feature key must be between 3 and 100 characters")
    @field:Pattern(regexp = "^[a-z0-9_-]+$", message = "Feature key must contain only lowercase letters, numbers, hyphens, and underscores")
    val key: String,

    @field:NotBlank(message = "Feature name is required")
    @field:Size(min = 3, max = 200, message = "Feature name must be between 3 and 200 characters")
    val name: String,

    val description: String? = null,

    val enabled: Boolean = false,

    val createdBy: String? = null
)

/**
 * Request DTO for updating a feature flag
 */
data class UpdateFeatureRequest(
    @field:Size(min = 3, max = 200, message = "Feature name must be between 3 and 200 characters")
    val name: String? = null,

    val description: String? = null,

    val enabled: Boolean? = null,

    val updatedBy: String? = null
)

/**
 * Response DTO for feature flag
 */
data class FeatureResponse(
    val id: Long,
    val key: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val createdBy: String?,
    val updatedBy: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val rules: List<RuleResponse> = emptyList(),
    val ruleCount: Int = 0
)

/**
 * Summary DTO for feature flag (without rules)
 */
data class FeatureSummary(
    val id: Long,
    val key: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val ruleCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
