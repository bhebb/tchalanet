-- V211: Repair seed data broken by V202 using wrong tenant code 'default' instead of 'tchalanet'.
-- V202 failed to insert tenant_user records → V205 skipped terminal_assignment/terminal_binding.
-- This migration repairs both.

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$
DECLARE
  t_id  uuid;
  u_id  uuid;
  r_id  uuid;
  u_admin uuid;
BEGIN
  RAISE NOTICE 'V211__repair_seed_tenant_user_bindings: start';

  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V211: tenant tchalanet not found, skipping';
    RETURN;
  END IF;

  -- ── super_admin ──────────────────────────────────────────────────────────
  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010001'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'SUPER_ADMIN' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL THEN
    INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
    VALUES (gen_random_uuid(), t_id, u_id, r_id, true, now(), now())
    ON CONFLICT DO NOTHING;
  END IF;

  -- ── admin (TENANT_ADMIN) ─────────────────────────────────────────────────
  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010002'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'TENANT_ADMIN' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL THEN
    INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
    VALUES (gen_random_uuid(), t_id, u_id, r_id, false, now(), now())
    ON CONFLICT DO NOTHING;
    u_admin := u_id;
  END IF;

  -- ── cashier ──────────────────────────────────────────────────────────────
  SELECT id INTO u_id FROM app_user WHERE keycloak_sub = '00000000-0000-0000-0000-000000010003'::uuid LIMIT 1;
  SELECT id INTO r_id FROM app_role WHERE code = 'CASHIER' AND tenant_id IS NULL LIMIT 1;
  IF u_id IS NOT NULL AND r_id IS NOT NULL THEN
    INSERT INTO tenant_user (id, tenant_id, user_id, role_id, is_owner, created_at, updated_at)
    VALUES (gen_random_uuid(), t_id, u_id, r_id, false, now(), now())
    ON CONFLICT DO NOTHING;
  END IF;

  -- ── terminal_assignment & terminal.assigned_user_id ──────────────────────
  -- Use admin user (TENANT_ADMIN) for the seeded terminal assignment.
  -- The E2E challenge/verify flow will create the proper binding after this.
  IF u_admin IS NOT NULL THEN
    INSERT INTO terminal_assignment (
      id, tenant_id, terminal_id, user_id, status, assigned_at, created_at, updated_at
    )
    VALUES (
      '00000000-0000-0000-0000-000000003111'::uuid,
      t_id,
      '00000000-0000-0000-0000-000000003101'::uuid,
      u_admin,
      'ACTIVE',
      now(), now(), now()
    )
    ON CONFLICT DO NOTHING;

    UPDATE terminal
    SET assigned_user_id = u_admin, updated_at = now()
    WHERE tenant_id = t_id
      AND id = '00000000-0000-0000-0000-000000003101'::uuid
      AND assigned_user_id IS NULL;
  END IF;

  -- ── terminal_binding ─────────────────────────────────────────────────────
  -- Seed a known dev binding so local E2E can authenticate with credential 'e2e-cred-dev'.
  -- Hash = SHA256Hex(tenantId + "|" + terminalId + "|" + "e2e-cred-dev")
  -- = 4d0e4221f2911bd2acd91765ec95b6dba87b965e5ecd2218f0a7b3ed638e10cf
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
    now(), now(), now()
  )
  ON CONFLICT DO NOTHING;

  RAISE NOTICE 'V211: repair complete for tenant %', t_id;
END $$;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
