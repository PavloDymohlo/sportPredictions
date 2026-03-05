-- =============================================
-- V1: Initial schema for sportPredictions app
-- =============================================

CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,
    user_name               VARCHAR(255),
    ranking_position        BIGINT      NOT NULL DEFAULT 0,
    total_score             BIGINT      NOT NULL DEFAULT 0,
    prediction_count        BIGINT      NOT NULL DEFAULT 0,
    percent_guessed_matches INTEGER     NOT NULL DEFAULT 0,
    password                VARCHAR(255),
    last_predictions        TIMESTAMP,
    language                VARCHAR(5)  NOT NULL DEFAULT 'en'
);

CREATE TABLE IF NOT EXISTS competitions (
    id      BIGSERIAL PRIMARY KEY,
    country VARCHAR(255) NOT NULL,
    name    VARCHAR(255) NOT NULL,
    code    VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT uq_competitions_country_name UNIQUE (country, name)
);

CREATE TABLE IF NOT EXISTS user_groups (
    id               BIGSERIAL PRIMARY KEY,
    group_name       VARCHAR(255) NOT NULL UNIQUE,
    group_leader_id  BIGINT       NOT NULL,
    CONSTRAINT fk_user_groups_leader FOREIGN KEY (group_leader_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS user_group_members (
    group_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_ugm_group FOREIGN KEY (group_id) REFERENCES user_groups (id),
    CONSTRAINT fk_ugm_user  FOREIGN KEY (user_id)  REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS group_tournaments (
    id                  BIGSERIAL PRIMARY KEY,
    group_id            BIGINT      NOT NULL,
    start_date          DATE,
    finish_date         DATE,
    status              VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    winner_user_id      BIGINT,
    last_processed_date DATE,
    CONSTRAINT fk_gt_group  FOREIGN KEY (group_id)       REFERENCES user_groups (id),
    CONSTRAINT fk_gt_winner FOREIGN KEY (winner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS group_competitions (
    id             BIGSERIAL PRIMARY KEY,
    tournament_id  BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    CONSTRAINT uq_gc_tournament_competition UNIQUE (tournament_id, competition_id),
    CONSTRAINT fk_gc_tournament  FOREIGN KEY (tournament_id)  REFERENCES group_tournaments (id),
    CONSTRAINT fk_gc_competition FOREIGN KEY (competition_id) REFERENCES competitions (id)
);

CREATE TABLE IF NOT EXISTS group_user_statistics (
    id                  BIGSERIAL PRIMARY KEY,
    tournament_id       BIGINT  NOT NULL,
    user_id             BIGINT  NOT NULL,
    ranking_position    BIGINT  NOT NULL DEFAULT 0,
    correct_predictions BIGINT  NOT NULL DEFAULT 0,
    prediction_count    BIGINT  NOT NULL DEFAULT 0,
    accuracy_percent    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_gus_tournament_user UNIQUE (tournament_id, user_id),
    CONSTRAINT fk_gus_tournament FOREIGN KEY (tournament_id) REFERENCES group_tournaments (id),
    CONSTRAINT fk_gus_user       FOREIGN KEY (user_id)       REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS user_competitions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    CONSTRAINT uq_uc_user_competition UNIQUE (user_id, competition_id),
    CONSTRAINT fk_uc_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_uc_competition FOREIGN KEY (competition_id) REFERENCES competitions (id)
);

CREATE TABLE IF NOT EXISTS group_join_requests (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    status     VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_gjr_group_user_status UNIQUE (group_id, user_id, status),
    CONSTRAINT fk_gjr_group FOREIGN KEY (group_id) REFERENCES user_groups (id),
    CONSTRAINT fk_gjr_user  FOREIGN KEY (user_id)  REFERENCES users (id)
);
