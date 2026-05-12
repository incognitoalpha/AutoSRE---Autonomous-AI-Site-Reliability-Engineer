#!/usr/bin/env python3
"""
Seed runbooks into pgvector for RAG-based root cause analysis.
Reads markdown files from src/main/resources/runbooks/,
chunks them by heading, embeds using Anthropic API,
and stores in PostgreSQL with pgvector extension.
"""

import os
import sys
import json
import psycopg2
from typing import List, Optional
from anthropic import Anthropic

# Configuration from environment
DB_HOST = os.getenv("AUTOSRE_DB_HOST", "localhost")
DB_PORT = os.getenv("AUTOSRE_DB_PORT", "5432")
DB_NAME = os.getenv("AUTOSRE_DB_NAME", "autosre")
DB_USER = os.getenv("AUTOSRE_DB_USER", "postgres")
DB_PASSWORD = os.getenv("AUTOSRE_DB_PASSWORD", "postgres")
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY", "")

ANTHROPIC_MODEL = "claude-sonnet-4-20250514"
CHUNK_SIZE = 1000  # Characters per chunk
CHUNK_OVERLAP = 100


def get_anthropic_client() -> Optional[Anthropic]:
    """Initialize Anthropic client."""
    if not ANTHROPIC_API_KEY:
        print("WARNING: ANTHROPIC_API_KEY not set, using mock embeddings")
        return None
    return Anthropic(api_key=ANTHROPIC_API_KEY)


def embed_text(client: Optional[Anthropic], text: str) -> List[float]:
    """Generate embedding for text using Anthropic."""
    if client is None:
        # Mock embedding for testing
        return [0.0] * 1536

    response = client.embeddings.create(
        model="claude-embedding-3-haiku-20241107",
        input=text
    )
    return response.embedding


def chunk_markdown(content: str) -> List[str]:
    """Split markdown into chunks by headings."""
    chunks = []
    lines = content.split('\n')
    current_chunk = []

    for line in lines:
        if line.startswith('#'):
            if current_chunk:
                chunk_text = '\n'.join(current_chunk).strip()
                if chunk_text:
                    chunks.append(chunk_text)
                current_chunk = []
        current_chunk.append(line)

    if current_chunk:
        chunk_text = '\n'.join(current_chunk).strip()
        if chunk_text:
            chunks.append(chunk_text)

    return chunks


def get_runbook_files(base_path: str) -> List[str]:
    """Find all markdown runbook files."""
    runbook_files = []
    for root, _, files in os.walk(base_path):
        for file in files:
            if file.endswith('.md'):
                runbook_files.append(os.path.join(root, file))
    return runbook_files


def init_database():
    """Initialize database connection."""
    return psycopg2.connect(
        host=DB_HOST,
        port=int(DB_PORT),
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD
    )


def create_table(conn):
    """Create runbook_chunks table with vector column."""
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS runbook_chunks (
            id SERIAL PRIMARY KEY,
            runbook_name VARCHAR(255) NOT NULL,
            chunk_index INTEGER NOT NULL,
            content TEXT NOT NULL,
            embedding vector(1536),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_runbook_chunks_embedding
        ON runbook_chunks USING ivfflat (embedding vector_cosine_ops)
        WITH (lists = 100)
    """)
    conn.commit()
    cursor.close()


def store_chunk(conn, runbook_name: str, chunk_index: int, content: str, embedding: List[float]):
    """Store a chunk with its embedding."""
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO runbook_chunks (runbook_name, chunk_index, content, embedding)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT DO NOTHING
    """, (runbook_name, chunk_index, content, embedding))
    conn.commit()
    cursor.close()


def seed_runbooks(base_path: str):
    """Seed all runbooks into the database."""
    client = get_anthropic_client()
    conn = init_database()
    create_table(conn)

    runbook_files = get_runbook_files(base_path)
    print(f"Found {len(runbook_files)} runbook files")

    for file_path in runbook_files:
        runbook_name = os.path.basename(file_path)
        print(f"Processing: {runbook_name}")

        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        chunks = chunk_markdown(content)
        print(f"  Created {len(chunks)} chunks")

        for i, chunk in enumerate(chunks):
            embedding = embed_text(client, chunk)
            store_chunk(conn, runbook_name, i, chunk, embedding)
            print(f"  Stored chunk {i+1}/{len(chunks)}")

    conn.close()
    print("Seeding complete!")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        base_path = sys.argv[1]
    else:
        base_path = "services/ai-agent-service/src/main/resources/runbooks"

    if not os.path.exists(base_path):
        print(f"Error: Path not found: {base_path}")
        sys.exit(1)

    seed_runbooks(base_path)