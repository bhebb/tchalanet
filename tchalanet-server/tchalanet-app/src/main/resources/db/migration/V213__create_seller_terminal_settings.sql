CREATE TABLE seller_terminal_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  seller_terminal_id uuid NOT NULL REFERENCES seller_terminal(id),
  receipt_auto_print boolean NOT NULL DEFAULT true,
  receipt_copy_count integer NOT NULL DEFAULT 1,
  notifications_enabled boolean NOT NULL DEFAULT true,
  notifications_critical_only boolean NOT NULL DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_seller_terminal_settings_terminal UNIQUE (tenant_id, seller_terminal_id),
  CONSTRAINT fk_seller_terminal_settings__tenant_terminal
    FOREIGN KEY (tenant_id, seller_terminal_id) REFERENCES seller_terminal (tenant_id, id),
  CONSTRAINT chk_seller_terminal_settings_receipt_copy_count
    CHECK (receipt_copy_count BETWEEN 1 AND 3)
);

CREATE INDEX idx_seller_terminal_settings_terminal
  ON seller_terminal_settings (tenant_id, seller_terminal_id)
  WHERE deleted_at IS NULL;

ALTER TABLE seller_terminal_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller_terminal_settings FORCE ROW LEVEL SECURITY;

CREATE POLICY seller_terminal_settings_rls_all ON seller_terminal_settings
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());

CREATE POLICY seller_terminal_settings_rls_select ON seller_terminal_settings
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));
