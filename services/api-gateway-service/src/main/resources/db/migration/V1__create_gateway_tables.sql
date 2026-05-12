-- Flyway migration for api-gateway-service tables
CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    root_cause TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_incidents_service_id ON incidents(service_id);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_detected_at ON incidents(detected_at DESC);

CREATE TABLE IF NOT EXISTS pending_approvals (
    id BIGSERIAL PRIMARY KEY,
    plan_id VARCHAR(255) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    actions_json TEXT NOT NULL,
    approval_tier VARCHAR(50) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL,
    approved_at BIGINT,
    rejected_at BIGINT,
    decided_by VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_pending_approvals_plan_id ON pending_approvals(plan_id);
CREATE INDEX idx_pending_approvals_status ON pending_approvals(status);
CREATE INDEX idx_pending_approvals_created_at ON pending_approvals(created_at DESC);