-- Phase A: migrate draw_channel_game.game_id → tenant_game_id
-- game_id currently points to catalog.game.id; target is tenant_game.id

ALTER TABLE draw_channel_game ADD COLUMN tenant_game_id uuid;

UPDATE draw_channel_game dcg
SET tenant_game_id = tg.id
FROM tenant_game tg
WHERE tg.tenant_id = dcg.tenant_id
  AND tg.game_id   = dcg.game_id;

-- Remove rows with no matching tenant_game (orphaned channel-game links).
DELETE FROM draw_channel_game WHERE tenant_game_id IS NULL;

ALTER TABLE draw_channel_game ALTER COLUMN tenant_game_id SET NOT NULL;

ALTER TABLE draw_channel_game
  ADD CONSTRAINT fk_dcg_tenant_game
  FOREIGN KEY (tenant_game_id) REFERENCES tenant_game(id);

ALTER TABLE draw_channel_game DROP CONSTRAINT uq_draw_channel_game__tenant_channel_game;

ALTER TABLE draw_channel_game
  ADD CONSTRAINT uq_draw_channel_game
  UNIQUE (tenant_id, draw_channel_id, tenant_game_id);

DROP INDEX IF EXISTS ix_dcg_tenant_game;
CREATE INDEX ix_dcg_tenant_tenant_game ON draw_channel_game (tenant_id, tenant_game_id);

-- audit table
ALTER TABLE draw_channel_game_aud ADD COLUMN IF NOT EXISTS tenant_game_id uuid;
