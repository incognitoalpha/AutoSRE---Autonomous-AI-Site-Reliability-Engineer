-- Flyway migration for audit_log table (M5-06)
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    plan_id VARCHAR(255) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    target VARCHAR(255) NOT NULL,
    executor VARCHAR(255) NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_message TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_plan_id ON audit_log(plan_id);
CREATE INDEX idx_audit_log_executed_at ON audit_log(executed_at);