-- A/B Testing Service Schema
-- Version: 1.0
-- Description: Initial schema for A/B testing and experimentation

-- Experiments table: A/B test definitions
CREATE TABLE experiments (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    hypothesis TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    traffic_percentage INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    CONSTRAINT chk_experiment_status CHECK (status IN ('DRAFT', 'RUNNING', 'PAUSED', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT chk_traffic_percentage CHECK (traffic_percentage >= 0 AND traffic_percentage <= 100),
    CONSTRAINT chk_experiment_dates CHECK (start_date IS NULL OR end_date IS NULL OR start_date < end_date)
);

CREATE INDEX idx_experiments_status ON experiments(status);
CREATE INDEX idx_experiments_key ON experiments(key);
CREATE INDEX idx_experiments_dates ON experiments(start_date, end_date);

-- Variants table: Different versions being tested
CREATE TABLE variants (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    allocation_percentage INTEGER NOT NULL,
    is_control BOOLEAN NOT NULL DEFAULT false,
    config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_variant_key UNIQUE(experiment_id, key),
    CONSTRAINT chk_allocation_percentage CHECK (allocation_percentage >= 0 AND allocation_percentage <= 100)
);

CREATE INDEX idx_variants_experiment_id ON variants(experiment_id);
CREATE INDEX idx_variants_is_control ON variants(is_control);

-- User Assignments table: Track which users got which variant
CREATE TABLE user_assignments (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES variants(id) ON DELETE CASCADE,
    user_id VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_sticky BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT uq_user_assignment UNIQUE(experiment_id, user_id)
);

CREATE INDEX idx_assignments_experiment_id ON user_assignments(experiment_id);
CREATE INDEX idx_assignments_variant_id ON user_assignments(variant_id);
CREATE INDEX idx_assignments_user_id ON user_assignments(user_id);

-- Metrics table: Define what to measure
CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    metric_type VARCHAR(50) NOT NULL,
    aggregation VARCHAR(50) NOT NULL DEFAULT 'COUNT',
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_metric_key UNIQUE(experiment_id, key),
    CONSTRAINT chk_metric_type CHECK (metric_type IN ('CONVERSION', 'REVENUE', 'DURATION', 'COUNT', 'CUSTOM')),
    CONSTRAINT chk_aggregation CHECK (aggregation IN ('COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'MEDIAN'))
);

CREATE INDEX idx_metrics_experiment_id ON metrics(experiment_id);
CREATE INDEX idx_metrics_is_primary ON metrics(is_primary);

-- Metric Events table: Raw event data
CREATE TABLE metric_events (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES variants(id) ON DELETE CASCADE,
    metric_id BIGINT NOT NULL REFERENCES metrics(id) ON DELETE CASCADE,
    user_id VARCHAR(100) NOT NULL,
    value NUMERIC(20, 4),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_metric_events_experiment_id ON metric_events(experiment_id);
CREATE INDEX idx_metric_events_variant_id ON metric_events(variant_id);
CREATE INDEX idx_metric_events_metric_id ON metric_events(metric_id);
CREATE INDEX idx_metric_events_timestamp ON metric_events(timestamp DESC);
CREATE INDEX idx_metric_events_user_id ON metric_events(user_id);

-- Experiment Results table: Aggregated results
CREATE TABLE experiment_results (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES variants(id) ON DELETE CASCADE,
    metric_id BIGINT NOT NULL REFERENCES metrics(id) ON DELETE CASCADE,
    sample_size BIGINT NOT NULL DEFAULT 0,
    value NUMERIC(20, 4),
    std_deviation NUMERIC(20, 4),
    confidence_interval_lower NUMERIC(20, 4),
    confidence_interval_upper NUMERIC(20, 4),
    p_value NUMERIC(10, 8),
    is_significant BOOLEAN,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_experiment_result UNIQUE(experiment_id, variant_id, metric_id, calculated_at)
);

CREATE INDEX idx_results_experiment_id ON experiment_results(experiment_id);
CREATE INDEX idx_results_variant_id ON experiment_results(variant_id);
CREATE INDEX idx_results_calculated_at ON experiment_results(calculated_at DESC);

-- Experiment Audit table: Track experiment lifecycle
CREATE TABLE experiment_audit (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value JSONB,
    new_value JSONB,
    reason TEXT,

    CONSTRAINT chk_experiment_action CHECK (action IN ('CREATED', 'UPDATED', 'STARTED', 'PAUSED', 'RESUMED', 'STOPPED', 'COMPLETED', 'ARCHIVED'))
);

CREATE INDEX idx_experiment_audit_experiment_id ON experiment_audit(experiment_id);
CREATE INDEX idx_experiment_audit_timestamp ON experiment_audit(timestamp DESC);

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_experiments_updated_at BEFORE UPDATE ON experiments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE experiments IS 'A/B test experiment definitions';
COMMENT ON TABLE variants IS 'Different variants/versions being tested';
COMMENT ON TABLE user_assignments IS 'Tracks which users are assigned to which variants';
COMMENT ON TABLE metrics IS 'Metrics to measure for each experiment';
COMMENT ON TABLE metric_events IS 'Raw event data for metric calculation';
COMMENT ON TABLE experiment_results IS 'Aggregated statistical results per variant';
COMMENT ON TABLE experiment_audit IS 'Audit trail for experiment lifecycle changes';
