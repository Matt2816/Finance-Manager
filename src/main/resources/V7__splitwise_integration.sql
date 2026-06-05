-- Splitwise integration: encrypted credentials, import state, sync runs, ShedLock

ALTER TABLE users ADD COLUMN IF NOT EXISTS splitwise_api_key_enc BYTEA;
ALTER TABLE users ADD COLUMN IF NOT EXISTS splitwise_api_key_iv BYTEA;
ALTER TABLE users ADD COLUMN IF NOT EXISTS splitwise_group_names TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS splitwise_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS splitwise_import_state (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_poll_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_error TEXT,
    splitwise_user_id BIGINT
);

CREATE TABLE IF NOT EXISTS splitwise_sync_runs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    imported_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    deleted_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    error_log VARCHAR(4000)
);

CREATE INDEX IF NOT EXISTS idx_splitwise_sync_runs_user_started
    ON splitwise_sync_runs (user_id, started_at DESC);

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
