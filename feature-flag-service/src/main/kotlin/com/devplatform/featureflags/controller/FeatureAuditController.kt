package com.devplatform.featureflags.controller

import com.devplatform.featureflags.domain.enums.AuditAction
import com.devplatform.featureflags.dto.AuditLogResponse
import com.devplatform.featureflags.dto.Mappers.toResponse
import com.devplatform.featureflags.service.FeatureAuditService
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * REST Controller for Feature Flag audit logs
 */
@RestController
@RequestMapping("/api/v1/audit")
class FeatureAuditController(
    private val auditService: FeatureAuditService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get all audit logs
     */
    @GetMapping
    fun getAllAuditLogs(): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAllAuditLogs().map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get audit logs for a specific feature
     */
    @GetMapping("/feature/{featureId}")
    fun getAuditLogsByFeature(@PathVariable featureId: Long): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAuditLogsForFeature(featureId).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs for feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get audit logs for a feature by key
     */
    @GetMapping("/feature/key/{featureKey}")
    fun getAuditLogsByFeatureKey(@PathVariable featureKey: String): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAuditLogsForFeatureKey(featureKey).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs for feature key", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get recent audit logs for a feature
     */
    @GetMapping("/feature/{featureId}/recent")
    fun getRecentAuditLogs(
        @PathVariable featureId: Long,
        @RequestParam(required = false, defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getRecentAuditLogsForFeature(featureId, limit).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching recent audit logs", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get audit logs by user
     */
    @GetMapping("/user/{changedBy}")
    fun getAuditLogsByUser(@PathVariable changedBy: String): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAuditLogsByUser(changedBy).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs by user", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get audit logs by action type
     */
    @GetMapping("/action/{action}")
    fun getAuditLogsByAction(@PathVariable action: AuditAction): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAuditLogsByAction(action).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs by action", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get audit logs within a time range
     */
    @GetMapping("/range")
    fun getAuditLogsByTimeRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ApiResponse<List<AuditLogResponse>>> {
        return try {
            val logs = auditService.getAuditLogsBetween(from, to).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(logs))
        } catch (e: Exception) {
            logger.error("Error fetching audit logs by time range", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch audit logs", "INTERNAL_ERROR"))
        }
    }
}
