ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

CREATE TABLE IF NOT EXISTS telegram_link_tokens (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_tlt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
