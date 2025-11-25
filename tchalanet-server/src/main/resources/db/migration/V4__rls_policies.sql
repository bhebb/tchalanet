-- V3__rls_policies.sql
-- RLS multi-tenant + gestion soft delete via deleted_visibility

DO $$
DECLARE t text;
BEGIN
  -- Tables multi-tenant avec tenant_id + deleted_at
  FOR t IN
    SELECT unnest(ARRAY[
      'tenant_game',
      'draw_channel',
      'draw',
      'odds',
      'limit_policy',
      'outlet',
      'terminal',
      'ticket',
      'subscription',
      'theme',
      'audit_event'
    ])
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);

    EXECUTE format($f$
      CREATE POLICY %1$s_rls_all ON %1$I
        FOR ALL
        USING (
          tenant_id = current_setting('app.current_tenant', true)::uuid
          AND (
            COALESCE(current_setting('app.deleted_visibility', true), 'active') = 'all'
            OR (
              COALESCE(current_setting('app.deleted_visibility', true), 'active') = 'active'
              AND deleted_at IS NULL
            )
            OR (
              COALESCE(current_setting('app.deleted_visibility', true), 'active') = 'deleted'
              AND deleted_at IS NOT NULL
            )
          )
        )
        WITH CHECK (
          tenant_id = current_setting('app.current_tenant', true)::uuid
        );
    $f$, t);

    -- Force RLS even for table owner so tests run as migration user still get policies applied
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
  END LOOP;
END$$;
