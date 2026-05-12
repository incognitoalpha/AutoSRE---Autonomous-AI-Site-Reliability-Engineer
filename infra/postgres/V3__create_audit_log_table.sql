-- V3__create_audit_log_table.sql
-- Creates the audit log table for tracking all agent actions

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action_type VARCHAR(100) NOT NULL,
    target_service VARCHAR(255) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_ms BIGINT NOT NULL DEFAULT 0,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX idx_audit_log_target_service ON audit_log(target_service);
CREATE INDEX idx_audit_log_executed_at ON audit_log(executed_at DESC);
CREATE INDEX idx_audit_log_agent_id ON audit_log(agent_id);

COMMENT ON TABLE audit_log IS 'Immutable audit trail of all AutoSRE agent actions';
COMMENT ON COLUMN audit_log.outcome IS 'SUCCESS, FAILURE, or PARTIAL';