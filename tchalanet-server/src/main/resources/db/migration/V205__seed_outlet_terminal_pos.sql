-- V47: seed outlet, terminal and optional sales_session for tenant 'tchalanet'
-- Flyway RLS context for default tenant seed
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$ BEGIN
  RAISE NOTICE 'V47__seed_outlet_terminal_pos: seeding outlet, terminal and sales_session for tenant tchalanet';
END $$;

-- Outlet
INSERT INTO outlet (id, tenant_id, name, slug, timezone, receipt_printing_enabled, require_opening_float)
SELECT
    '00000000-0000-0000-0000-000000003001'::uuid,
    t.id,
    'PDV Principal',
    'pdv-principal',
    'America/Toronto',
    true,
    true
FROM tenant t
WHERE t.code = 'tchalanet'
  AND NOT EXISTS (
    SELECT 1 FROM outlet o WHERE o.tenant_id = t.id AND o.slug = 'pdv-principal' AND o.deleted_at IS NULL
  );

-- Terminal
INSERT INTO terminal (id, tenant_id, outlet_id, state, label, inventory_tag, metadata)
SELECT
    '00000000-0000-0000-0000-000000003101'::uuid,
    t.id,
    '00000000-0000-0000-0000-000000003001'::uuid,
    'ACTIVE',
    'POS-01',
    'TCH-POS-01',
    '{"app":"tch-pos","platform":"android"}'::jsonb
FROM tenant t
WHERE t.code = 'tchalanet'
  AND NOT EXISTS (
    SELECT 1 FROM terminal tr WHERE tr.tenant_id = t.id AND tr.outlet_id = '00000000-0000-0000-0000-000000003001'::uuid AND tr.label = 'POS-01' AND tr.deleted_at IS NULL
  );

-- SALES_SESSION optional (only if an app_user exists)
DO $$
DECLARE u uuid; t_id uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V47__seed_outlet_terminal_pos: tenant tchalanet not found, skipping sales_session';
    RETURN;
  END IF;

  -- Trouver un user associé au tenant via tenant_user (app_user n'a pas de colonne tenant_id)
  SELECT tu.user_id INTO u
  FROM tenant_user tu
  JOIN app_user au ON au.id = tu.user_id
  WHERE tu.tenant_id = t_id
    AND au.deleted_at IS NULL
    AND tu.deleted_at IS NULL
  ORDER BY tu.created_at ASC
  LIMIT 1;

  IF u IS NULL THEN
    RAISE NOTICE 'V47__seed_outlet_terminal_pos: no app_user found for tenant, skipping sales_session';
    RETURN;
  END IF;

  IF EXISTS (
    SELECT 1 FROM sales_session ps WHERE ps.tenant_id = t_id AND ps.terminal_id = '00000000-0000-0000-0000-000000003101'::uuid AND ps.status = 'OPEN' AND ps.deleted_at IS NULL
  ) THEN
    RAISE NOTICE 'V47__seed_outlet_terminal_pos: existing open sales_session found, skipping';
    RETURN;
  END IF;

  INSERT INTO sales_session (id, tenant_id, outlet_id, terminal_id, user_id, status, opening_float, closing_amount, meta)
  VALUES (
    '00000000-0000-0000-0000-000000003201'::uuid,
    t_id,
    '00000000-0000-0000-0000-000000003001'::uuid,
    '00000000-0000-0000-0000-000000003101'::uuid,
    u,
    'OPEN',
    100.00,
    0.00,
    '{}'::jsonb
  );

  RAISE NOTICE 'V47__seed_outlet_terminal_pos: sales_session created for tenant % user %', t_id, u;
END $$;

-- Sanity check
DO $$
DECLARE cnt int; t_id uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NOT NULL THEN
    SELECT count(*) INTO cnt FROM outlet WHERE tenant_id = t_id AND deleted_at IS NULL;
    RAISE NOTICE 'V47__seed_outlet_terminal_pos sanity: tenant % has % outlets', t_id, cnt;
  END IF;
END $$;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
