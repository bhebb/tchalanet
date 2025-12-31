-- V45: seed tenant_game rows for tenant 'tchalanet'
DO $$ BEGIN
  RAISE NOTICE 'V45__seed_tenant_game: assigning games to tenant tchalanet';
END $$;

-- Insert or update tenant_game per (tenant_id, game_id)
WITH t AS (
  SELECT id AS tenant_id FROM tenant WHERE code = 'tchalanet' LIMIT 1
), g AS (
  SELECT id AS game_id, code AS game_code, name AS game_name FROM game
  WHERE code IN ('US_NY_PICK3','US_NY_PICK4','US_NY_TAKE5','US_FL_PICK3','US_FL_PICK4','US_FL_LOTTO')
)
INSERT INTO tenant_game (id, tenant_id, game_id, enabled, display_name, flags, min_stake, max_stake)
SELECT gen_random_uuid(), t.tenant_id, g.game_id, true, g.game_name, '{}'::jsonb, NULL, NULL
FROM t CROSS JOIN g
ON CONFLICT (tenant_id, game_id) DO UPDATE
  SET enabled = EXCLUDED.enabled,
      display_name = EXCLUDED.display_name,
      flags = COALESCE(EXCLUDED.flags, tenant_game.flags),
      min_stake = COALESCE(EXCLUDED.min_stake, tenant_game.min_stake),
      max_stake = COALESCE(EXCLUDED.max_stake, tenant_game.max_stake);

-- Sanity check
DO $$
DECLARE cnt int; t_id uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE EXCEPTION 'V45__seed_tenant_game sanity check failed: tenant tchalanet not found';
  END IF;
  SELECT count(*) INTO cnt FROM tenant_game WHERE tenant_id = t_id AND deleted_at IS NULL;
  RAISE NOTICE 'V45__seed_tenant_game: tenant % has % tenant_game rows', t_id, cnt;
END $$;

