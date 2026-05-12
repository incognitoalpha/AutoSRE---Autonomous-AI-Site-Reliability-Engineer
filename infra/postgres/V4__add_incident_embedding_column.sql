-- V4__add_incident_embedding_column.sql
-- Adds the embedding column to incidents table for RAG similarity search

-- Add embedding column if not exists (allows for migration from V1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'incidents' AND column_name = 'embedding'
    ) THEN
        ALTER TABLE incidents ADD COLUMN embedding VECTOR(1536);
    END IF;
END $$;

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS idx_incidents_embedding
ON incidents USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

COMMENT ON COLUMN incidents.embedding IS 'Anthropic embeddings vector for semantic similarity search';