-- V40__rls_policies.sql
-- RLS multi-tenant + soft delete visibility via app.deleted_visibility ('active'|'deleted'|'all')
-- Safer version: never casts current_setting(...)::uuid directly.
-- Relies on helper functions:
--   - current_tenant() -> uuid|null  (safe)
--   - deleted_visibility() -> text (safe default 'active')


-- --------------------------------------------------------------------
-- Policies
-- --------------------------------------------------------------------
DO $$
DECLARE
t text;
  tbl regclass;
  has_tenant_id boolean;
  has_deleted_at boolean;
  policy_name text;
  sql_stmt text;

  -- Tables multi-tenant + soft delete (require tenantId + deleted_at)
  soft_tables text[] := ARRAY[
    'outlet',
    'terminal',
    'pos_session',
    'ticket',
    'ticket_line',
    'tenant_game',
    'draw_channel',
    'draw',
    'limit_policy_config',
    'limit_definition',
    'limit_assignment',
    'subscription',
    'theme',
    'audit_event',
    'app_user',
    'app_role',
    'tenant_user',
    'page_model',
    'payout',
    'pricing_odds'
  ];

  -- Tables multi-tenant only (require tenantId; no deleted_at required)
  tenant_only_tables text[] := ARRAY[
    'ledger_entry',
    'stats_draw',
    'draw_exposure'
  ];
BEGIN
  -- ---------- soft delete tables ----------
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
      AND c.column_name = 'tenantId'
) INTO has_tenant_id;

SELECT EXISTS (
    SELECT 1
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = t
      AND c.column_name = 'deleted_at'
) INTO has_deleted_at;

-- Skip if required columns are missing
IF NOT has_tenant_id OR NOT has_deleted_at THEN
      CONTINUE;
END IF;

    policy_name := t || '_rls_all';

EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);

EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_name, tbl);

sql_stmt := format($f$
      CREATE POLICY %1$I ON %2$s
      FOR ALL
        USING (
          public.current_tenant() IS NOT NULL
          AND tenant_id = public.current_tenant()
          AND (
            public.deleted_visibility() = 'all'
            OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
            OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
          )
        )
        WITH CHECK (
          public.current_tenant() IS NOT NULL
          AND tenant_id = public.current_tenant()
        );

    $f$, policy_name, tbl);

EXECUTE sql_stmt;
END LOOP;

  -- ---------- tenant-only tables ----------
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
      AND c.column_name = 'tenantId'
) INTO has_tenant_id;

IF NOT has_tenant_id THEN
      CONTINUE;
END IF;

    policy_name := t || '_rls_all';

EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);

EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_name, tbl);

sql_stmt := format($f$
  CREATE POLICY %1$I ON %2$s
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
  )
  WITH CHECK (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
  );
$f$, policy_name, tbl);

EXECUTE sql_stmt;
END LOOP;
END
$$;
