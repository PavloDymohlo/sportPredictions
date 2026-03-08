-- =============================================
-- V6: Add ON DELETE CASCADE / SET NULL to all FK constraints
-- =============================================

-- user_group_members: cascade when group or user deleted
ALTER TABLE user_group_members
    DROP CONSTRAINT fk_ugm_group,
    ADD CONSTRAINT fk_ugm_group FOREIGN KEY (group_id) REFERENCES user_groups (id) ON DELETE CASCADE;

ALTER TABLE user_group_members
    DROP CONSTRAINT fk_ugm_user,
    ADD CONSTRAINT fk_ugm_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- group_tournaments: cascade when group deleted; set null when winner user deleted
ALTER TABLE group_tournaments
    DROP CONSTRAINT fk_gt_group,
    ADD CONSTRAINT fk_gt_group FOREIGN KEY (group_id) REFERENCES user_groups (id) ON DELETE CASCADE;

ALTER TABLE group_tournaments
    DROP CONSTRAINT fk_gt_winner,
    ADD CONSTRAINT fk_gt_winner FOREIGN KEY (winner_user_id) REFERENCES users (id) ON DELETE SET NULL;

-- group_competitions: cascade when tournament or competition deleted
ALTER TABLE group_competitions
    DROP CONSTRAINT fk_gc_tournament,
    ADD CONSTRAINT fk_gc_tournament FOREIGN KEY (tournament_id) REFERENCES group_tournaments (id) ON DELETE CASCADE;

ALTER TABLE group_competitions
    DROP CONSTRAINT fk_gc_competition,
    ADD CONSTRAINT fk_gc_competition FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE;

-- group_user_statistics: cascade when tournament or user deleted
ALTER TABLE group_user_statistics
    DROP CONSTRAINT fk_gus_tournament,
    ADD CONSTRAINT fk_gus_tournament FOREIGN KEY (tournament_id) REFERENCES group_tournaments (id) ON DELETE CASCADE;

ALTER TABLE group_user_statistics
    DROP CONSTRAINT fk_gus_user,
    ADD CONSTRAINT fk_gus_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- user_competitions: cascade when user or competition deleted
ALTER TABLE user_competitions
    DROP CONSTRAINT fk_uc_user,
    ADD CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_competitions
    DROP CONSTRAINT fk_uc_competition,
    ADD CONSTRAINT fk_uc_competition FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE;

-- group_join_requests: cascade when group or user deleted
ALTER TABLE group_join_requests
    DROP CONSTRAINT fk_gjr_group,
    ADD CONSTRAINT fk_gjr_group FOREIGN KEY (group_id) REFERENCES user_groups (id) ON DELETE CASCADE;

ALTER TABLE group_join_requests
    DROP CONSTRAINT fk_gjr_user,
    ADD CONSTRAINT fk_gjr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- predictions: cascade when user deleted
ALTER TABLE predictions
    DROP CONSTRAINT fk_predictions_user,
    ADD CONSTRAINT fk_predictions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
