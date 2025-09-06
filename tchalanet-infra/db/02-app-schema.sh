
#!/usr/bin/env bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d tchalanet_db <<'SQL'
ALTER SCHEMA public OWNER TO app;
GRANT USAGE, CREATE ON SCHEMA public TO app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO app;
SQL
