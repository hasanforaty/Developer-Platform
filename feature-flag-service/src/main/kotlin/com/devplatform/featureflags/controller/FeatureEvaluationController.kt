package com.devplatform.featureflags.controller

import com.devplatform.featureflags.dto.*
import com.devplatform.featureflags.service.FeatureEvaluationService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * REST Controller for Feature Flag evaluation
 * This is the main API used by client SDKs to check if features are enabled
 */
@RestController
@RequestMapping("/api/v1/evaluate")
class FeatureEvaluationController(
    private val evaluationService: FeatureEvaluationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluate a single feature flag
     * This is the main evaluation endpoint used by client applications
     */
    @PostMapping
    fun evaluateFeature(@Valid @RequestBody request: EvaluationRequest): ResponseEntity<ApiResponse<EvaluationResponse>> {
        return try {
            val result = evaluationService.evaluateFeature(request)
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Error evaluating feature: ${request.featureKey}", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to evaluate feature", "EVALUATION_ERROR"))
        }
    }

    /**
     * Evaluate a single feature flag (simplified GET endpoint)
     * This allows for simple GET requests without a request body
     */
    @GetMapping
    fun evaluateFeatureSimple(
        @RequestParam featureKey: String,
        @RequestParam environment: String,
        @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<EvaluationResponse>> {
        return try {
            val request = EvaluationRequest(
                featureKey = featureKey,
                environment = environment,
                userId = userId
            )
            val result = evaluationService.evaluateFeature(request)
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Error evaluating feature: $featureKey", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to evaluate feature", "EVALUATION_ERROR"))
        }
    }

    /**
     * Evaluate multiple features at once
     * This is more efficient than making multiple individual requests
     */
    @PostMapping("/batch")
    fun evaluateBatch(@Valid @RequestBody request: BatchEvaluationRequest): ResponseEntity<ApiResponse<BatchEvaluationResponse>> {
        return try {
            val result = evaluationService.evaluateBatch(request)
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Error evaluating features batch", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to evaluate features", "EVALUATION_ERROR"))
        }
    }

    /**
     * Get evaluation statistics for a feature
     */
    @GetMapping("/stats/{featureId}")
    fun getEvaluationStats(
        @PathVariable featureId: Long,
        @RequestParam(required = false, defaultValue = "24") hoursBack: Long
    ): ResponseEntity<ApiResponse<EvaluationStatsResponse>> {
        return try {
            val since = LocalDateTime.now().minusHours(hoursBack)
            val stats = evaluationService.getEvaluationStats(featureId, since)
            ResponseEntity.ok(ApiResponse.success(stats))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(404)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error fetching evaluation stats", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch stats", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get recent evaluations for a feature (for debugging)
     */
    @GetMapping("/history/{featureId}")
    fun getRecentEvaluations(@PathVariable featureId: Long): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        return try {
            val evaluations = evaluationService.getRecentEvaluations(featureId).take(50).map { eval ->
                mapOf(
                    "id" to (eval.id ?: 0),
                    "userId" to (eval.userId ?: "anonymous"),
                    "environment" to eval.environment,
                    "result" to eval.result,
                    "timestamp" to eval.timestamp,
                    "ruleId" to eval.rule?.id
                )
            }
            ResponseEntity.ok(ApiResponse.success(evaluations))
        } catch (e: Exception) {
            logger.error("Error fetching evaluation history", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch evaluation history", "INTERNAL_ERROR"))
        }
    }
}
