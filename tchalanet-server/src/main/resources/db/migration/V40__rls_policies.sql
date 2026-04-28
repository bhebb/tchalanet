-- V40__rls_policies.sql (standard only)
-- Helpers used by policies
create or replace function public.api_scope()
returns text language sql stable as $$
select coalesce(current_setting('app.api_scope', true), '')
           $$;

create or replace function public.is_super_admin()
returns boolean language sql stable as $$
select coalesce(current_setting('app.is_super_admin', true), '') = 'true'
           $$;

create or replace function public.allow_platform_cross_tenant_select()
returns boolean language sql stable as $$
select public.is_super_admin() and public.api_scope() = 'platform'
           $$;


DO $$
DECLARE
t text;
  tbl regclass;
  has_tenant_id boolean;
  has_deleted_at boolean;

  policy_rw text;
  policy_sel text;

  sql_rw text;
  sql_sel text;

  soft_tables text[] := ARRAY[
    'outlet','terminal','sales_session','ticket','ticket_line','tenant_game','draw_channel','draw',
    'autonomy_policy_rule','limit_definition','limit_assignment','tenant_subscription','tenant_theme','audit_event',
    'app_user','app_role','tenant_user','page_model','payout','pricing_odds'
  ];

  tenant_only_tables text[] := ARRAY[
    'ledger_entry','stats_draw','draw_exposure'
  ];
BEGIN

  -- Soft-delete tables (tenant_id + deleted_at)
  FOREACH t IN ARRAY soft_tables LOOP
    tbl := to_regclass('public.' || t);
    IF tbl IS NULL THEN CONTINUE; END IF;

SELECT EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema='public' AND c.table_name=t AND c.column_name='tenant_id'
) INTO has_tenant_id;

SELECT EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema='public' AND c.table_name=t AND c.column_name='deleted_at'
) INTO has_deleted_at;

IF NOT has_tenant_id OR NOT has_deleted_at THEN
      CONTINUE;
END IF;

    policy_rw := t || '_rls_rw';
    policy_sel := t || '_rls_select';

EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);

EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_rw, tbl);
EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_sel, tbl);

-- RW: strict tenant (unchanged behavior)
sql_rw := format($f$
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
    $f$, policy_rw, tbl);

    -- SELECT: allow cross-tenant only for platform+superadmin
    sql_sel := format($f$
      CREATE POLICY %1$I ON %2$s
      FOR SELECT
      USING (
        (
          public.allow_platform_cross_tenant_select()
          AND (
            public.deleted_visibility() = 'all'
            OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
            OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
          )
        )
        OR
        (
          public.current_tenant() IS NOT NULL
          AND tenant_id = public.current_tenant()
          AND (
            public.deleted_visibility() = 'all'
            OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
            OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL)
          )
        )
      );
    $f$, policy_sel, tbl);

EXECUTE sql_rw;
EXECUTE sql_sel;
END LOOP;

  -- Tenant-only tables (tenant_id only)
  FOREACH t IN ARRAY tenant_only_tables LOOP
    tbl := to_regclass('public.' || t);
    IF tbl IS NULL THEN CONTINUE; END IF;

SELECT EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema='public' AND c.table_name=t AND c.column_name='tenant_id'
) INTO has_tenant_id;

IF NOT has_tenant_id THEN
      CONTINUE;
END IF;

    policy_rw := t || '_rls_rw';
    policy_sel := t || '_rls_select';

EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);

EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_rw, tbl);
EXECUTE format('DROP POLICY IF EXISTS %I ON %s', policy_sel, tbl);

sql_rw := format($f$
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
    $f$, policy_rw, tbl);

    sql_sel := format($f$
      CREATE POLICY %1$I ON %2$s
      FOR SELECT
      USING (
        public.allow_platform_cross_tenant_select()
        OR (
          public.current_tenant() IS NOT NULL
          AND tenant_id = public.current_tenant()
        )
      );
    $f$, policy_sel, tbl);

EXECUTE sql_rw;
EXECUTE sql_sel;
END LOOP;

END $$;


