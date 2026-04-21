-- =============================================
-- V8: Add group chat table
-- =============================================

CREATE TABLE group_chat (
    id         BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL,
    username   VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_group_chat_group_name ON group_chat(group_name);
