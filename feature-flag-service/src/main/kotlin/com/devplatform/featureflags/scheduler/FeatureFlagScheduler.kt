package com.devplatform.featureflags.scheduler

import com.devplatform.featureflags.service.FeatureEvaluationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Scheduled tasks for Feature Flag service
 */
@Component
class FeatureFlagScheduler(
    private val evaluationService: FeatureEvaluationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Clean up old evaluation records daily at 3 AM
     * Keeps only the last 30 days of evaluation data
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupOldEvaluations() {
        logger.info("Starting cleanup of old evaluation records")

        try {
            val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
            val deletedCount = evaluationService.cleanupOldEvaluations(thirtyDaysAgo)

            logger.info("Cleaned up $deletedCount old evaluation records (older than $thirtyDaysAgo)")
        } catch (e: Exception) {
            logger.error("Error during evaluation cleanup", e)
        }
    }

    /**
     * Log statistics about feature flag usage every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    fun logStatistics() {
        logger.info("Feature flag service is running - hourly check")
        // Future: Add metrics collection here
    }
}
