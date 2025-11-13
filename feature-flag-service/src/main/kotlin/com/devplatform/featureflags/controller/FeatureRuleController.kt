package com.devplatform.featureflags.controller

import com.devplatform.featureflags.dto.CreateRuleRequest
import com.devplatform.featureflags.dto.Mappers.toResponse
import com.devplatform.featureflags.dto.RuleResponse
import com.devplatform.featureflags.dto.UpdateRuleRequest
import com.devplatform.featureflags.service.FeatureRuleService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Feature Flag Rules management
 */
@RestController
@RequestMapping("/api/v1/features")
class FeatureRuleController(
    private val ruleService: FeatureRuleService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new rule for a feature
     */
    @PostMapping("/{featureId}/rules")
    fun createRule(
        @PathVariable featureId: Long,
        @Valid @RequestBody request: CreateRuleRequest,
        @RequestParam(required = false) createdBy: String?
    ): ResponseEntity<ApiResponse<RuleResponse>> {
        return try {
            val rule = ruleService.createRule(featureId, request, createdBy ?: "system")
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(rule.toResponse(), "Rule created successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.message ?: "Invalid request", "INVALID_REQUEST"))
        } catch (e: Exception) {
            logger.error("Error creating rule", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to create rule", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get rule by ID
     */
    @GetMapping("/rules/{ruleId}")
    fun getRuleById(@PathVariable ruleId: Long): ResponseEntity<ApiResponse<RuleResponse>> {
        return try {
            val rule = ruleService.getRuleById(ruleId)
            ResponseEntity.ok(ApiResponse.success(rule.toResponse()))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Rule not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error fetching rule", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch rule", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get all rules for a feature
     */
    @GetMapping("/{featureId}/rules")
    fun getRulesByFeature(@PathVariable featureId: Long): ResponseEntity<ApiResponse<List<RuleResponse>>> {
        return try {
            val rules = ruleService.getRulesByFeature(featureId).map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(rules))
        } catch (e: Exception) {
            logger.error("Error fetching rules", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch rules", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get enabled rules for a feature in a specific environment
     */
    @GetMapping("/{featureId}/rules/enabled")
    fun getEnabledRules(
        @PathVariable featureId: Long,
        @RequestParam environment: String
    ): ResponseEntity<ApiResponse<List<RuleResponse>>> {
        return try {
            val rules = ruleService.getEnabledRulesByFeatureAndEnvironment(featureId, environment)
                .map { it.toResponse() }
            ResponseEntity.ok(ApiResponse.success(rules))
        } catch (e: Exception) {
            logger.error("Error fetching enabled rules", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch enabled rules", "INTERNAL_ERROR"))
        }
    }

    /**
     * Update a rule
     */
    @PutMapping("/rules/{ruleId}")
    fun updateRule(
        @PathVariable ruleId: Long,
        @Valid @RequestBody request: UpdateRuleRequest,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<ApiResponse<RuleResponse>> {
        return try {
            val rule = ruleService.updateRule(ruleId, request, updatedBy ?: "system")
            ResponseEntity.ok(ApiResponse.success(rule.toResponse(), "Rule updated successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Rule not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error updating rule", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to update rule", "INTERNAL_ERROR"))
        }
    }

    /**
     * Delete a rule
     */
    @DeleteMapping("/rules/{ruleId}")
    fun deleteRule(
        @PathVariable ruleId: Long,
        @RequestParam(required = false) deletedBy: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        return try {
            ruleService.deleteRule(ruleId, deletedBy ?: "system")
            ResponseEntity.ok(ApiResponse.success(Unit, "Rule deleted successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Rule not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error deleting rule", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to delete rule", "INTERNAL_ERROR"))
        }
    }

    /**
     * Toggle rule enabled state
     */
    @PostMapping("/rules/{ruleId}/toggle")
    fun toggleRule(
        @PathVariable ruleId: Long,
        @RequestParam(required = false) changedBy: String?
    ): ResponseEntity<ApiResponse<RuleResponse>> {
        return try {
            val rule = ruleService.toggleRule(ruleId, changedBy ?: "system")
            ResponseEntity.ok(ApiResponse.success(rule.toResponse(), "Rule toggled successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Rule not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error toggling rule", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to toggle rule", "INTERNAL_ERROR"))
        }
    }
}
