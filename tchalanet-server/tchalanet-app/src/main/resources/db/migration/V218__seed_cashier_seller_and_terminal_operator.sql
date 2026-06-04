-- V218: Unblock the cashier POS happy path on the dev/e2e seed.
--
-- Two backend gates were failing for the seeded cashier (keycloak_sub …010003):
--   1. ValidateTerminalForOperation → Terminal.assignedTo(user) checks the
--      denormalised terminal.assigned_user_id. V205 set it to super_admin
--      (…010001), so the cashier was rejected with "terminal.seller_not_assigned".
--   2. ResolveSellerForOperation requires (a) a seller row for the user
--      ("seller.no_seller_for_user"), ACTIVE ("seller.not_active"), and
--      (b) an active seller_outlet_assignment for the operating outlet
--      ("seller.not_assigned_to_outlet"). Both tables were empty.
--
-- This migration idempotently seeds the cashier seller + outlet assignment and
-- points the seeded terminal's operator at the cashier. Guarded on the seed
-- tenant 'tchalanet' so it no-ops in environments that lack it (prod-safe).

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$
DECLARE
  t_id       uuid;
  u_cashier  uuid;
  o_id       uuid := '00000000-0000-0000-0000-000000003001'::uuid; -- PDV Principal
  term_id    uuid := '00000000-0000-0000-0000-000000003101'::uuid; -- POS-01
  seller_id  uuid := '00000000-0000-0000-0000-000000004001'::uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V218: tenant tchalanet not found, skipping';
    RETURN;
  END IF;

  SELECT id INTO u_cashier
    FROM app_user
    WHERE keycloak_sub = '00000000-0000-0000-0000-000000010003'::uuid
    LIMIT 1;

  IF u_cashier IS NULL THEN
    RAISE NOTICE 'V218: cashier app_user not found, skipping';
    RETURN;
  END IF;

  -- 1. Seller linked to the cashier user (ACTIVE).
  INSERT INTO seller (
    id, tenant_id, user_id, code, display_name, status,
    created_at, updated_at, version
  )
  VALUES (
    seller_id, t_id, u_cashier, 'CASHIER', 'Caissier (seed)', 'ACTIVE',
    now(), now(), 0
  )
  ON CONFLICT (id) DO NOTHING;

  -- 2. Seller assigned to the default outlet (ACTIVE).
  INSERT INTO seller_outlet_assignment (
    id, tenant_id, seller_id, outlet_id, starts_at, status,
    created_at, updated_at, version
  )
  VALUES (
    '00000000-0000-0000-0000-000000004011'::uuid,
    t_id, seller_id, o_id, now(), 'ACTIVE',
    now(), now(), 0
  )
  ON CONFLICT (id) DO NOTHING;

  -- 3. Make the cashier the operator of the seeded terminal so
  --    Terminal.assignedTo(cashier) passes (was super_admin from V205).
  UPDATE terminal
  SET assigned_user_id = u_cashier,
      updated_at = now()
  WHERE id = term_id
    AND tenant_id = t_id;

  RAISE NOTICE 'V218: cashier seller+assignment seeded, terminal operator=%', u_cashier;
END $$;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
