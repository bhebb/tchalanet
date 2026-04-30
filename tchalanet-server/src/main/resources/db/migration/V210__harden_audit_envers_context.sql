-- Harden Envers revision context and application audit tenant/platform model.

ALTER TABLE revinfo
  ADD COLUMN IF NOT EXISTS request_id varchar(128),
  ADD COLUMN IF NOT EXISTS actor_type varchar(32),
  ADD COLUMN IF NOT EXISTS api_scope varchar(32),
  ADD COLUMN IF NOT EXISTS tenant_overridden boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS ix_revinfo__request_id ON revinfo (request_id);
CREATE INDEX IF NOT EXISTS ix_revinfo__api_scope ON revinfo (api_scope);

ALTER TABLE audit_event
  ALTER COLUMN tenant_id DROP NOT NULL,
  ALTER COLUMN actor_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS ix_audit_event__tenant_occurred
  ON audit_event (tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_event__entity
  ON audit_event (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_audit_event__action_occurred
  ON audit_event (action, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_event__actor_occurred
  ON audit_event (actor_id, occurred_at DESC);

DROP POLICY IF EXISTS audit_event_rls_all ON audit_event;
DROP POLICY IF EXISTS audit_event_rls_select ON audit_event;

CREATE POLICY audit_event_rls_all ON audit_event
  FOR ALL
  USING (
    (
      public.allow_platform_cross_tenant_select()
      OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    )
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (
    public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );

CREATE POLICY audit_event_rls_select ON audit_event
  FOR SELECT
  USING (
    public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
  );
