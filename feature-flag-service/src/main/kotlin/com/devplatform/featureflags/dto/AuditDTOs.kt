package com.devplatform.featureflags.dto

import com.devplatform.featureflags.domain.enums.AuditAction
import java.time.LocalDateTime

/**
 * Response DTO for audit log entry
 */
data class AuditLogResponse(
    val id: Long,
    val featureId: Long,
    val featureKey: String,
    val featureName: String,
    val action: AuditAction,
    val actionDescription: String,
    val changedBy: String,
    val timestamp: LocalDateTime,
    val oldValue: Map<String, Any>?,
    val newValue: Map<String, Any>?,
    val reason: String?
)

/**
 * Request DTO for creating an audit log
 */
data class CreateAuditLogRequest(
    val action: AuditAction,
    val changedBy: String,
    val oldValue: Map<String, Any>? = null,
    val newValue: Map<String, Any>? = null,
    val reason: String? = null
)
