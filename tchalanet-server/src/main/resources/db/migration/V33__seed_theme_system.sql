-- V43: seed system themes (tenant_id IS NULL)
DO $$ BEGIN
  RAISE NOTICE 'V43__seed_theme_system: seeding system themes';
END $$;

-- Insère dans les colonnes présentes au moment de cette migration (code, name, definition)
INSERT INTO theme (id, tenant_id, code, name, definition, active, created_at, updated_at)
SELECT v.id, NULL::uuid, v.code, v.name, v.definition::jsonb, true, now(), now()
FROM (
  VALUES
    ('00000000-0000-0000-0000-000000000401'::uuid,'default-light','Default Light','{"basePresetId":"default-light","cssVars":{},"mode":"LIGHT","tokens":{}}'),
    ('00000000-0000-0000-0000-000000000402'::uuid,'default-dark','Default Dark','{"basePresetId":"default-dark","cssVars":{},"mode":"DARK","tokens":{}}')
) AS v(id, code, name, definition)
WHERE NOT EXISTS (
  SELECT 1 FROM theme t WHERE t.id = v.id
);

-- Sanity check
DO $$
DECLARE cnt int;
BEGIN
  SELECT count(*) INTO cnt FROM theme WHERE tenant_id IS NULL;
  IF cnt < 1 THEN
    RAISE EXCEPTION 'V43__seed_theme_system sanity check failed: no system theme found';
  ELSE
    RAISE NOTICE 'V43__seed_theme_system sanity check OK: % system themes present', cnt;
  END IF;
END $$;
