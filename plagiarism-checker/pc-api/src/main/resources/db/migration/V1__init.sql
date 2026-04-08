CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ──────────────── Users ────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL DEFAULT 'STUDENT',   -- STUDENT | INSTRUCTOR | ADMIN
    full_name     TEXT,
    created_at    TIMESTAMPTZ DEFAULT now()
);

-- Default admin user (password: Admin@123 — bcrypt hash)
INSERT INTO users (email, password_hash, role, full_name)
VALUES ('admin@pc.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMIN', 'System Administrator');

-- ──────────────── Submissions ────────────────
CREATE TABLE submissions (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename      TEXT NOT NULL,
    storage_key   TEXT NOT NULL,           -- MinIO object key
    sha256_hash   TEXT NOT NULL,           -- tamper-proof receipt
    user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    job_id        UUID,
    version       INT DEFAULT 1,           -- re-submission version counter
    status        TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING|PROCESSING|DONE|FAILED
    assignment_id TEXT,                    -- optional grouping key
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_submissions_user_id ON submissions(user_id);
CREATE INDEX idx_submissions_status  ON submissions(status);

-- ──────────────── Similarity Results ────────────────
CREATE TABLE similarity_results (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doc_a_id        UUID REFERENCES submissions(id) ON DELETE CASCADE,
    doc_b_id        UUID REFERENCES submissions(id) ON DELETE CASCADE,
    tfidf_score     NUMERIC(5,4) DEFAULT 0,
    simhash_score   NUMERIC(5,4) DEFAULT 0,
    semantic_score  NUMERIC(5,4) DEFAULT 0,
    ai_probability  NUMERIC(5,4) DEFAULT 0,   -- 0=human, 1=AI
    final_score     NUMERIC(5,4) DEFAULT 0,
    algorithm       TEXT DEFAULT 'TFIDF+SIMHASH+SEMANTIC',
    matched_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_results_doc_a ON similarity_results(doc_a_id);
CREATE INDEX idx_results_doc_b ON similarity_results(doc_b_id);

-- ──────────────── Matched Spans ────────────────
CREATE TABLE matched_spans (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id     UUID REFERENCES similarity_results(id) ON DELETE CASCADE,
    doc_id        UUID REFERENCES submissions(id) ON DELETE CASCADE,
    start_char    INT NOT NULL,
    end_char      INT NOT NULL,
    matched_text  TEXT,
    source_type   TEXT DEFAULT 'CORPUS',   -- CORPUS | WEB
    source_url    TEXT                     -- populated for WEB matches
);

CREATE INDEX idx_spans_result_id ON matched_spans(result_id);

-- ──────────────── Annotations ────────────────
CREATE TABLE annotations (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id     UUID REFERENCES similarity_results(id) ON DELETE CASCADE,
    reviewer_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    span_start    INT,
    verdict       TEXT,   -- confirmed | dismissed | grey_area
    note          TEXT,
    created_at    TIMESTAMPTZ DEFAULT now()
);

-- ──────────────── Assignment Webhooks ────────────────
CREATE TABLE assignment_webhooks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assignment_id   TEXT UNIQUE NOT NULL,
    webhook_url     TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);
