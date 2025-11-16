#!/usr/bin/env bash
# Generate and insert base64 cookie secrets (32 bytes) into envs/<ENV>/.secrets
# Usage: ./generate-cookie-secret.sh <env> VAR1 [VAR2 ...]
set -euo pipefail
ENV="${1:-dev}"
shift || true
SECRETS_FILE="$(pwd)/envs/${ENV}/.secrets"
if [ ! -d "$(dirname "$SECRETS_FILE")" ]; then
  echo "Env dir missing: $(dirname "$SECRETS_FILE")" >&2
  exit 2
fi
# Ensure file exists
if [ ! -f "$SECRETS_FILE" ]; then
  touch "$SECRETS_FILE"
  chmod 600 "$SECRETS_FILE"
fi
for VAR in "$@"; do
  # generate base64 32 bytes without newline
  NEW_SECRET=$(python3 - <<'PY'
import secrets,base64
print(base64.b64encode(secrets.token_bytes(32)).decode())
PY
)
  # atomically replace or append
  TMP=$(mktemp)
  awk -v var="$VAR" -v val="$NEW_SECRET" 'BEGIN{repl=0}
    /^\s*#/ {print; next}
    $0 ~ "^"var"=" { print var"="val; repl=1; next }
    { print }
    END{ if(!repl) print var"="val }' "$SECRETS_FILE" > "$TMP"
  mv "$TMP" "$SECRETS_FILE"
  chmod 600 "$SECRETS_FILE"
  echo "Updated $VAR in $SECRETS_FILE"
done

