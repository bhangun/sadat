CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_memory (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT,
    embedding vector(1536), -- OpenAI ada-002 dimension
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vector_memory_session ON vector_memory(session_id, tenant_id);
CREATE INDEX idx_vector_memory_embedding ON vector_memory 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- For faster similarity search
CREATE INDEX idx_vector_memory_hnsw ON vector_memory 
    USING hnsw (embedding vector_cosine_ops);