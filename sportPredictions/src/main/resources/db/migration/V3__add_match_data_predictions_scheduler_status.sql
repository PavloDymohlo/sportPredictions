-- =============================================
-- V3: Replace Redis with PostgreSQL storage
-- =============================================

CREATE TABLE IF NOT EXISTS match_data (
    id         BIGSERIAL PRIMARY KEY,
    match_date DATE        NOT NULL UNIQUE,
    data       TEXT        NOT NULL,
    fetched_at TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS predictions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT    NOT NULL,
    match_date       DATE      NOT NULL,
    predictions_data TEXT      NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    CONSTRAINT uq_predictions_user_date UNIQUE (user_id, match_date),
    CONSTRAINT fk_predictions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS scheduler_status (
    id          BIGINT      PRIMARY KEY DEFAULT 1,
    status      VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETE',
    last_run_at VARCHAR(50),
    CONSTRAINT chk_scheduler_singleton CHECK (id = 1)
);

INSERT INTO scheduler_status (id, status, last_run_at)
VALUES (1, 'INCOMPLETE', NULL)
ON CONFLICT DO NOTHING;