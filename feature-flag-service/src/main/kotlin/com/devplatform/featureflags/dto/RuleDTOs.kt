package com.devplatform.featureflags.dto

import jakarta.validation.constraints.*
import java.time.LocalDateTime

/**
 * Request DTO for creating a new feature rule
 */
data class CreateRuleRequest(
    @field:NotBlank(message = "Rule name is required")
    @field:Size(min = 3, max = 200, message = "Rule name must be between 3 and 200 characters")
    val name: String,

    @field:NotBlank(message = "Environment is required")
    @field:Pattern(regexp = "^(development|staging|production|test)$", message = "Invalid environment")
    val environment: String,

    @field:Min(value = 0, message = "Rollout percentage must be between 0 and 100")
    @field:Max(value = 100, message = "Rollout percentage must be between 0 and 100")
    val rolloutPercentage: Int = 0,

    val userSegment: Map<String, Any>? = null,

    val enabled: Boolean = true,

    @field:Min(value = 0, message = "Priority must be non-negative")
    val priority: Int = 0
)

/**
 * Request DTO for updating a feature rule
 */
data class UpdateRuleRequest(
    @field:Size(min = 3, max = 200, message = "Rule name must be between 3 and 200 characters")
    val name: String? = null,

    @field:Pattern(regexp = "^(development|staging|production|test)$", message = "Invalid environment")
    val environment: String? = null,

    @field:Min(value = 0, message = "Rollout percentage must be between 0 and 100")
    @field:Max(value = 100, message = "Rollout percentage must be between 0 and 100")
    val rolloutPercentage: Int? = null,

    val userSegment: Map<String, Any>? = null,

    val enabled: Boolean? = null,

    @field:Min(value = 0, message = "Priority must be non-negative")
    val priority: Int? = null
)

/**
 * Response DTO for feature rule
 */
data class RuleResponse(
    val id: Long,
    val name: String,
    val environment: String,
    val rolloutPercentage: Int,
    val userSegment: Map<String, Any>?,
    val enabled: Boolean,
    val priority: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
