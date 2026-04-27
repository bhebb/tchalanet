#!/usr/bin/env bash
set -euo pipefail
unset PGDATABASE PGUSER PGSERVICE PGSERVICEFILE || true

# Création des utilisateurs et bases de données pour Tchalanet
# Ce script est exécuté automatiquement par PostgreSQL lors de l'initialisation du conteneur

echo "[init] Starting database initialization..."

# Validate required env vars to avoid generating invalid SQL when variables are empty
required=(
  # KC_* names only (Keycloak Quarkus expects KC_DB_USERNAME)
  KC_DB_NAME KC_DB_USERNAME KC_DB_PASSWORD
  APP_DB_NAME APP_DB_USER APP_DB_PASSWORD
  UNLEASH_DB_NAME UNLEASH_DB_USER UNLEASH_DB_PASSWORD
)
missing=()
for v in "${required[@]}"; do
  if [ -z "${!v-}" ]; then
    missing+=("$v")
  fi
done

# Re-check required vars after fallback
missing=()
for v in "${required[@]}"; do
  if [ -z "${!v-}" ]; then
    missing+=("$v")
  fi
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "[init] FATAL: missing required DB env vars: ${missing[*]}" >&2
  echo "[init] Ensure envs/<env>/.secrets contains these values and that docker-compose loads it via env_file." >&2
  exit 1
fi

echo "[init] All required environment variables present"

# helper: escape single quotes for SQL string literal (protection against SQL injection)
escape_sql() {
  printf "%s" "$1" | sed "s/'/''/g"
}

# Escape passwords (use KC_* variables)
KC_PW=$(escape_sql "${KC_DB_PASSWORD}")
APP_PW=$(escape_sql "${APP_DB_PASSWORD}")
UNLEASH_PW=$(escape_sql "${UNLEASH_DB_PASSWORD}")

# Function to create user and database (idempotent)
create_user_and_db() {
  local db_user="$1"
  local db_pass="$2"
  local db_name="$3"
  local label="$4"

  echo "[init] Creating $label database and user..."

  psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname "${POSTGRES_DB:-postgres}" <<-EOSQL
    -- Create user if not exists (idempotent)
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$db_user') THEN
        CREATE USER $db_user WITH PASSWORD '$db_pass';
        RAISE NOTICE 'User $db_user created';
      ELSE
        RAISE NOTICE 'User $db_user already exists, skipping';
      END IF;
    END
    \$\$;

    -- Create database (on s'en fiche si on recrée le volume à chaque fois en staging)
    CREATE DATABASE $db_name OWNER $db_user;

    -- Grant all privileges
    GRANT ALL PRIVILEGES ON DATABASE $db_name TO $db_user;

    \c $db_name
    GRANT ALL ON SCHEMA public TO $db_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $db_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $db_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $db_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $db_user;
EOSQL

  echo "[init] $label database and user ready"
}

# Create all databases and users (use KC_* for Keycloak)
create_user_and_db "${KC_DB_USERNAME}" "${KC_PW}" "${KC_DB_NAME}" "Keycloak"
create_user_and_db "${APP_DB_USER}" "${APP_PW}" "${APP_DB_NAME}" "Application"
create_user_and_db "${UNLEASH_DB_USER}" "${UNLEASH_PW}" "${UNLEASH_DB_NAME}" "Unleash"

echo "[init] Database initialization completed successfully"
