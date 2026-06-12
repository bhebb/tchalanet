-- V212: Ensure terminal_assignment exists for admin and cashier seed users.
-- V205 assigns the first tenant_user (super_admin) to the seeded terminal.
-- V211 attempted admin but conflicted on the same PK. Neither admin nor cashier had assignments.
-- This migration idempotently adds assignments for admin (003112) and cashier (003113).

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$
DECLARE
  t_id      uuid;
  u_admin   uuid;
  u_cashier uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V212: tenant tchalanet not found, skipping';
    RETURN;
  END IF;

  SELECT id INTO u_admin   FROM app_user WHERE id = '00000000-0000-0000-0000-000000010002'::uuid LIMIT 1;
  SELECT id INTO u_cashier FROM app_user WHERE id = '00000000-0000-0000-0000-000000010003'::uuid LIMIT 1;

  -- admin terminal_assignment
  IF u_admin IS NOT NULL THEN
    INSERT INTO terminal_assignment (
      id, tenant_id, terminal_id, user_id, status, assigned_at, created_at, updated_at
    )
    VALUES (
      '00000000-0000-0000-0000-000000003112'::uuid,
      t_id,
      '00000000-0000-0000-0000-000000003101'::uuid,
      u_admin,
      'ACTIVE',
      now(), now(), now()
    )
    ON CONFLICT DO NOTHING;
  END IF;

  -- cashier terminal_assignment
  IF u_cashier IS NOT NULL THEN
    INSERT INTO terminal_assignment (
      id, tenant_id, terminal_id, user_id, status, assigned_at, created_at, updated_at
    )
    VALUES (
      '00000000-0000-0000-0000-000000003113'::uuid,
      t_id,
      '00000000-0000-0000-0000-000000003101'::uuid,
      u_cashier,
      'ACTIVE',
      now(), now(), now()
    )
    ON CONFLICT DO NOTHING;
  END IF;

  RAISE NOTICE 'V212: terminal_assignment seeded for admin=% cashier=%', u_admin, u_cashier;
END $$;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
