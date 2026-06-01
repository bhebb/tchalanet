-- V211: Seed terminal_assignment + terminal_binding for local dev / E2E.
-- User/role seeding is handled by V202 (no longer a repair migration).

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$
DECLARE
  t_id    uuid;
  u_admin uuid;
BEGIN
  RAISE NOTICE 'V211: seeding terminal assignment + binding';

  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V211: tenant tchalanet not found, skipping';
    RETURN;
  END IF;

  SELECT id INTO u_admin FROM app_user
  WHERE keycloak_sub = '00000000-0000-0000-0000-000000010002'::uuid LIMIT 1;

  IF u_admin IS NOT NULL THEN
    INSERT INTO terminal_assignment (
      id, tenant_id, terminal_id, user_id, status, assigned_at, created_at, updated_at
    ) VALUES (
      '00000000-0000-0000-0000-000000003111'::uuid,
      t_id, '00000000-0000-0000-0000-000000003101'::uuid,
      u_admin, 'ACTIVE', now(), now(), now()
    ) ON CONFLICT DO NOTHING;

    UPDATE terminal
    SET assigned_user_id = u_admin, updated_at = now()
    WHERE tenant_id = t_id
      AND id = '00000000-0000-0000-0000-000000003101'::uuid
      AND assigned_user_id IS NULL;
  END IF;

  -- Dev binding with known credential 'e2e-cred-dev'
  -- credential_hash = SHA256Hex(tenantId + "|" + terminalId + "|" + "e2e-cred-dev")
  INSERT INTO terminal_binding (
    id, tenant_id, terminal_id, binding_type, status,
    binding_public_key, credential_hash, device_fingerprint_hash,
    bound_at, created_at, updated_at
  ) VALUES (
    '00000000-0000-0000-0000-000000003121'::uuid,
    t_id, '00000000-0000-0000-0000-000000003101'::uuid,
    'POS_DEVICE', 'ACTIVE',
    'local-dev-public-key',
    encode(digest(t_id::text || '|' || '00000000-0000-0000-0000-000000003101' || '|' || 'e2e-cred-dev', 'sha256'), 'hex'),
    'local-dev-device-fingerprint-hash',
    now(), now(), now()
  ) ON CONFLICT DO NOTHING;

  RAISE NOTICE 'V211: terminal seeding complete for tenant %', t_id;
END $$;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
