package com.devplatform.featureflags.domain.enums

enum class AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    ENABLED,
    DISABLED,
    RULE_ADDED,
    RULE_UPDATED,
    RULE_DELETED;

    fun getDescription(): String {
        return when (this) {
            CREATED -> "Feature flag created"
            UPDATED -> "Feature flag updated"
            DELETED -> "Feature flag deleted"
            ENABLED -> "Feature flag enabled"
            DISABLED -> "Feature flag disabled"
            RULE_ADDED -> "Rule added to feature flag"
            RULE_UPDATED -> "Rule updated"
            RULE_DELETED -> "Rule deleted"
        }
    }
}
