package com.devplatform.featureflags.service

import com.devplatform.featureflags.domain.entity.Feature
import com.devplatform.featureflags.domain.entity.FeatureEvaluation
import com.devplatform.featureflags.domain.entity.FeatureRule
import com.devplatform.featureflags.dto.BatchEvaluationRequest
import com.devplatform.featureflags.dto.BatchEvaluationResponse
import com.devplatform.featureflags.dto.EvaluationRequest
import com.devplatform.featureflags.dto.EvaluationResponse
import com.devplatform.featureflags.dto.EvaluationStatsResponse
import com.devplatform.featureflags.repository.FeatureEvaluationRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * Service for evaluating feature flags
 * This is the core evaluation engine that determines if a feature should be enabled
 */
@Service
@Transactional
class FeatureEvaluationService(
    private val featureManagementService: FeatureManagementService,
    private val ruleService: FeatureRuleService,
    private val evaluationRepository: FeatureEvaluationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Evaluate a feature flag for a user/context
     * This is the main evaluation method used by client SDKs
     */
    fun evaluateFeature(request: EvaluationRequest): EvaluationResponse {
        logger.debug("Evaluating feature: ${request.featureKey} for user: ${request.userId} in environment: ${request.environment}")

        return try {
            val feature = featureManagementService.getFeatureByKeyWithRules(request.featureKey)

            // Check if feature is globally disabled
            if (!feature.enabled) {
                val response = EvaluationResponse(
                    featureKey = request.featureKey,
                    enabled = false,
                    reason = "Feature is globally disabled"
                )
                recordEvaluation(feature, request, response, null)
                return response
            }

            // Get enabled rules for this environment
            val rules = ruleService.getEnabledRulesByFeatureAndEnvironment(
                feature.id!!,
                request.environment
            )

            if (rules.isEmpty()) {
                val response = EvaluationResponse(
                    featureKey = request.featureKey,
                    enabled = false,
                    reason = "No enabled rules for environment '${request.environment}'"
                )
                recordEvaluation(feature, request, response, null)
                return response
            }

            // Evaluate rules in priority order (already sorted by repository)
            for (rule in rules) {
                val matches = evaluateRule(rule, request)
                if (matches) {
                    val response = EvaluationResponse(
                        featureKey = request.featureKey,
                        enabled = true,
                        ruleId = rule.id,
                        ruleName = rule.name,
                        reason = "Matched rule: ${rule.name}"
                    )
                    recordEvaluation(feature, request, response, rule)
                    return response
                }
            }

            // No rules matched
            val response = EvaluationResponse(
                featureKey = request.featureKey,
                enabled = false,
                reason = "No rules matched"
            )
            recordEvaluation(feature, request, response, null)
            return response

        } catch (e: NoSuchElementException) {
            logger.warn("Feature not found: ${request.featureKey}")
            return EvaluationResponse(
                featureKey = request.featureKey,
                enabled = false,
                reason = "Feature not found"
            )
        }
    }

    /**
     * Evaluate multiple features in a single request
     */
    fun evaluateBatch(request: BatchEvaluationRequest): BatchEvaluationResponse {
        val results = request.featureKeys.associateWith { featureKey ->
            evaluateFeature(
                EvaluationRequest(
                    featureKey = featureKey,
                    userId = request.userId,
                    environment = request.environment,
                    userAttributes = request.userAttributes
                )
            )
        }

        return BatchEvaluationResponse(results = results)
    }

    /**
     * Evaluate a single rule against the request context
     */
    private fun evaluateRule(rule: FeatureRule, request: EvaluationRequest): Boolean {
        // Check user segment targeting
        if (!rule.matchesUserSegment(request.userAttributes)) {
            logger.debug("Rule ${rule.name} does not match user segment")
            return false
        }

        // Check rollout percentage
        if (request.userId != null) {
            val matchesRollout = rule.matchesRolloutPercentage(request.userId, request.featureKey)
            logger.debug("Rule ${rule.name} rollout match: $matchesRollout (${rule.rolloutPercentage}%)")
            return matchesRollout
        }

        // If no user ID, check if rollout is 100%
        return rule.rolloutPercentage == 100
    }

    /**
     * Record the evaluation result asynchronously
     */
    @Async
    fun recordEvaluation(
        feature: Feature,
        request: EvaluationRequest,
        response: EvaluationResponse,
        rule: FeatureRule?
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                val evaluation = FeatureEvaluation(
                    feature = feature,
                    userId = request.userId,
                    environment = request.environment,
                    result = response.enabled,
                    rule = rule,
                    metadata = request.metadata
                )

                evaluationRepository.save(evaluation)
            } catch (e: Exception) {
                logger.error("Failed to record evaluation for feature ${feature.key}", e)
            }
        }
    }

    /**
     * Get evaluation statistics for a feature
     */
    @Transactional(readOnly = true)
    fun getEvaluationStats(featureId: Long, since: LocalDateTime): EvaluationStatsResponse {
        val feature = featureManagementService.getFeatureById(featureId)

        val totalEvaluations = evaluationRepository.countByFeatureId(featureId)
        val enabledCount = evaluationRepository.countByFeatureIdAndResult(featureId, true)
        val disabledCount = evaluationRepository.countByFeatureIdAndResult(featureId, false)

        val enabledPercentage = if (totalEvaluations > 0) {
            (enabledCount.toDouble() / totalEvaluations.toDouble()) * 100.0
        } else {
            0.0
        }

        return EvaluationStatsResponse(
            featureId = featureId,
            featureKey = feature.key,
            totalEvaluations = totalEvaluations,
            enabledCount = enabledCount,
            disabledCount = disabledCount,
            enabledPercentage = enabledPercentage,
            period = "since $since"
        )
    }

    /**
     * Get recent evaluations for a feature
     */
    @Transactional(readOnly = true)
    fun getRecentEvaluations(featureId: Long): List<FeatureEvaluation> {
        return evaluationRepository.findRecentEvaluationsByFeature(featureId)
    }

    /**
     * Delete old evaluations (cleanup)
     */
    fun cleanupOldEvaluations(before: LocalDateTime): Long {
        logger.info("Cleaning up evaluations before $before")
        return evaluationRepository.deleteByTimestampBefore(before)
    }
}
