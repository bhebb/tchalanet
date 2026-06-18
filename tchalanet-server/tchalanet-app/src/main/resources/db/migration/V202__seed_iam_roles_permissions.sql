-- V202: V1 permission catalog, system roles, role-permission matrix, local dev users
-- Replaces the former V42 seed. Volume wipe required when upgrading from pre-V202.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Permission catalog — V1 full set
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO permission (code, name, category, system, active) VALUES
  -- Platform
  ('platform.access',          'Platform access',                'platform', true, true),
  ('platform.ops.read',        'Platform ops read',              'platform', true, true),
  ('platform.ops.execute',     'Execute platform ops',           'platform', true, true),
  -- Tenant management
  ('tenant.create',            'Create tenant',                  'tenant',   true, true),
  ('tenant.read',              'Read tenants',                   'tenant',   true, true),
  ('tenant.update',            'Update tenant',                  'tenant',   true, true),
  ('tenant.activate',          'Activate tenant',                'tenant',   true, true),
  ('tenant.suspend',           'Suspend tenant',                 'tenant',   true, true),
  ('tenant.admin.create',      'Create tenant admin',            'tenant',   true, true),
  ('tenant.override',          'Override tenant context',        'tenant',   true, true),
  -- Admin area
  ('admin.access',             'Tenant admin access',            'admin',    true, true),
  ('dashboard.read',           'Read dashboard',                 'dashboard',true, true),
  -- User management
  ('user.read',                'Read users',                     'user',     true, true),
  ('user.create',              'Create users',                   'user',     true, true),
  ('user.update',              'Update users',                   'user',     true, true),
  ('user.disable',             'Disable users',                  'user',     true, true),
  ('user.invite',              'Invite users',                   'user',     true, true),
  ('user.sync',                'Sync users with identity provider','user',   true, true),
  ('user.membership.manage',   'Manage tenant membership',       'user',     true, true),
  ('user.role.assign',         'Assign user roles',              'user',     true, true),
  ('user.permission.manage',   'Manage user permission overrides','user',    true, true),
  -- Access control
  ('role.read',                'Read roles',                     'access-control', true, true),
  ('role.manage',              'Manage roles',                   'access-control', true, true),
  ('role.permission.manage',   'Manage role permissions',        'access-control', true, true),
  ('permission.read',          'Read permissions',               'access-control', true, true),
  -- Settings / pricing
  ('settings.read',            'Read settings',                  'settings', true, true),
  ('settings.update',          'Update settings',                'settings', true, true),
  ('game-pricing.read',        'Read game pricing',              'pricing',  true, true),
  ('game-pricing.update',      'Update game pricing',            'pricing',  true, true),
  -- Limits / promotions
  ('limit.read',               'Read limits',                    'limit',    true, true),
  ('limit.manage',             'Manage limits',                  'limit',    true, true),
  ('promotion.read',           'Read promotions',                'promotion',true, true),
  ('promotion.manage',         'Manage promotions',              'promotion',true, true),
  -- Reports / audit
  ('report.read',              'Read reports',                   'report',   true, true),
  ('audit.read',               'Read audit log',                 'audit',    true, true),
  -- Cashier / operator area
  ('cashier.access',           'Cashier access',                 'cashier',  true, true),
  ('operator.access',          'Operator access',                'operator', true, true),
  -- Operational context
  ('operational-context.read', 'Read operational context',       'operational-context', true, true),
  ('operational-context.select','Select operational context',    'operational-context', true, true),
  -- Tickets
  ('ticket.sell',              'Sell tickets',                   'ticket',   true, true),
  ('ticket.read',              'Read tickets',                   'ticket',   true, true),
  ('ticket.print',             'Print tickets',                  'ticket',   true, true),
  ('ticket.resend',            'Resend tickets',                 'ticket',   true, true),
  ('ticket.verify',            'Verify tickets',                 'ticket',   true, true),
  ('ticket.cancel-own',        'Cancel own tickets',             'ticket',   true, true),
  ('sync.read',                'Read sync state',                'sync',     true, true),
  ('sync.submit',              'Submit sync',                    'sync',     true, true)
ON CONFLICT (code) DO UPDATE SET
  name   = EXCLUDED.name,
  category = EXCLUDED.category,
  system = EXCLUDED.system,
  active = EXCLUDED.active,
  updated_at = now();

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. System roles
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_role (id, tenant_id, code, name, description, scope, system, custom, active) VALUES
  ('00000000-0000-0000-0000-000000000301'::uuid, NULL, 'SUPER_ADMIN',   'Super Admin',   'System super administrator',      'PLATFORM', true, false, true),
  ('00000000-0000-0000-0000-000000000302'::uuid, NULL, 'TENANT_ADMIN',  'Tenant Admin',  'Tenant-level administrator',      'TENANT',   true, false, true),
  ('00000000-0000-0000-0000-000000000303'::uuid, NULL, 'OPERATOR',      'Operator',      'Outlet operator / supervisor',    'TENANT',   true, false, true),
  ('00000000-0000-0000-0000-000000000304'::uuid, NULL, 'CASHIER',       'Cashier',       'Point-of-sale cashier',           'TENANT',   true, false, true)
-- ON CONFLICT (id) using fixed UUIDs — tenant_id IS NULL cannot be used in ON CONFLICT target
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name, description = EXCLUDED.description,
  scope = EXCLUDED.scope, system = EXCLUDED.system,
  active = EXCLUDED.active, updated_at = now();

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Role-permission matrix (V1 defaults)
-- ─────────────────────────────────────────────────────────────────────────────

-- SUPER_ADMIN
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000301'::uuid, unnest(ARRAY[
  'platform.access','platform.ops.read','platform.ops.execute',
  'tenant.create','tenant.read','tenant.update','tenant.activate','tenant.suspend',
  'tenant.admin.create','tenant.override',
  'audit.read'
]) ON CONFLICT DO NOTHING;

-- TENANT_ADMIN
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000302'::uuid, unnest(ARRAY[
  'admin.access','dashboard.read',
  'user.read','user.create','user.update','user.disable','user.invite','user.sync',
  'user.membership.manage','user.role.assign','user.permission.manage',
  'role.read','permission.read',
  'settings.read','settings.update',
  'game-pricing.read','game-pricing.update',
  'limit.read','limit.manage',
  'promotion.read','promotion.manage',
  'operational-context.read','operational-context.select',
  'report.read'
]) ON CONFLICT DO NOTHING;

-- OPERATOR
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000303'::uuid, unnest(ARRAY[
  'operator.access','dashboard.read',
  'operational-context.read','operational-context.select',
  'ticket.read','ticket.verify','ticket.print','ticket.resend',
  'report.read'
]) ON CONFLICT DO NOTHING;

-- CASHIER
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000304'::uuid, unnest(ARRAY[
  'cashier.access','operational-context.read',
  'ticket.sell','ticket.read','ticket.print','ticket.resend','ticket.verify'
]) ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Local dev / E2E seed users (super_admin, admin, cashier)
--    These users exist only for local dev and E2E runs.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
  t_id uuid;
BEGIN
  RAISE NOTICE 'V202: seeding local dev users';
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;

  -- super_admin
  INSERT INTO app_user (id, username, email, display_name, status, created_at, updated_at)
  VALUES ('00000000-0000-0000-0000-000000010001'::uuid,
          'super_admin', 'super_admin@local', 'Super Admin', 'ACTIVE', now(), now())
  ON CONFLICT (id) DO UPDATE SET
    username = 'super_admin', email = 'super_admin@local',
    display_name = 'Super Admin', status = 'ACTIVE', updated_at = now();

  -- admin (TENANT_ADMIN)
  INSERT INTO app_user (id, username, email, display_name, status, created_at, updated_at)
  VALUES ('00000000-0000-0000-0000-000000010002'::uuid,
          'admin', 'admin@local', 'Admin', 'ACTIVE', now(), now())
  ON CONFLICT (id) DO UPDATE SET
    username = 'admin', email = 'admin@local',
    display_name = 'Admin', status = 'ACTIVE', updated_at = now();

  -- cashier
  INSERT INTO app_user (id, username, email, display_name, status, created_at, updated_at)
  VALUES ('00000000-0000-0000-0000-000000010003'::uuid,
          'cashier', 'cashier@local', 'Cashier', 'ACTIVE', now(), now())
  ON CONFLICT (id) DO UPDATE SET
    username = 'cashier', email = 'cashier@local',
    display_name = 'Cashier', status = 'ACTIVE', updated_at = now();

  INSERT INTO app_user_external_identity (
    app_user_id, provider, issuer, external_subject, email_snapshot, created_at, updated_at)
  VALUES
    ('00000000-0000-0000-0000-000000010001'::uuid, 'LOCAL_JWT', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010001', 'super_admin@local', now(), now()),
    ('00000000-0000-0000-0000-000000010002'::uuid, 'LOCAL_JWT', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010002', 'admin@local', now(), now()),
    ('00000000-0000-0000-0000-000000010003'::uuid, 'LOCAL_JWT', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010003', 'cashier@local', now(), now()),
    ('00000000-0000-0000-0000-000000010001'::uuid, 'LOCAL_PERF', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010001', 'super_admin@local', now(), now()),
    ('00000000-0000-0000-0000-000000010002'::uuid, 'LOCAL_PERF', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010002', 'admin@local', now(), now()),
    ('00000000-0000-0000-0000-000000010003'::uuid, 'LOCAL_PERF', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010003', 'cashier@local', now(), now())
  ON CONFLICT (provider, issuer, external_subject) DO NOTHING;

  IF t_id IS NULL THEN
    RAISE NOTICE 'V202: tenant tchalanet not found, skipping tenant_user and tenant_user_role inserts';
    RETURN;
  END IF;

  -- Set RLS context so tenant_user inserts pass the RLS policy
  PERFORM set_config('app.current_tenant', t_id::text, true);
  PERFORM set_config('app.deleted_visibility', 'active', true);

  -- Tenant membership
  INSERT INTO tenant_user (id, tenant_id, user_id, is_owner, created_at, updated_at)
  VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010001'::uuid, true,  now(), now())
  ON CONFLICT (tenant_id, user_id) DO NOTHING;

  INSERT INTO tenant_user (id, tenant_id, user_id, is_owner, created_at, updated_at)
  VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010002'::uuid, false, now(), now())
  ON CONFLICT (tenant_id, user_id) DO NOTHING;

  INSERT INTO tenant_user (id, tenant_id, user_id, is_owner, created_at, updated_at)
  VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010003'::uuid, false, now(), now())
  ON CONFLICT (tenant_id, user_id) DO NOTHING;

  -- Role assignments via tenant_user_role (no ON CONFLICT — partial unique index not supported)
  IF NOT EXISTS (SELECT 1 FROM tenant_user_role WHERE tenant_id = t_id AND user_id = '00000000-0000-0000-0000-000000010001'::uuid AND role_id = '00000000-0000-0000-0000-000000000301'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO tenant_user_role (id, tenant_id, user_id, role_id, assigned_at)
    VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010001'::uuid, '00000000-0000-0000-0000-000000000301'::uuid, now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM tenant_user_role WHERE tenant_id = t_id AND user_id = '00000000-0000-0000-0000-000000010002'::uuid AND role_id = '00000000-0000-0000-0000-000000000302'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO tenant_user_role (id, tenant_id, user_id, role_id, assigned_at)
    VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010002'::uuid, '00000000-0000-0000-0000-000000000302'::uuid, now());
  END IF;

  IF NOT EXISTS (SELECT 1 FROM tenant_user_role WHERE tenant_id = t_id AND user_id = '00000000-0000-0000-0000-000000010003'::uuid AND role_id = '00000000-0000-0000-0000-000000000304'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO tenant_user_role (id, tenant_id, user_id, role_id, assigned_at)
    VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010003'::uuid, '00000000-0000-0000-0000-000000000304'::uuid, now());
  END IF;

  -- Reset RLS context
  PERFORM set_config('app.current_tenant', '', true);

  RAISE NOTICE 'V202: done for tenant %', t_id;
END $$;

-- Sanity check
DO $$ DECLARE cnt int; BEGIN
  SELECT count(*) INTO cnt FROM app_role WHERE tenant_id IS NULL AND deleted_at IS NULL;
  IF cnt < 4 THEN RAISE EXCEPTION 'V202 sanity: expected >=4 system roles, found %', cnt; END IF;
  RAISE NOTICE 'V202 sanity OK: % system roles', cnt;
END $$;
