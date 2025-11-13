-- Log Aggregation Schema
-- Version: 1.0
-- Description: Initial schema for centralized log collection and search

-- Log Entries table: Store all log messages
CREATE TABLE log_entries (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    service_name VARCHAR(100) NOT NULL,
    level VARCHAR(10) NOT NULL,
    logger_name VARCHAR(255),
    thread_name VARCHAR(100),
    message TEXT NOT NULL,
    exception_class VARCHAR(255),
    stack_trace TEXT,
    trace_id VARCHAR(64),
    span_id VARCHAR(16),
    metadata JSONB,

    CONSTRAINT chk_log_level CHECK (level IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'))
);

-- Indexes for efficient log searching
CREATE INDEX idx_log_entries_timestamp ON log_entries(timestamp DESC);
CREATE INDEX idx_log_entries_service_name ON log_entries(service_name);
CREATE INDEX idx_log_entries_level ON log_entries(level);
CREATE INDEX idx_log_entries_trace_id ON log_entries(trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX idx_log_entries_service_time ON log_entries(service_name, timestamp DESC);
CREATE INDEX idx_log_entries_service_level ON log_entries(service_name, level);

-- GIN index for full-text search on message
CREATE INDEX idx_log_entries_message_gin ON log_entries USING gin(to_tsvector('english', message));

-- GIN index for JSONB metadata queries
CREATE INDEX idx_log_entries_metadata ON log_entries USING gin(metadata);

-- Log Statistics table: Aggregated statistics
CREATE TABLE log_statistics (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    level VARCHAR(10) NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_log_statistics UNIQUE(service_name, date, level)
);

CREATE INDEX idx_log_statistics_service_date ON log_statistics(service_name, date DESC);

-- Log Retention Policy table: Configure retention per service
CREATE TABLE log_retention_policies (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    retention_days INTEGER NOT NULL DEFAULT 30,
    archive_enabled BOOLEAN NOT NULL DEFAULT false,
    archive_location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_retention_days CHECK (retention_days > 0 AND retention_days <= 365)
);

-- Trigger for auto-updating updated_at
CREATE TRIGGER update_log_retention_policies_updated_at BEFORE UPDATE ON log_retention_policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Default retention policy
INSERT INTO log_retention_policies (service_name, retention_days, archive_enabled)
VALUES ('default', 30, false);

-- Comments
COMMENT ON TABLE log_entries IS 'Centralized log storage for all microservices';
COMMENT ON TABLE log_statistics IS 'Daily aggregated log statistics per service and level';
COMMENT ON TABLE log_retention_policies IS 'Retention and archival policies per service';
