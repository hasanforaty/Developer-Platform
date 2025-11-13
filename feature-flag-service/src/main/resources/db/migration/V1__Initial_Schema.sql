-- Feature Flag Service Schema
-- Version: 1.0
-- Description: Initial schema for feature flag management

-- Features table: Feature flag definitions
CREATE TABLE features (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_features_enabled ON features(enabled);
CREATE INDEX idx_features_key ON features(key);

-- Feature Rules table: Targeting and rollout rules
CREATE TABLE feature_rules (
    id BIGSERIAL PRIMARY KEY,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    environment VARCHAR(50) NOT NULL DEFAULT 'production',
    rollout_percentage INTEGER NOT NULL DEFAULT 0,
    user_segment JSONB,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_rollout_percentage CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100),
    CONSTRAINT chk_environment CHECK (environment IN ('development', 'staging', 'production', 'test'))
);

CREATE INDEX idx_feature_rules_feature_id ON feature_rules(feature_id);
CREATE INDEX idx_feature_rules_environment ON feature_rules(environment);
CREATE INDEX idx_feature_rules_enabled ON feature_rules(enabled);

-- Feature Evaluations table: Track flag evaluations
CREATE TABLE feature_evaluations (
    id BIGSERIAL PRIMARY KEY,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    user_id VARCHAR(100),
    environment VARCHAR(50) NOT NULL,
    result BOOLEAN NOT NULL,
    rule_id BIGINT REFERENCES feature_rules(id) ON DELETE SET NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_feature_evaluations_feature_id ON feature_evaluations(feature_id);
CREATE INDEX idx_feature_evaluations_timestamp ON feature_evaluations(timestamp DESC);
CREATE INDEX idx_feature_evaluations_user_id ON feature_evaluations(user_id);

-- Feature Audit table: Track all feature flag changes
CREATE TABLE feature_audit (
    id BIGSERIAL PRIMARY KEY,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value JSONB,
    new_value JSONB,
    reason TEXT,

    CONSTRAINT chk_action CHECK (action IN ('CREATED', 'UPDATED', 'DELETED', 'ENABLED', 'DISABLED', 'RULE_ADDED', 'RULE_UPDATED', 'RULE_DELETED'))
);

CREATE INDEX idx_feature_audit_feature_id ON feature_audit(feature_id);
CREATE INDEX idx_feature_audit_timestamp ON feature_audit(timestamp DESC);
CREATE INDEX idx_feature_audit_changed_by ON feature_audit(changed_by);

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_features_updated_at BEFORE UPDATE ON features
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_feature_rules_updated_at BEFORE UPDATE ON feature_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE features IS 'Feature flag definitions';
COMMENT ON TABLE feature_rules IS 'Targeting and rollout rules for feature flags';
COMMENT ON TABLE feature_evaluations IS 'Historical record of feature flag evaluations';
COMMENT ON TABLE feature_audit IS 'Audit trail for all feature flag changes';
