package com.devplatform.featureflags.controller

import com.devplatform.featureflags.dto.CreateFeatureRequest
import com.devplatform.featureflags.dto.FeatureResponse
import com.devplatform.featureflags.dto.FeatureSummary
import com.devplatform.featureflags.dto.Mappers.toResponse
import com.devplatform.featureflags.dto.Mappers.toSummary
import com.devplatform.featureflags.dto.UpdateFeatureRequest
import com.devplatform.featureflags.service.FeatureManagementService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Feature Flag management
 */
@RestController
@RequestMapping("/api/v1/features")
class FeatureController(
    private val featureService: FeatureManagementService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new feature flag
     */
    @PostMapping
    fun createFeature(@Valid @RequestBody request: CreateFeatureRequest): ResponseEntity<ApiResponse<FeatureResponse>> {
        return try {
            val feature = featureService.createFeature(request)
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(feature.toResponse(), "Feature created successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.message ?: "Invalid request", "INVALID_REQUEST"))
        } catch (e: Exception) {
            logger.error("Error creating feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to create feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get feature by ID
     */
    @GetMapping("/{id}")
    fun getFeatureById(@PathVariable id: Long): ResponseEntity<ApiResponse<FeatureResponse>> {
        return try {
            val feature = featureService.getFeatureById(id)
            ResponseEntity.ok(ApiResponse.success(feature.toResponse()))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error fetching feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get feature by key
     */
    @GetMapping("/key/{key}")
    fun getFeatureByKey(@PathVariable key: String): ResponseEntity<ApiResponse<FeatureResponse>> {
        return try {
            val feature = featureService.getFeatureByKey(key)
            ResponseEntity.ok(ApiResponse.success(feature.toResponse()))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error fetching feature by key", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get all features
     */
    @GetMapping
    fun getAllFeatures(
        @RequestParam(required = false, defaultValue = "false") includeRules: Boolean
    ): ResponseEntity<ApiResponse<List<FeatureResponse>>> {
        return try {
            val features = if (includeRules) {
                featureService.getAllFeaturesWithRules().map { it.toResponse(includeRules = true) }
            } else {
                featureService.getAllFeatures().map { it.toResponse(includeRules = false) }
            }
            ResponseEntity.ok(ApiResponse.success(features))
        } catch (e: Exception) {
            logger.error("Error fetching features", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch features", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get all features as summaries
     */
    @GetMapping("/summary")
    fun getAllFeaturesSummary(): ResponseEntity<ApiResponse<List<FeatureSummary>>> {
        return try {
            val features = featureService.getAllFeatures().map { it.toSummary() }
            ResponseEntity.ok(ApiResponse.success(features))
        } catch (e: Exception) {
            logger.error("Error fetching feature summaries", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch features", "INTERNAL_ERROR"))
        }
    }

    /**
     * Search features
     */
    @GetMapping("/search")
    fun searchFeatures(@RequestParam searchText: String): ResponseEntity<ApiResponse<List<FeatureSummary>>> {
        return try {
            val features = featureService.searchFeatures(searchText).map { it.toSummary() }
            ResponseEntity.ok(ApiResponse.success(features))
        } catch (e: Exception) {
            logger.error("Error searching features", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to search features", "INTERNAL_ERROR"))
        }
    }

    /**
     * Update a feature
     */
    @PutMapping("/{id}")
    fun updateFeature(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateFeatureRequest
    ): ResponseEntity<ApiResponse<FeatureResponse>> {
        return try {
            val feature = featureService.updateFeature(id, request)
            ResponseEntity.ok(ApiResponse.success(feature.toResponse(), "Feature updated successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error updating feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to update feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Delete a feature
     */
    @DeleteMapping("/{id}")
    fun deleteFeature(
        @PathVariable id: Long,
        @RequestParam(required = false) deletedBy: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        return try {
            featureService.deleteFeature(id, deletedBy ?: "system")
            ResponseEntity.ok(ApiResponse.success(Unit, "Feature deleted successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error deleting feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to delete feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Toggle feature enabled state
     */
    @PostMapping("/{id}/toggle")
    fun toggleFeature(
        @PathVariable id: Long,
        @RequestParam(required = false) changedBy: String?
    ): ResponseEntity<ApiResponse<FeatureResponse>> {
        return try {
            val feature = featureService.toggleFeature(id, changedBy ?: "system")
            ResponseEntity.ok(ApiResponse.success(feature.toResponse(), "Feature toggled successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "Feature not found", "NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error toggling feature", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to toggle feature", "INTERNAL_ERROR"))
        }
    }

    /**
     * Get enabled features for an environment
     */
    @GetMapping("/enabled")
    fun getEnabledFeatures(@RequestParam environment: String): ResponseEntity<ApiResponse<List<FeatureSummary>>> {
        return try {
            val features = featureService.getEnabledFeaturesForEnvironment(environment).map { it.toSummary() }
            ResponseEntity.ok(ApiResponse.success(features))
        } catch (e: Exception) {
            logger.error("Error fetching enabled features", e)
            ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Failed to fetch enabled features", "INTERNAL_ERROR"))
        }
    }
}
