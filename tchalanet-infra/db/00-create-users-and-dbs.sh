#!/usr/bin/env bash
set -euo pipefail

# Defaults (can be overridden via service env)
: "${KC_USER:=kc}"
: "${KC_PASS:=kc_pwd}"
: "${KC_DB_NAME:=keycloak_db}"

: "${APP_USER:=app}"
: "${APP_PASS:=app_pwd}"
: "${APP_DB:=tchalanet_db}"

# 1) Create roles (allowed inside DO)
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='${KC_USER}') THEN
    CREATE ROLE ${KC_USER} LOGIN PASSWORD '${KC_PASS}';
  ELSE
    ALTER ROLE ${KC_USER} WITH LOGIN PASSWORD '${KC_PASS}';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='${APP_USER}') THEN
    CREATE ROLE ${APP_USER} LOGIN PASSWORD '${APP_PASS}';
  ELSE
    ALTER ROLE ${APP_USER} WITH LOGIN PASSWORD '${APP_PASS}';
  END IF;
END
\$\$;
SQL

# helper: create DB if missing (must be outside DO)
create_db_if_missing () {
  local dbname="$1" owner="$2"
  if ! psql -tAc "SELECT 1 FROM pg_database WHERE datname='${dbname}'" -U "$POSTGRES_USER" -d postgres | grep -q 1; then
    echo "Creating database ${dbname} (owner=${owner})"
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE ${dbname} OWNER ${owner};"
  else
    echo "Database ${dbname} already exists — skipping"
  fi
}

# 2) Create databases
create_db_if_missing "${KC_DB_NAME}"  "${KC_USER}"
create_db_if_missing "${APP_DB}"      "${APP_USER}"

# 3) (Optional) Harden ownership for safety
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "${KC_DB_NAME}"  -c "ALTER DATABASE ${KC_DB_NAME} OWNER TO ${KC_USER};"
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "${APP_DB}"      -c "ALTER DATABASE ${APP_DB} OWNER TO ${APP_USER};"

echo "Init script completed."
