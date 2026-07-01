-- V248: Seed tenant-admin feature permissions introduced after the app split.
-- This is deliberately additive/idempotent and replaces manual bootstrap repair calls.

INSERT INTO permission (code, name, category, system, active) VALUES
  -- Tenant configuration and company data
  ('tenant.address.read',    'Read tenant address',           'tenant',    true, true),
  ('tenant.address.manage',  'Manage tenant address',         'tenant',    true, true),
  ('tenant.config.read',     'Read tenant configuration',      'tenant',    true, true),
  ('tenant.config.manage',   'Manage tenant configuration',    'tenant',    true, true),
  -- Existing tenant admin feature surfaces, re-seeded for already migrated databases
  ('settings.read',          'Read settings',                  'settings',  true, true),
  ('settings.update',        'Update settings',                'settings',  true, true),
  ('limit.read',             'Read limits',                    'limit',     true, true),
  ('limit.manage',           'Manage limits',                  'limit',     true, true),
  ('promotion.read',         'Read promotions',                'promotion', true, true),
  ('promotion.manage',       'Manage promotions',              'promotion', true, true),
  ('theme.read',             'Read tenant theme',              'theme',     true, true),
  ('theme.manage',           'Manage tenant theme',            'theme',     true, true)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  category = EXCLUDED.category,
  system = EXCLUDED.system,
  active = EXCLUDED.active,
  updated_at = now();

-- TENANT_ADMIN: operational admin surface.
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000302'::uuid, unnest(ARRAY[
  'tenant.address.read',
  'tenant.address.manage',
  'tenant.config.read',
  'tenant.config.manage',
  'settings.read',
  'settings.update',
  'limit.read',
  'limit.manage',
  'promotion.read',
  'promotion.manage',
  'theme.read',
  'theme.manage'
]) ON CONFLICT DO NOTHING;

-- TENANT_OWNER: full tenant owner surface.
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000305'::uuid, unnest(ARRAY[
  'tenant.address.read',
  'tenant.address.manage',
  'tenant.config.read',
  'tenant.config.manage',
  'settings.read',
  'settings.update',
  'limit.read',
  'limit.manage',
  'promotion.read',
  'promotion.manage',
  'theme.read',
  'theme.manage'
]) ON CONFLICT DO NOTHING;

-- SUPER_ADMIN: lets platform support/admin inspect and correct tenant setup when scoped to a tenant.
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000301'::uuid, unnest(ARRAY[
  'tenant.address.read',
  'tenant.address.manage',
  'tenant.config.read',
  'tenant.config.manage',
  'settings.read',
  'settings.update',
  'limit.read',
  'limit.manage',
  'promotion.read',
  'promotion.manage',
  'theme.read',
  'theme.manage'
]) ON CONFLICT DO NOTHING;

DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM role_permission
    WHERE role_id = '00000000-0000-0000-0000-000000000302'::uuid
      AND permission_code = 'theme.read'
  ) THEN
    RAISE EXCEPTION 'V248 sanity: TENANT_ADMIN missing theme.read';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM role_permission
    WHERE role_id = '00000000-0000-0000-0000-000000000302'::uuid
      AND permission_code = 'promotion.manage'
  ) THEN
    RAISE EXCEPTION 'V248 sanity: TENANT_ADMIN missing promotion.manage';
  END IF;

  RAISE NOTICE 'V248 OK: tenant admin feature permissions seeded';
END $$;
