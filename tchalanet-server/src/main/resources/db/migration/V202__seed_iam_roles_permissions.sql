-- V42: seed IAM roles, permissions and mappings

-- Seed local test users (super_admin, admin, agent)
DO $$
DECLARE
  t_id uuid;
  u_id uuid;
  r_id uuid;
BEGIN
  RAISE NOTICE 'V42__seed_iam_roles_permissions: seeding local users (super_admin, admin, agent)';

  -- find tenant 'default' if present (optional)
  SELECT id INTO t_id FROM tenant WHERE code = 'default' LIMIT 1;

  -- --- super_admin ---
  IF NOT EXISTS (SELECT 1 FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010001'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO app_user (id, keycloak_sub, username, email, display_name, status, created_at, updated_at)
    VALUES (
      '00000000-0000-0000-0000-000000010001'::uuid,
      '00000000-0000-0000-0000-000000010001'::uuid,
      'super_admin', 'super_admin@local', 'Super Admin', 'ACTIVE', now(), now()
    );
  ELSE
    UPDATE app_user
    SET username = 'super_admin', email = 'super_admin@local', display_name = 'Super Admin', status = 'ACTIVE', updated_at = now()
    WHERE keycloak_sub = '00000000-0000-0000-0000-000000010001'::uuid;
  END IF;

  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010001'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'SUPER_ADMIN' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL AND t_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM tenant_user WHERE tenant_id = t_id AND user_id = u_id AND role_id = r_id AND deleted_at IS NULL) THEN
      INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
      VALUES (gen_random_uuid(), t_id, u_id, r_id, true, now(), now());
    END IF;
  END IF;

  -- --- admin ---
  IF NOT EXISTS (SELECT 1 FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010002'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO app_user (id, keycloak_sub, username, email, display_name, status, created_at, updated_at)
    VALUES (
      '00000000-0000-0000-0000-000000010002'::uuid,
      '00000000-0000-0000-0000-000000010002'::uuid,
      'admin', 'admin@local', 'Admin', 'ACTIVE', now(), now()
    );
  ELSE
    UPDATE app_user
    SET username = 'admin', email = 'admin@local', display_name = 'Admin', status = 'ACTIVE', updated_at = now()
    WHERE keycloak_sub = '00000000-0000-0000-0000-000000010002'::uuid;
  END IF;

  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010002'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'TENANT_ADMIN' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL AND t_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM tenant_user WHERE tenant_id = t_id AND user_id = u_id AND role_id = r_id AND deleted_at IS NULL) THEN
      INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
      VALUES (gen_random_uuid(), t_id, u_id, r_id, false, now(), now());
    END IF;
  END IF;

  -- --- agent ---
  IF NOT EXISTS (SELECT 1 FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010003'::uuid AND deleted_at IS NULL) THEN
    INSERT INTO app_user (id, keycloak_sub, username, email, display_name, status, created_at, updated_at)
    VALUES (
      '00000000-0000-0000-0000-000000010003'::uuid,
      '00000000-0000-0000-0000-000000010003'::uuid,
      'agent', 'agent@local', 'Agent', 'ACTIVE', now(), now()
    );
  ELSE
    UPDATE app_user
    SET username = 'agent', email = 'agent@local', display_name = 'Agent', status = 'ACTIVE', updated_at = now()
    WHERE keycloak_sub = '00000000-0000-0000-0000-000000010003'::uuid;
  END IF;

  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010003'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'CASHIER' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL AND t_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM tenant_user WHERE tenant_id = t_id AND user_id = u_id AND role_id = r_id AND deleted_at IS NULL) THEN
      INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
      VALUES (gen_random_uuid(), t_id, u_id, r_id, false, now(), now());
    END IF;
  END IF;

END $$;

DO $$ BEGIN
  RAISE NOTICE 'V42__seed_iam_roles_permissions: seeding roles & permissions';
END $$;

-- permissions (stable codes)
INSERT INTO permission (code, name, description)
VALUES
    ('session.read', 'Session Read', 'Read session totals and status'),
    ('session.totals.recompute', 'Session Totals Recompute', 'Force recompute of session totals'),
    ('reporting.view', 'Reporting View', 'View tenant reports')
    ON CONFLICT (code) DO NOTHING;

-- system roles (tenant_id NULL)
INSERT INTO app_role (id, tenant_id, code, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000301'::uuid, NULL, 'SUPER_ADMIN', 'Super Admin', 'System super administrator'),
    ('00000000-0000-0000-0000-000000000302'::uuid, NULL, 'TENANT_ADMIN', 'Tenant Admin', 'Tenant-level administrator'),
    ('00000000-0000-0000-0000-000000000303'::uuid, NULL, 'CASHIER', 'Cashier', 'Point-of-sale cashier')
    ON CONFLICT DO NOTHING;

-- role_permission mappings (idempotent)
INSERT INTO role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM app_role r
         JOIN permission p
              ON p.code IN ('session.read','session.totals.recompute','reporting.view')
WHERE r.tenant_id IS NULL
  AND r.code IN ('SUPER_ADMIN','TENANT_ADMIN','CASHIER')
    ON CONFLICT DO NOTHING;

-- Sanity check
DO $$
DECLARE cnt int;
BEGIN
SELECT count(*) INTO cnt
FROM app_role
WHERE tenant_id IS NULL AND deleted_at IS NULL;

IF cnt < 3 THEN
    RAISE EXCEPTION 'V42 sanity check failed: expected >=3 system roles, found %', cnt;
ELSE
    RAISE NOTICE 'V42 sanity check OK: % system roles present', cnt;
END IF;
END $$;
