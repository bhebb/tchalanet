#!/usr/bin/env bash
# Shared ownership guard for the rebuildable analytics projections.
#
# These tables are intentionally dumped/restored with --no-owner. The runtime
# application role must own them because their RLS configuration has no broad
# policy and relies on the owner bypass for projection reads and writes.

ensure_runtime_rls_owners() {
  [ -n "${PG_CONTAINER:-}" ] || { echo "PG_CONTAINER is required" >&2; return 1; }
  [ -n "${PGUSER:-}" ] || { echo "PGUSER is required" >&2; return 1; }
  [ -n "${PGPASSWORD:-}" ] || { echo "PGPASSWORD is required" >&2; return 1; }
  [ -n "${APP_DB:-}" ] || { echo "APP_DB is required" >&2; return 1; }
  [ -n "${APP_DB_USER:-}" ] || { echo "APP_DB_USER is required" >&2; return 1; }
  [[ "$APP_DB_USER" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || {
    echo "unsafe application database user: $APP_DB_USER" >&2
    return 1
  }

  docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
    psql -h 127.0.0.1 -U "$PGUSER" -d "$APP_DB" -v ON_ERROR_STOP=1 -q -c \
    "DO \$\$
DECLARE
  table_name text;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$APP_DB_USER') THEN
    RAISE EXCEPTION 'application role % does not exist', '$APP_DB_USER';
  END IF;

  FOREACH table_name IN ARRAY ARRAY[
    'analytics_daily',
    'analytics_draw',
    'analytics_selection'
  ] LOOP
    IF to_regclass('public.' || table_name) IS NOT NULL THEN
      EXECUTE format('ALTER TABLE public.%I OWNER TO %I', table_name, '$APP_DB_USER');
    END IF;
  END LOOP;
END
\$\$;"

  local expected owned
  expected="$(docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
    psql -h 127.0.0.1 -U "$PGUSER" -d "$APP_DB" -tAc \
    "SELECT count(*) FROM unnest(ARRAY['analytics_daily', 'analytics_draw', 'analytics_selection']) AS t(table_name) WHERE to_regclass('public.' || t.table_name) IS NOT NULL")"
  owned="$(docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
    psql -h 127.0.0.1 -U "$PGUSER" -d "$APP_DB" -tAc \
    "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace JOIN pg_roles r ON r.oid = c.relowner WHERE n.nspname = 'public' AND c.relname IN ('analytics_daily', 'analytics_draw', 'analytics_selection') AND r.rolname = '$APP_DB_USER'")"
  expected="$(printf '%s' "$expected" | tr -d '[:space:]')"
  owned="$(printf '%s' "$owned" | tr -d '[:space:]')"
  [ "$expected" = "$owned" ] || {
    echo "analytics ownership check failed: $owned/$expected tables owned by $APP_DB_USER" >&2
    return 1
  }
  printf 'Analytics ownership OK: %s/%s tables owned by %s\n' "$owned" "$expected" "$APP_DB_USER"
}
