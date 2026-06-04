-- V219: Retire the seeded OPEN sales_session so the cashier can open a fresh one.
--
-- V205 seeds an OPEN sales_session (…3201) on the seeded terminal (…3101),
-- opened_by super_admin (…010001). The partial unique index
-- ux_sales_session__open_terminal (tenant_id, terminal_id) WHERE status='OPEN'
-- AND deleted_at IS NULL allows only ONE open session per terminal, so the
-- cashier's POST /tenant/cashier/session/open fails with a duplicate-key 500.
--
-- The happy-path expects the terminal to start with NO open session. Soft-delete
-- the seeded artifact (deleted_at) — the unique index and the app's
-- deleted_visibility=active RLS both exclude soft-deleted rows, so no close-field
-- CHECK constraints are involved. Guarded on the seed tenant; idempotent.

SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000003', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'tenant', true);
SELECT set_config('app.is_super_admin', 'false', true);

DO $$
DECLARE
  t_id uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V219: tenant tchalanet not found, skipping';
    RETURN;
  END IF;

  UPDATE sales_session
  SET deleted_at = now(),
      updated_at = now()
  WHERE id = '00000000-0000-0000-0000-000000003201'::uuid
    AND tenant_id = t_id
    AND status = 'OPEN'
    AND deleted_at IS NULL;

  RAISE NOTICE 'V219: retired seeded open sales_session (if present)';
END $$;

-- Reset RLS context
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
