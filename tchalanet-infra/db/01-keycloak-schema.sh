#!/usr/bin/env bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d keycloak_db <<'SQL'
ALTER SCHEMA public OWNER TO kc;
GRANT USAGE, CREATE ON SCHEMA public TO kc;
-- (facultatif) droits par défaut
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO kc;
SQL
