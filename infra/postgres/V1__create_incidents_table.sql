-- V1__create_incidents_table.sql
-- Creates the main incidents table for storing completed incident records

CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id VARCHAR(255) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    root_cause TEXT,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    embedding VECTOR(1536),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_service_id ON incidents(service_id);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_detected_at ON incidents(detected_at DESC);

COMMENT ON TABLE incidents IS 'Stores completed incidents with root cause analysis';
COMMENT ON COLUMN incidents.root_cause IS 'AI-generated root cause description from RCA';
COMMENT ON COLUMN incidents.embedding IS 'pgvector embedding for similarity search';