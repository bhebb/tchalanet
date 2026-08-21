CREATE TABLE client_diagnostic_policy (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  seller_terminal_id uuid NOT NULL REFERENCES seller_terminal(id),
  enabled boolean NOT NULL DEFAULT false,
  expires_at timestamptz,
  max_events integer NOT NULL DEFAULT 100,
  categories text[] NOT NULL DEFAULT ARRAY[]::text[],
  reason varchar(240),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_client_diagnostic_policy_terminal UNIQUE (tenant_id, seller_terminal_id),
  CONSTRAINT chk_client_diagnostic_policy_max_events CHECK (max_events BETWEEN 1 AND 500),
  CONSTRAINT chk_client_diagnostic_policy_expiry CHECK (enabled = false OR expires_at IS NOT NULL)
);

CREATE INDEX idx_client_diagnostic_policy_active
  ON client_diagnostic_policy (tenant_id, seller_terminal_id, expires_at)
  WHERE enabled = true AND deleted_at IS NULL;

CREATE TABLE client_diagnostic_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  seller_terminal_id uuid NOT NULL REFERENCES seller_terminal(id),
  event_id varchar(96) NOT NULL,
  category varchar(32) NOT NULL,
  severity varchar(32) NOT NULL,
  operation varchar(96) NOT NULL,
  occurred_at_client timestamptz NOT NULL,
  received_at_server timestamptz NOT NULL DEFAULT now(),
  request_id varchar(128),
  correlation_id varchar(128),
  error_code varchar(128),
  message varchar(512),
  exception_type varchar(160),
  http_status integer,
  endpoint_key varchar(128),
  app_version varchar(48),
  build_number varchar(48),
  platform varchar(32),
  device_model varchar(96),
  os_version varchar(96),
  printer_provider varchar(96),
  printer_service varchar(160),
  printer_state varchar(96),
  stack_frames jsonb NOT NULL DEFAULT '[]'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uq_client_diagnostic_event_terminal_event UNIQUE (tenant_id, seller_terminal_id, event_id),
  CONSTRAINT chk_client_diagnostic_event_http_status CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599),
  CONSTRAINT chk_client_diagnostic_event_category CHECK (category IN ('API', 'CONNECTIVITY', 'SALE', 'PRINT', 'SCANNER', 'PRINTER_CONFIG', 'FLUTTER', 'ASYNC', 'DEVICE')),
  CONSTRAINT chk_client_diagnostic_event_severity CHECK (severity IN ('WARN', 'ERROR'))
);

CREATE INDEX idx_client_diagnostic_event_recent
  ON client_diagnostic_event (received_at_server DESC, tenant_id, seller_terminal_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER trg_client_diagnostic_policy__set_updated_at
  BEFORE UPDATE ON client_diagnostic_policy
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE client_diagnostic_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE client_diagnostic_policy FORCE ROW LEVEL SECURITY;
CREATE POLICY client_diagnostic_policy_rls_all ON client_diagnostic_policy
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY client_diagnostic_policy_rls_select ON client_diagnostic_policy
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE client_diagnostic_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE client_diagnostic_event FORCE ROW LEVEL SECURITY;
CREATE POLICY client_diagnostic_event_rls_all ON client_diagnostic_event
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY client_diagnostic_event_rls_select ON client_diagnostic_event
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));
