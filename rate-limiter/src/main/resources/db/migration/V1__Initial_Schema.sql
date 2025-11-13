-- Rate Limiter Schema
-- Version: 1.0
-- Description: Initial schema for API rate limiting and usage analytics

-- Rate Limits table: Configuration for rate limits
CREATE TABLE rate_limits (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    endpoint_pattern VARCHAR(255) NOT NULL,
    limit_count INTEGER NOT NULL,
    window_seconds INTEGER NOT NULL,
    strategy VARCHAR(50) NOT NULL DEFAULT 'FIXED_WINDOW',
    enabled BOOLEAN NOT NULL DEFAULT true,
    burst_capacity INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    CONSTRAINT uq_rate_limits UNIQUE(service_name, endpoint_pattern),
    CONSTRAINT chk_limit_count CHECK (limit_count > 0),
    CONSTRAINT chk_window_seconds CHECK (window_seconds > 0),
    CONSTRAINT chk_strategy CHECK (strategy IN ('FIXED_WINDOW', 'SLIDING_WINDOW', 'TOKEN_BUCKET', 'LEAKY_BUCKET'))
);

CREATE INDEX idx_rate_limits_service ON rate_limits(service_name);
CREATE INDEX idx_rate_limits_enabled ON rate_limits(enabled);

-- API Usage table: Track API calls
CREATE TABLE api_usage (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    api_key VARCHAR(100),
    user_id VARCHAR(100),
    request_count INTEGER NOT NULL DEFAULT 1,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    status_code INTEGER,
    ip_address INET,
    user_agent TEXT
);

-- Partitioning by timestamp (monthly) for efficient querying
CREATE INDEX idx_api_usage_timestamp ON api_usage(timestamp DESC);
CREATE INDEX idx_api_usage_service ON api_usage(service_name);
CREATE INDEX idx_api_usage_endpoint ON api_usage(service_name, endpoint);
CREATE INDEX idx_api_usage_api_key ON api_usage(api_key);
CREATE INDEX idx_api_usage_user_id ON api_usage(user_id);

-- API Keys table: Manage API keys for services
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(10) NOT NULL,
    name VARCHAR(200) NOT NULL,
    service_name VARCHAR(100),
    rate_limit_override INTEGER,
    enabled BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_used_at TIMESTAMP,
    usage_count BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_api_keys_enabled ON api_keys(enabled);
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at);
CREATE INDEX idx_api_keys_service ON api_keys(service_name);

-- Rate Limit Violations table: Track violations
CREATE TABLE rate_limit_violations (
    id BIGSERIAL PRIMARY KEY,
    rate_limit_id BIGINT REFERENCES rate_limits(id) ON DELETE SET NULL,
    service_name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    api_key VARCHAR(100),
    user_id VARCHAR(100),
    ip_address INET,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_count INTEGER NOT NULL,
    limit_exceeded_by INTEGER NOT NULL
);

CREATE INDEX idx_violations_timestamp ON rate_limit_violations(timestamp DESC);
CREATE INDEX idx_violations_service ON rate_limit_violations(service_name);
CREATE INDEX idx_violations_api_key ON rate_limit_violations(api_key);
CREATE INDEX idx_violations_ip ON rate_limit_violations(ip_address);

-- API Usage Statistics table: Pre-aggregated metrics
CREATE TABLE api_usage_statistics (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    hour INTEGER NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    avg_response_time_ms INTEGER,
    p95_response_time_ms INTEGER,
    p99_response_time_ms INTEGER,

    CONSTRAINT uq_usage_stats UNIQUE(service_name, endpoint, date, hour),
    CONSTRAINT chk_hour CHECK (hour >= 0 AND hour < 24)
);

CREATE INDEX idx_usage_stats_service_date ON api_usage_statistics(service_name, date DESC);

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_rate_limits_updated_at BEFORE UPDATE ON rate_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE rate_limits IS 'Rate limit configurations per service and endpoint';
COMMENT ON TABLE api_usage IS 'Real-time API usage tracking';
COMMENT ON TABLE api_keys IS 'API key management and tracking';
COMMENT ON TABLE rate_limit_violations IS 'Log of rate limit violations';
COMMENT ON TABLE api_usage_statistics IS 'Pre-aggregated hourly statistics for analytics';
