-- V83: Recreate RLS policies using current_tenant() helper to avoid cast errors when app.current_tenant is empty
-- This migration is idempotent: it drops existing policies and recreates them using current_tenant()

DO $$
DECLARE
  t text;
  tbl regclass;
  has_tenant_id boolean;
  has_deleted_at boolean;
  policy_name text;
  sql_stmt text;
  soft_tables text[] := ARRAY[
    'outlet', 'terminal', 'pos_session', 'ticket', 'ticket_line', 'tenant_game', 'draw_channel',
    'draw', 'draw_result', 'odds', 'limit_policy_config', 'limit_definition', 'limit_assignment',
    'subscription', 'theme', 'audit_event', 'app_user', 'app_role', 'tenant_user', 'page_model',
    'i18n_override', 'app_setting', 'payout'
  ];
  tenant_only_tables text[] := ARRAY['ledger_entry','stats_draw','draw_exposure'];
BEGIN
  -- soft delete tables
  FOREACH t IN ARRAY soft_tables
  LOOP
    tbl := to_regclass('public.' || t);
    IF tbl IS NULL THEN
      CONTINUE;
    END IF;

    SELECT EXISTS (
      SELECT 1
      FROM information_schema.columns c
      WHERE c.table_schema = 'public'
        AND c.table_name = t
        AND c.column_name = 'tenant_id'
    ) INTO has_tenant_id;

    SELECT EXISTS (
      SELECT 1
      FROM information_schema.columns c
      WHERE c.table_schema = 'public'
        AND c.table_name = t
        AND c.column_name = 'deleted_at'
    ) INTO has_deleted_at;

    IF NOT has_tenant_id OR NOT has_deleted_at THEN
      CONTINUE;
    END IF;

    policy_name := t || '_rls_all';

    EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_name, tbl);

    sql_stmt := format($f$
      CREATE POLICY %1$I ON %2$s
      FOR ALL
      USING (
        tenant_id = current_tenant()
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
        tenant_id = current_tenant()
      );
    $f$, policy_name, tbl);

    EXECUTE sql_stmt;
  END LOOP;

  -- tenant-only tables
  FOREACH t IN ARRAY tenant_only_tables
  LOOP
    tbl := to_regclass('public.' || t);
    IF tbl IS NULL THEN
      CONTINUE;
    END IF;

    SELECT EXISTS (
      SELECT 1
      FROM information_schema.columns c
      WHERE c.table_schema = 'public'
        AND c.table_name = t
        AND c.column_name = 'tenant_id'
    ) INTO has_tenant_id;

    IF NOT has_tenant_id THEN
      CONTINUE;
    END IF;

    policy_name := t || '_rls_all';

    EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_name, tbl);

    sql_stmt := format($f$
      CREATE POLICY %1$I ON %2$s
      FOR ALL
      USING (
        tenant_id = current_tenant()
      )
      WITH CHECK (
        tenant_id = current_tenant()
      );
    $f$, policy_name, tbl);

    EXECUTE sql_stmt;
  END LOOP;
END $$;

