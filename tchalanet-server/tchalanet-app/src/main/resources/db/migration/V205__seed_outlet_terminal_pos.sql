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
INSERT INTO outlet (id, tenant_id, name, slug, timezone, receipt_printing_enabled, require_opening_float, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000003001'::uuid,
    t.id,
    'PDV Principal',
    'pdv-principal',
    'America/Toronto',
    true,
    true,
    now(),
    now()
FROM tenant t
WHERE t.code = 'tchalanet'
  AND NOT EXISTS (
    SELECT 1 FROM outlet o WHERE o.tenant_id = t.id AND o.slug = 'pdv-principal' AND o.deleted_at IS NULL
  );

-- Terminal
INSERT INTO terminal (id, tenant_id, outlet_id, kind, state, sync_state, label, inventory_tag, metadata, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000003101'::uuid,
    t.id,
    '00000000-0000-0000-0000-000000003001'::uuid,
    'PHYSICAL',
    'ACTIVE',
    'ONLINE',
    'POS-01',
    'TCH-POS-01',
    '{"app":"tch-pos","platform":"android","surface":"POS"}'::jsonb,
    now(),
    now()
FROM tenant t
WHERE t.code = 'tchalanet'
  AND NOT EXISTS (
    SELECT 1 FROM terminal tr WHERE tr.tenant_id = t.id AND tr.outlet_id = '00000000-0000-0000-0000-000000003001'::uuid AND tr.label = 'POS-01' AND tr.deleted_at IS NULL
  );

-- Terminal default capabilities for local/dev smoke tests.
INSERT INTO terminal_capability (id, tenant_id, terminal_id, capability, created_at, updated_at)
SELECT
    gen_random_uuid(),
    t.id,
    '00000000-0000-0000-0000-000000003101'::uuid,
    capability,
    now(),
    now()
FROM tenant t
CROSS JOIN (VALUES
    ('SELL_TICKET'),
    ('PRINT_TICKET'),
    ('REPRINT_TICKET'),
    ('SCAN_TICKET'),
    ('PAYOUT_CLAIM'),
    ('OFFLINE_SELL'),
    ('OFFLINE_SYNC')
) AS c(capability)
WHERE t.code = 'tchalanet'
ON CONFLICT (tenant_id, terminal_id, capability) DO NOTHING;

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

  INSERT INTO terminal_assignment (
    id, tenant_id, terminal_id, user_id, status, assigned_at, created_at, updated_at
  )
  VALUES (
    '00000000-0000-0000-0000-000000003111'::uuid,
    t_id,
    '00000000-0000-0000-0000-000000003101'::uuid,
    u,
    'ACTIVE',
    now(),
    now(),
    now()
  )
  ON CONFLICT DO NOTHING;

  UPDATE terminal
  SET assigned_user_id = u,
      updated_at = now()
  WHERE tenant_id = t_id
    AND id = '00000000-0000-0000-0000-000000003101'::uuid
    AND assigned_user_id IS NULL;

  INSERT INTO terminal_binding (
    id, tenant_id, terminal_id, binding_type, status,
    binding_public_key, binding_secret_hash, device_fingerprint_hash,
    bound_at, created_at, updated_at
  )
  VALUES (
    '00000000-0000-0000-0000-000000003121'::uuid,
    t_id,
    '00000000-0000-0000-0000-000000003101'::uuid,
    'POS_DEVICE',
    'ACTIVE',
    'local-dev-public-key',
    '4d0e4221f2911bd2acd91765ec95b6dba87b965e5ecd2218f0a7b3ed638e10cf',
    'local-dev-device-fingerprint-hash',
    now(),
    now(),
    now()
  )
  ON CONFLICT DO NOTHING;

  IF EXISTS (
    SELECT 1 FROM sales_session ps WHERE ps.tenant_id = t_id AND ps.terminal_id = '00000000-0000-0000-0000-000000003101'::uuid AND ps.status = 'OPEN' AND ps.deleted_at IS NULL
  ) THEN
    RAISE NOTICE 'V47__seed_outlet_terminal_pos: existing open sales_session found, skipping';
    RETURN;
  END IF;

  INSERT INTO sales_session (
    id, tenant_id, outlet_id, terminal_id,
    opened_by, opened_at, business_date,
    status, opening_float_cents,
    created_at, updated_at
  )
  VALUES (
    '00000000-0000-0000-0000-000000003201'::uuid,
    t_id,
    '00000000-0000-0000-0000-000000003001'::uuid,
    '00000000-0000-0000-0000-000000003101'::uuid,
    u,
    now(),
    CURRENT_DATE,
    'OPEN',
    10000,
    now(),
    now()
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
