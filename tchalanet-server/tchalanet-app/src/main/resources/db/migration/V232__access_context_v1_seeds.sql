-- V232: Access-context V1 role and permission seeds
-- Additive to V202. Adds TENANT_OWNER role, missing permissions, and role-permission matrix.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Missing permissions (provider-neutral-access-context-v1)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO permission (code, name, category, system, active) VALUES
  -- Seller-terminal admin surface (V0 operational POS actor)
  ('seller_terminal.manage',     'Manage seller terminals',        'seller_terminal', true, true),
  ('seller_terminal.block',      'Block seller terminals',         'seller_terminal', true, true),
  ('seller_terminal.reset_access','Reset seller terminal access',   'seller_terminal', true, true),
  ('seller_terminal.operational_context.read','Read seller terminal operational context','seller_terminal', true, true),
  -- Sales reporting
  ('sales.read',                 'Read sales',                     'sales',    true, true),
  ('sales.report.read',          'Read sales reports',             'sales',    true, true),
  -- Ticket admin
  ('ticket.void',                'Void tickets',                   'ticket',   true, true),
  -- Odds
  ('odds.read',                  'Read odds',                      'odds',     true, true),
  ('odds.manage',                'Manage odds',                    'odds',     true, true),
  -- Draw results
  ('draw_result.read',           'Read draw results',              'draw_result', true, true),
  ('draw_result.manage',         'Manage draw results',            'draw_result', true, true),
  ('draw_result.confirm',        'Confirm draw results',           'draw_result', true, true),
  -- Billing
  ('billing.read',               'Read billing',                   'billing',  true, true),
  -- Platform governance (new; coarse-grained alternatives to fine-grained V202 codes)
  ('platform.tenant.manage',     'Manage tenants (platform)',      'platform', true, true),
  ('platform.tenant.override',   'Override tenant scope (platform)','platform', true, true),
  ('platform.ops.run',           'Run platform ops',               'platform', true, true),
  -- Seller-terminal-derived (produced by access resolution, not assigned via role_permission rows)
  ('seller_terminal.me.read',           'Read own seller terminal profile',      'seller_terminal', true, true),
  ('seller_terminal.sell',              'Sell via seller terminal',              'seller_terminal', true, true),
  ('seller_terminal.ticket.read_own',   'Read own seller terminal tickets',      'seller_terminal', true, true),
  ('seller_terminal.ticket.reprint_own','Reprint own seller terminal tickets',   'seller_terminal', true, true)
ON CONFLICT (code) DO UPDATE SET
  name     = EXCLUDED.name,
  category = EXCLUDED.category,
  system   = EXCLUDED.system,
  active   = EXCLUDED.active,
  updated_at = now();

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. TENANT_OWNER role
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_role (id, tenant_id, code, name, description, scope, system, custom, active) VALUES
  ('00000000-0000-0000-0000-000000000305'::uuid, NULL,
   'TENANT_OWNER', 'Tenant Owner', 'Full owner of a tenant', 'TENANT', true, false, true)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name, description = EXCLUDED.description,
  scope = EXCLUDED.scope, system = EXCLUDED.system,
  active = EXCLUDED.active, updated_at = now();

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Role-permission matrix additions
-- ─────────────────────────────────────────────────────────────────────────────

-- TENANT_OWNER — all tenant permissions (V202 TENANT_ADMIN set + V232 additions)
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000305'::uuid, unnest(ARRAY[
  -- Inherited from TENANT_ADMIN (V202)
  'admin.access','dashboard.read',
  'user.read','user.create','user.update','user.disable','user.invite','user.sync',
  'user.membership.manage','user.role.assign','user.permission.manage',
  'role.read','permission.read',
  'settings.read','settings.update',
  'game-pricing.read','game-pricing.update',
  'limit.read','limit.manage',
  'promotion.read','promotion.manage',
  'operational-context.read','operational-context.select',
  'report.read',
  -- V232 additions
  'seller_terminal.manage','seller_terminal.block','seller_terminal.reset_access',
  'seller_terminal.operational_context.read',
  'sales.read','sales.report.read',
  'ticket.read','ticket.void',
  'odds.read','odds.manage',
  'draw_result.read','draw_result.manage','draw_result.confirm',
  'billing.read',
  'audit.read'
]) ON CONFLICT DO NOTHING;

-- TENANT_ADMIN — add new V232 permissions
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000302'::uuid, unnest(ARRAY[
  'seller_terminal.manage','seller_terminal.block','seller_terminal.reset_access',
  'seller_terminal.operational_context.read',
  'sales.read','sales.report.read',
  'ticket.read','ticket.void',
  'odds.read','odds.manage',
  'draw_result.read','draw_result.manage','draw_result.confirm',
  'billing.read'
]) ON CONFLICT DO NOTHING;

-- SUPER_ADMIN — add new platform governance permissions
INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000301'::uuid, unnest(ARRAY[
  'platform.tenant.manage',
  'platform.tenant.override',
  'platform.ops.run'
]) ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- Sanity checks
-- ─────────────────────────────────────────────────────────────────────────────
DO $$ DECLARE cnt int; BEGIN
  SELECT count(*) INTO cnt FROM app_role WHERE tenant_id IS NULL AND deleted_at IS NULL;
  IF cnt < 5 THEN RAISE EXCEPTION 'V232 sanity: expected >=5 system roles, found %', cnt; END IF;

  IF NOT EXISTS (SELECT 1 FROM permission WHERE code = 'seller_terminal.sell') THEN
    RAISE EXCEPTION 'V232 sanity: permission seller_terminal.sell missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM app_role WHERE code = 'TENANT_OWNER' AND tenant_id IS NULL) THEN
    RAISE EXCEPTION 'V232 sanity: TENANT_OWNER role missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM permission WHERE code = 'platform.tenant.override') THEN
    RAISE EXCEPTION 'V232 sanity: permission platform.tenant.override missing';
  END IF;

  RAISE NOTICE 'V232 OK: % system roles, TENANT_OWNER present, seller_terminal.sell present', cnt;
END $$;
