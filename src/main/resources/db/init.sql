CREATE EXTENSION IF NOT EXISTS vector;

-- 文档全文检索 + 向量字段（演示用，简化版）
CREATE TABLE IF NOT EXISTS documents (
    id          TEXT PRIMARY KEY,
    content     TEXT NOT NULL,
    embedding   vector(1024),
    metadata    JSONB DEFAULT '{}'::jsonb,
    tsv         tsvector,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_documents_tsv ON documents USING GIN(tsv);
CREATE INDEX IF NOT EXISTS idx_documents_embedding ON documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE OR REPLACE FUNCTION documents_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.tsv := to_tsvector('simple', coalesce(NEW.content, ''));
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_documents_tsv ON documents;
CREATE TRIGGER trg_documents_tsv BEFORE INSERT OR UPDATE
    ON documents FOR EACH ROW EXECUTE FUNCTION documents_tsv_trigger();

-- 长期记忆
CREATE TABLE IF NOT EXISTS long_term_memory (
    id          TEXT PRIMARY KEY,
    user_id     TEXT NOT NULL,
    content     TEXT NOT NULL,
    embedding   vector(1024),
    created_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ltm_user ON long_term_memory(user_id);
CREATE INDEX IF NOT EXISTS idx_ltm_embedding ON long_term_memory USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- LLM 调用成本表
CREATE TABLE IF NOT EXISTS llm_cost (
    id              BIGSERIAL PRIMARY KEY,
    ts              TIMESTAMPTZ NOT NULL DEFAULT now(),
    session_id      TEXT,
    model           TEXT NOT NULL,
    input_tokens    INT  NOT NULL DEFAULT 0,
    output_tokens   INT  NOT NULL DEFAULT 0,
    cost_yuan       NUMERIC(12,6) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_llm_cost_ts ON llm_cost(ts DESC);
