-- =============================================
-- V7: Add indexes on foreign key columns
-- =============================================

CREATE INDEX idx_gt_group_id           ON group_tournaments(group_id);
CREATE INDEX idx_gt_winner_user_id     ON group_tournaments(winner_user_id);

CREATE INDEX idx_gc_tournament_id      ON group_competitions(tournament_id);
CREATE INDEX idx_gc_competition_id     ON group_competitions(competition_id);

CREATE INDEX idx_gus_tournament_id     ON group_user_statistics(tournament_id);
CREATE INDEX idx_gus_user_id           ON group_user_statistics(user_id);

CREATE INDEX idx_uc_user_id            ON user_competitions(user_id);
CREATE INDEX idx_uc_competition_id     ON user_competitions(competition_id);

CREATE INDEX idx_ugm_group_id          ON user_group_members(group_id);
CREATE INDEX idx_ugm_user_id           ON user_group_members(user_id);

CREATE INDEX idx_gjr_group_id          ON group_join_requests(group_id);
CREATE INDEX idx_gjr_user_id           ON group_join_requests(user_id);

CREATE INDEX idx_tlt_user_id           ON telegram_link_tokens(user_id);
