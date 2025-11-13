-- Service Registry Schema
-- Version: 1.0
-- Description: Initial schema for service registration and health monitoring

-- Services table: Represents logical services
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    base_url VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    deleted_at TIMESTAMP NULL,

    CONSTRAINT chk_status CHECK (status IN ('UP', 'DOWN', 'DEGRADED', 'UNKNOWN'))
);

CREATE INDEX idx_services_status ON services(status);
CREATE INDEX idx_services_deleted_at ON services(deleted_at);

-- Service Instances table: Multiple instances of a service
CREATE TABLE service_instances (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    instance_id VARCHAR(100) NOT NULL UNIQUE,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'STARTING',
    metadata JSONB,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_instance_status CHECK (status IN ('STARTING', 'UP', 'DOWN', 'UNKNOWN')),
    CONSTRAINT chk_port_range CHECK (port > 0 AND port < 65536)
);

CREATE INDEX idx_service_instances_service_id ON service_instances(service_id);
CREATE INDEX idx_service_instances_status ON service_instances(status);
CREATE INDEX idx_service_instances_last_heartbeat ON service_instances(last_heartbeat);

-- Health Checks table: Historical health check data
CREATE TABLE health_checks (
    id BIGSERIAL PRIMARY KEY,
    instance_id BIGINT NOT NULL REFERENCES service_instances(id) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    response_time_ms INTEGER,
    error_message TEXT,
    details JSONB,

    CONSTRAINT chk_health_status CHECK (status IN ('HEALTHY', 'UNHEALTHY', 'TIMEOUT', 'ERROR'))
);

CREATE INDEX idx_health_checks_instance_id ON health_checks(instance_id);
CREATE INDEX idx_health_checks_timestamp ON health_checks(timestamp DESC);
CREATE INDEX idx_health_checks_status ON health_checks(status);

-- Audit Logs table: Track all changes
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for auto-updating updated_at
CREATE TRIGGER update_services_updated_at BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_service_instances_updated_at BEFORE UPDATE ON service_instances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE services IS 'Logical services registered in the platform';
COMMENT ON TABLE service_instances IS 'Physical instances of services (for horizontal scaling)';
COMMENT ON TABLE health_checks IS 'Historical health check results for monitoring';
COMMENT ON TABLE audit_logs IS 'Audit trail for all entity changes';
