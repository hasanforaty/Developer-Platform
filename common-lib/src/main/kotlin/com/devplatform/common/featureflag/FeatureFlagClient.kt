package com.devplatform.common.featureflag

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Client for evaluating feature flags
 * This is a lightweight client that can be used by any service to check if features are enabled
 */
@Component
class FeatureFlagClient(
    private val config: FeatureFlagClientConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(config.baseUrl)
        .build()

    /**
     * Check if a feature is enabled for the current context
     *
     * @param featureKey The unique key of the feature
     * @param userId Optional user ID for user-specific evaluation
     * @param environment The environment (development, staging, production, test)
     * @param userAttributes Additional attributes for targeting rules
     * @return true if the feature is enabled, false otherwise
     */
    fun isFeatureEnabled(
        featureKey: String,
        userId: String? = null,
        environment: String = config.environment,
        userAttributes: Map<String, Any> = emptyMap()
    ): Boolean {
        return try {
            val request = EvaluationRequest(
                featureKey = featureKey,
                userId = userId,
                environment = environment,
                userAttributes = userAttributes
            )

            val response = restClient.post()
                .uri("/api/v1/evaluate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body<ApiResponse<EvaluationResponse>>()

            val result = response?.data?.enabled ?: config.defaultValue

            logger.debug("Feature '$featureKey' evaluation: $result (reason: ${response?.data?.reason})")

            result
        } catch (e: Exception) {
            logger.error("Error evaluating feature '$featureKey', returning default value: ${config.defaultValue}", e)
            config.defaultValue
        }
    }

    /**
     * Evaluate multiple features at once
     *
     * @param featureKeys List of feature keys to evaluate
     * @param userId Optional user ID
     * @param environment The environment
     * @param userAttributes Additional attributes
     * @return Map of feature keys to their enabled status
     */
    fun evaluateFeatures(
        featureKeys: List<String>,
        userId: String? = null,
        environment: String = config.environment,
        userAttributes: Map<String, Any> = emptyMap()
    ): Map<String, Boolean> {
        return try {
            val request = BatchEvaluationRequest(
                featureKeys = featureKeys,
                userId = userId,
                environment = environment,
                userAttributes = userAttributes
            )

            val response = restClient.post()
                .uri("/api/v1/evaluate/batch")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body<ApiResponse<BatchEvaluationResponse>>()

            response?.data?.results?.mapValues { it.value.enabled } ?: emptyMap()
        } catch (e: Exception) {
            logger.error("Error evaluating features batch, returning empty map", e)
            emptyMap()
        }
    }
}

/**
 * Configuration for Feature Flag Client
 */
@Component
@ConfigurationProperties(prefix = "feature-flags.client")
data class FeatureFlagClientConfig(
    /**
     * Base URL of the feature flag service
     * Default: http://localhost:8083
     */
    var baseUrl: String = "http://localhost:8083",

    /**
     * Current environment
     * Default: production
     */
    var environment: String = "production",

    /**
     * Default value to return when evaluation fails
     * Default: false (feature disabled on error)
     */
    var defaultValue: Boolean = false,

    /**
     * Enable caching of evaluations
     * Default: true
     */
    var cacheEnabled: Boolean = true,

    /**
     * Cache TTL in seconds
     * Default: 60
     */
    var cacheTtlSeconds: Long = 60
)

/**
 * Data classes for API communication
 */
private data class EvaluationRequest(
    val featureKey: String,
    val userId: String?,
    val environment: String,
    val userAttributes: Map<String, Any>,
    val metadata: Map<String, Any>? = null
)

private data class BatchEvaluationRequest(
    val featureKeys: List<String>,
    val userId: String?,
    val environment: String,
    val userAttributes: Map<String, Any>
)

private data class EvaluationResponse(
    val featureKey: String,
    val enabled: Boolean,
    val ruleId: Long?,
    val ruleName: String?,
    val reason: String
)

private data class BatchEvaluationResponse(
    val results: Map<String, EvaluationResponse>
)

private data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: Map<String, Any>?
)
