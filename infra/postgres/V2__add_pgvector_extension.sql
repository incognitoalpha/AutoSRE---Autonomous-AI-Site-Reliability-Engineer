-- V2__add_pgvector_extension.sql
-- Enables pgvector extension for vector similarity search

CREATE EXTENSION IF NOT EXISTS vector CASCADE;

-- Verify extension is available
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'pgvector extension not available';
    END IF;
END $$;

COMMENT ON EXTENSION vector IS 'pgvector vector similarity search extension';