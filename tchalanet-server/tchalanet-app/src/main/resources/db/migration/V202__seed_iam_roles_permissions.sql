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
  ('user.activate',            'Activate users',                 'user',     true, true),
  ('user.suspend',             'Suspend users',                  'user',     true, true),
  ('user.archive',             'Archive users',                  'user',     true, true),
  ('user.password.reset',      'Reset user passwords',           'user',     true, true),
  -- Access control
  ('role.read',                'Read roles',                     'access-control', true, true),
  ('role.manage',              'Manage roles',                   'access-control', true, true),
  ('role.permission.manage',   'Manage role permissions',        'access-control', true, true),
  ('permission.read',          'Read permissions',               'access-control', true, true),
  -- Settings / pricing
  ('settings.read',            'Read settings',                  'settings', true, true),
  ('settings.update',          'Update settings',                'settings', true, true),
  ('tenant.address.read',      'Read tenant address',            'tenant',   true, true),
  ('tenant.address.manage',    'Manage tenant address',          'tenant',   true, true),
  ('tenant.config.read',       'Read tenant configuration',      'tenant',   true, true),
  ('tenant.config.manage',     'Manage tenant configuration',    'tenant',   true, true),
  ('game-pricing.read',        'Read game pricing',              'pricing',  true, true),
  ('game-pricing.update',      'Update game pricing',            'pricing',  true, true),
  ('theme.read',               'Read tenant theme',              'theme',    true, true),
  ('theme.manage',             'Manage tenant theme',            'theme',    true, true),
  ('draw_channel.read',        'Read draw channels',             'draw_channel', true, true),
  ('draw_channel.manage',      'Manage draw channels',           'draw_channel', true, true),
  -- Limits / promotions
  ('limit.read',               'Read limits',                    'limit',    true, true),
  ('limit.manage',             'Manage limits',                  'limit',    true, true),
  ('promotion.read',           'Read promotions',                'promotion',true, true),
  ('promotion.manage',         'Manage promotions',              'promotion',true, true),
  -- Reports / audit
  ('report.read',              'Read reports',                   'report',   true, true),
  ('audit.read',               'Read audit log',                 'audit',    true, true),
  ('archive.read',             'Read archived entities',         'archive',  true, true),
  ('archive.run',              'Trigger archive run',            'archive',  true, true),
  ('archive.restore',          'Restore from archive',           'archive',  true, true),
  ('archive.objects.list',     'List archive objects',           'archive',  true, true),
  -- Draws / results
  ('draw.read',                'Read draws',                     'draw',     true, true),
  ('draw.lifecycle.manage',    'Manage draw lifecycle',          'draw',     true, true),
  ('draw.lifecycle.settle',    'Settle draws',                   'draw',     true, true),
  ('draw.schedule.manage',     'Manage draw schedule',           'draw',     true, true),
  ('draw_result.read',         'Read draw results',              'draw_result', true, true),
  ('draw_result.record_manual','Record manual draw results',     'draw_result', true, true),
  ('draw_result.override',     'Override draw results',          'draw_result', true, true),
  -- Seller terminal
  ('seller_terminal.read',              'Read seller terminals',              'seller_terminal', true, true),
  ('seller_terminal.manage',            'Manage seller terminals',            'seller_terminal', true, true),
  ('seller_terminal.block',             'Block/unblock seller terminals',     'seller_terminal', true, true),
  ('seller_terminal.reset_access',      'Reset seller terminal access',       'seller_terminal', true, true),
  ('seller_terminal.pin.reset',         'Reset seller terminal PIN',          'seller_terminal', true, true),
  ('seller_terminal.operational_context.read','Read seller terminal operational context','seller_terminal', true, true),
  -- Seller-terminal actor/self permissions
  ('seller_terminal.me.read',           'Read own seller terminal profile',   'seller_terminal', true, true),
  ('seller_terminal.pin.change',        'Change own seller terminal PIN',     'seller_terminal', true, true),
  -- Tickets / POS
  ('ticket.sell',              'Sell tickets',                    'ticket',   true, true),
  ('ticket.read',              'Read tickets',                    'ticket',   true, true),
  ('ticket.read_own',          'Read own tickets',                'ticket',   true, true),
  ('ticket.reprint_own',       'Reprint own tickets',             'ticket',   true, true),
  ('ticket.print',             'Print tickets',                   'ticket',   true, true),
  ('ticket.resend',            'Resend tickets',                  'ticket',   true, true),
  ('ticket.verify',            'Verify tickets',                  'ticket',   true, true),
  ('ticket.approve',           'Approve tickets',                 'ticket',   true, true),
  ('ticket.reject',            'Reject tickets',                  'ticket',   true, true),
  ('ticket.cancel',            'Cancel tickets',                  'ticket',   true, true),
  ('ticket.cancel-own',        'Cancel own tickets',              'ticket',   true, true),
  ('cashier.access',           'Legacy POS access',               'pos',      true, true),
  ('cashier.home.read',        'Read POS home surface',           'pos',      true, true),
  ('operational-context.read', 'Read operational context',         'context',  true, true),
  ('operational-context.select','Select operational context',      'context',  true, true),
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
  ('00000000-0000-0000-0000-000000000305'::uuid, NULL, 'TENANT_OWNER',  'Tenant Owner',  'Full owner of a tenant',          'TENANT',   true, false, true)
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
  'admin.access','dashboard.read',
  'user.read','user.create','user.update','user.disable','user.invite','user.sync',
  'user.membership.manage','user.role.assign','user.permission.manage',
  'user.activate','user.suspend','user.archive','user.password.reset',
  'role.read','role.manage','role.permission.manage','permission.read',
  'tenant.address.read','tenant.address.manage','tenant.config.read','tenant.config.manage',
  'settings.read','settings.update',
  'game-pricing.read','game-pricing.update',
  'draw_channel.read','draw_channel.manage',
  'limit.read','limit.manage',
  'promotion.read','promotion.manage',
  'theme.read','theme.manage',
  'draw.read','draw.lifecycle.manage','draw.lifecycle.settle','draw.schedule.manage',
  'draw_result.read','draw_result.record_manual','draw_result.override',
  'seller_terminal.read','seller_terminal.manage','seller_terminal.block','seller_terminal.reset_access',
  'seller_terminal.pin.reset','seller_terminal.operational_context.read',
  'ticket.sell','ticket.read','ticket.print','ticket.resend','ticket.verify',
  'ticket.approve','ticket.reject','ticket.cancel',
  'operational-context.read','operational-context.select',
  'sync.read','sync.submit',
  'report.read','audit.read',
  'archive.read','archive.run','archive.restore','archive.objects.list'
]) ON CONFLICT DO NOTHING;

-- TENANT_OWNER
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000305'::uuid, unnest(ARRAY[
  'admin.access','dashboard.read',
  'user.read','user.create','user.update','user.disable','user.invite','user.sync',
  'user.membership.manage','user.role.assign','user.permission.manage',
  'user.activate','user.suspend','user.archive','user.password.reset',
  'role.read','role.manage','role.permission.manage','permission.read',
  'tenant.address.read','tenant.address.manage','tenant.config.read','tenant.config.manage',
  'settings.read','settings.update',
  'game-pricing.read','game-pricing.update',
  'draw_channel.read',
  'limit.read','limit.manage',
  'promotion.read','promotion.manage',
  'theme.read','theme.manage',
  'draw.read','draw.lifecycle.manage',
  'draw_result.read','draw_result.record_manual',
  'seller_terminal.read','seller_terminal.manage','seller_terminal.block','seller_terminal.reset_access',
  'seller_terminal.pin.reset','seller_terminal.operational_context.read',
  'ticket.sell','ticket.read','ticket.print','ticket.resend','ticket.verify',
  'ticket.approve','ticket.reject','ticket.cancel',
  'operational-context.read','operational-context.select',
  'sync.read','sync.submit',
  'report.read','audit.read','archive.read'
]) ON CONFLICT DO NOTHING;

-- TENANT_ADMIN
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000302'::uuid, unnest(ARRAY[
  'admin.access','dashboard.read',
  'user.read','user.create','user.update','user.disable','user.invite','user.sync',
  'user.membership.manage','user.role.assign','user.permission.manage',
  'user.activate','user.suspend','user.archive','user.password.reset',
  'role.read','role.manage','role.permission.manage','permission.read',
  'tenant.address.read','tenant.address.manage','tenant.config.read','tenant.config.manage',
  'settings.read','settings.update',
  'game-pricing.read','game-pricing.update',
  'draw_channel.read',
  'limit.read','limit.manage',
  'promotion.read','promotion.manage',
  'theme.read','theme.manage',
  'draw.read','draw.lifecycle.manage',
  'draw_result.read','draw_result.record_manual',
  'seller_terminal.read','seller_terminal.manage','seller_terminal.block','seller_terminal.reset_access',
  'seller_terminal.pin.reset','seller_terminal.operational_context.read',
  'ticket.sell','ticket.read','ticket.print','ticket.resend','ticket.verify',
  'ticket.approve','ticket.reject','ticket.cancel',
  'operational-context.read','operational-context.select',
  'sync.read','sync.submit',
  'report.read','audit.read','archive.read'
]) ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Local dev / E2E seed users (super_admin, admin)
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
          'super_admin', 'super_admin@localtest.me', 'Super Admin', 'ACTIVE', now(), now())
  ON CONFLICT (id) DO UPDATE SET
    username = 'super_admin', email = 'super_admin@localtest.me',
    display_name = 'Super Admin', status = 'ACTIVE', updated_at = now();

  -- admin (TENANT_ADMIN)
  INSERT INTO app_user (id, username, email, display_name, status, created_at, updated_at)
  VALUES ('00000000-0000-0000-0000-000000010002'::uuid,
          'admin', 'admin@localtest.me', 'Admin', 'ACTIVE', now(), now())
  ON CONFLICT (id) DO UPDATE SET
    username = 'admin', email = 'admin@localtest.me',
    display_name = 'Admin', status = 'ACTIVE', updated_at = now();

  INSERT INTO app_user_external_identity (
    app_user_id, provider, issuer, external_subject, email_snapshot, created_at, updated_at)
  VALUES
    ('00000000-0000-0000-0000-000000010001'::uuid, 'LOCAL_JWT', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010001', 'super_admin@localtest.me', now(), now()),
    ('00000000-0000-0000-0000-000000010002'::uuid, 'LOCAL_JWT', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010002', 'admin@localtest.me', now(), now()),
    ('00000000-0000-0000-0000-000000010001'::uuid, 'LOCAL_PERF', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010001', 'super_admin@localtest.me', now(), now()),
    ('00000000-0000-0000-0000-000000010002'::uuid, 'LOCAL_PERF', 'tchalanet-local',
     '00000000-0000-0000-0000-000000010002', 'admin@localtest.me', now(), now())
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

  -- Tenant role assignments (no ON CONFLICT — partial unique index not supported).
  -- Platform roles such as SUPER_ADMIN are assigned via platform_user_role.
  IF NOT EXISTS (SELECT 1 FROM tenant_user_role WHERE tenant_id = t_id AND user_id = '00000000-0000-0000-0000-000000010002'::uuid AND role_id = '00000000-0000-0000-0000-000000000302'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO tenant_user_role (id, tenant_id, user_id, role_id, assigned_at)
    VALUES (gen_random_uuid(), t_id, '00000000-0000-0000-0000-000000010002'::uuid, '00000000-0000-0000-0000-000000000302'::uuid, now());
  END IF;

  -- Reset RLS context
  PERFORM set_config('app.current_tenant', '', true);

  RAISE NOTICE 'V202: done for tenant %', t_id;
END $$;

-- Sanity check
DO $$ DECLARE cnt int; BEGIN
  SELECT count(*) INTO cnt FROM app_role WHERE tenant_id IS NULL AND deleted_at IS NULL;
  IF cnt < 3 THEN RAISE EXCEPTION 'V202 sanity: expected >=3 system roles, found %', cnt; END IF;
  RAISE NOTICE 'V202 sanity OK: % system roles', cnt;
END $$;

-- ── Platform-scoped SUPER_ADMIN assignment for the local/E2E super admin user ──
INSERT INTO platform_user_role (id, user_id, role_id, assigned_at, assigned_by)
SELECT gen_random_uuid(), u.id, r.id, now(), null
FROM app_user u
JOIN app_role r
  ON r.code = 'SUPER_ADMIN'
 AND r.scope = 'PLATFORM'
 AND r.tenant_id IS NULL
WHERE u.id = '00000000-0000-0000-0000-000000010001'::uuid
  AND u.deleted_at IS NULL
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
