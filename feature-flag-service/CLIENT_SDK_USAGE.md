# Feature Flag Client SDK - Usage Guide

## Overview

The Feature Flag Client SDK provides a simple way for services to evaluate feature flags without needing to understand the underlying implementation details.

## Installation

Add the `common-lib` dependency to your service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":common-lib"))
}
```

## Configuration

Add the following configuration to your `application.yml`:

```yaml
feature-flags:
  client:
    base-url: http://localhost:8083  # URL of the feature flag service
    environment: production            # Current environment
    default-value: false              # Default value when evaluation fails
    cache-enabled: true               # Enable result caching
    cache-ttl-seconds: 60             # Cache TTL
```

## Basic Usage

### 1. Inject the Client

```kotlin
import com.devplatform.common.featureflag.FeatureFlagClient
import org.springframework.stereotype.Service

@Service
class MyService(
    private val featureFlagClient: FeatureFlagClient
) {
    // Your service logic
}
```

### 2. Check if a Feature is Enabled

```kotlin
fun processCheckout() {
    if (featureFlagClient.isFeatureEnabled("new_checkout_flow")) {
        // Use new checkout flow
        processNewCheckout()
    } else {
        // Use old checkout flow
        processOldCheckout()
    }
}
```

### 3. Evaluate with User Context

```kotlin
fun showFeature(userId: String) {
    if (featureFlagClient.isFeatureEnabled(
        featureKey = "premium_features",
        userId = userId,
        userAttributes = mapOf(
            "country" to "US",
            "userType" to "premium"
        )
    )) {
        // Show premium features
    }
}
```

### 4. Batch Evaluation

Evaluate multiple features at once for better performance:

```kotlin
fun loadDashboard(userId: String) {
    val features = featureFlagClient.evaluateFeatures(
        featureKeys = listOf(
            "new_dashboard",
            "analytics_widget",
            "beta_features"
        ),
        userId = userId
    )

    if (features["new_dashboard"] == true) {
        // Show new dashboard
    }

    if (features["analytics_widget"] == true) {
        // Show analytics widget
    }
}
```

## Advanced Usage

### Custom Environment

Override the default environment for specific checks:

```kotlin
val isEnabledInStaging = featureFlagClient.isFeatureEnabled(
    featureKey = "experimental_feature",
    environment = "staging"
)
```

### User Attributes for Targeting

Pass user attributes to leverage targeting rules:

```kotlin
val isEnabled = featureFlagClient.isFeatureEnabled(
    featureKey = "localized_content",
    userId = user.id,
    userAttributes = mapOf(
        "country" to user.country,
        "language" to user.language,
        "subscriptionTier" to user.tier,
        "accountAge" to user.accountAgeDays
    )
)
```

## Best Practices

### 1. Feature Key Naming Convention

Use lowercase with underscores:
- ✅ `new_checkout_flow`
- ✅ `enable_analytics`
- ❌ `NewCheckoutFlow`
- ❌ `enable-analytics`

### 2. Default to Safe Behavior

Always default to the safer/older implementation when a feature is disabled:

```kotlin
if (featureFlagClient.isFeatureEnabled("risky_new_feature")) {
    // New, potentially risky code
} else {
    // Proven, safe code path
}
```

### 3. Cache Feature Checks

If you're checking the same feature multiple times in a request, cache the result:

```kotlin
class RequestContext {
    private val featureCache = mutableMapOf<String, Boolean>()

    fun isFeatureEnabled(featureKey: String): Boolean {
        return featureCache.getOrPut(featureKey) {
            featureFlagClient.isFeatureEnabled(featureKey, userId)
        }
    }
}
```

### 4. Handle Failures Gracefully

The client automatically returns the `defaultValue` on errors. Ensure your default value is set appropriately:

```yaml
feature-flags:
  client:
    default-value: false  # Safer for new features
```

### 5. Use Batch Evaluation for Multiple Checks

When checking multiple features at once, use batch evaluation to reduce network calls:

```kotlin
// ❌ Bad: Multiple network calls
val feature1 = featureFlagClient.isFeatureEnabled("feature1")
val feature2 = featureFlagClient.isFeatureEnabled("feature2")
val feature3 = featureFlagClient.isFeatureEnabled("feature3")

// ✅ Good: Single network call
val features = featureFlagClient.evaluateFeatures(
    listOf("feature1", "feature2", "feature3")
)
```

## Example: Gradual Rollout

### Step 1: Create Feature with 0% Rollout

```kotlin
// Initially disabled for everyone
```

### Step 2: Enable for Internal Testing (10%)

```kotlin
// Update rollout to 10% in the dashboard
// Only 10% of users will see the feature
```

### Step 3: Increase Rollout Gradually

```kotlin
// 25% -> 50% -> 75% -> 100%
// Monitor metrics at each stage
```

### Step 4: Remove Feature Flag

Once the feature is stable and at 100% rollout, remove the flag from code:

```kotlin
// Before (with flag)
if (featureFlagClient.isFeatureEnabled("new_feature")) {
    newImplementation()
} else {
    oldImplementation()
}

// After (flag removed)
newImplementation()
```

## Troubleshooting

### Feature Always Returns False

1. Check that the feature exists in the dashboard
2. Verify the feature is enabled
3. Ensure there are rules for your environment
4. Check rollout percentage
5. Verify user attributes match targeting rules

### Client Connection Errors

1. Verify the feature flag service URL: `feature-flags.client.base-url`
2. Ensure the feature flag service is running
3. Check network connectivity
4. Review service logs for detailed error messages

### Unexpected Evaluation Results

1. Check the audit logs in the dashboard to see recent changes
2. Verify the environment matches: `feature-flags.client.environment`
3. Review rule priorities (higher priority rules are evaluated first)
4. Check user attributes being passed

## Performance Considerations

- **Caching**: The client caches evaluation results for 60 seconds by default
- **Batch Evaluation**: Use batch evaluation for multiple features
- **Async Evaluation**: Consider evaluating features asynchronously if not needed immediately
- **Circuit Breaker**: The client returns `defaultValue` on failures, preventing cascading failures

## Monitoring

Monitor feature flag evaluations to track:
- Feature adoption rates
- Rollout progress
- Evaluation errors
- Cache hit rates

Check the Evaluations tab in the dashboard for real-time statistics.
