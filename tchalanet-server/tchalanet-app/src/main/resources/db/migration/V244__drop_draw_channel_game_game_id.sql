-- Phase B: drop legacy game_id column from draw_channel_game.
-- Phase A (V242) backfilled tenant_game_id from game_id; all Java code now uses tenant_game_id.

ALTER TABLE draw_channel_game DROP COLUMN IF EXISTS game_id;
ALTER TABLE draw_channel_game_aud DROP COLUMN IF EXISTS game_id;
