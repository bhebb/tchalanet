-- Make seller_terminal_external_identity tenant-scoped directly.
-- The table is a child of seller_terminal, but RLS checks on the parent table can
-- be fragile during aggregate flushes. Storing tenant_id keeps the policy simple
-- and lets the DB enforce tenant consistency through a composite foreign key.

ALTER TABLE seller_terminal_external_identity
  ADD COLUMN IF NOT EXISTS tenant_id uuid;

UPDATE seller_terminal_external_identity sei
SET tenant_id = st.tenant_id
FROM seller_terminal st
WHERE sei.seller_terminal_id = st.id
  AND sei.tenant_id IS NULL;

ALTER TABLE seller_terminal_external_identity
  ALTER COLUMN tenant_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_seller_terminal_tenant_id_id
  ON seller_terminal (tenant_id, id);

CREATE INDEX IF NOT EXISTS idx_seller_terminal_ext_identity_tenant
  ON seller_terminal_external_identity (tenant_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_seller_terminal_ext_identity__tenant_terminal'
  ) THEN
    ALTER TABLE seller_terminal_external_identity
      ADD CONSTRAINT fk_seller_terminal_ext_identity__tenant_terminal
      FOREIGN KEY (tenant_id, seller_terminal_id)
      REFERENCES seller_terminal (tenant_id, id);
  END IF;
END $$;

DROP POLICY IF EXISTS seller_terminal_external_identity_rls_all
  ON seller_terminal_external_identity;

DROP POLICY IF EXISTS seller_terminal_external_identity_rls_select
  ON seller_terminal_external_identity;

CREATE POLICY seller_terminal_external_identity_rls_all
  ON seller_terminal_external_identity
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
  );

CREATE POLICY seller_terminal_external_identity_rls_select
  ON seller_terminal_external_identity
  FOR SELECT
  USING (
    public.allow_platform_cross_tenant_select()
    OR (
      public.current_tenant() IS NOT NULL
      AND tenant_id = public.current_tenant()
    )
  );
