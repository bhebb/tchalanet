#!/usr/bin/env bash
set -euo pipefail
unset PGDATABASE PGUSER PGSERVICE PGSERVICEFILE || true

echo "[init] Starting database initialization..."

# Required v0
required=(KC_DB_NAME KC_DB_USERNAME KC_DB_PASSWORD APP_DB_NAME APP_DB_USER APP_DB_PASSWORD)
missing=()
for v in "${required[@]}"; do
  [ -z "${!v-}" ] && missing+=("$v")
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "[init] FATAL: missing required DB env vars: ${missing[*]}" >&2
  echo "[init] Ensure envs/<env>/.secrets contains these values." >&2
  exit 1
fi

echo "[init] All required environment variables present"

escape_sql() { printf "%s" "$1" | sed "s/'/''/g"; }

KC_PW=$(escape_sql "${KC_DB_PASSWORD}")
APP_PW=$(escape_sql "${APP_DB_PASSWORD}")

create_user_and_db() {
  local db_user="$1" db_pass="$2" db_name="$3" label="$4"
  echo "[init] Creating $label database and user..."
  psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname "${POSTGRES_DB:-postgres}" <<-EOSQL
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$db_user') THEN
        CREATE USER $db_user WITH PASSWORD '$db_pass';
        RAISE NOTICE 'User $db_user created';
      ELSE
        ALTER USER $db_user WITH PASSWORD '$db_pass';
        RAISE NOTICE 'User $db_user already exists, password updated';
      END IF;
    END
    \$\$;

    SELECT 'CREATE DATABASE $db_name OWNER $db_user'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db_name') \gexec

    GRANT ALL PRIVILEGES ON DATABASE $db_name TO $db_user;

    \c $db_name
    GRANT ALL ON SCHEMA public TO $db_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $db_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $db_user;
EOSQL
  echo "[init] $label database and user ready"
}

# v0 required
create_user_and_db "${KC_DB_USERNAME}" "${KC_PW}" "${KC_DB_NAME}" "Keycloak"
create_user_and_db "${APP_DB_USER}" "${APP_PW}" "${APP_DB_NAME}" "Application"

echo "[init] Database initialization completed successfully"
