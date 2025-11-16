#!/usr/bin/env sh
set -eu

# meili-entrypoint.sh
# Validates MEILI_MASTER_KEY for production and starts MeiliSearch with configured env vars.

MEILI_ENV=${MEILI_ENV:-production}
MEILI_MASTER_KEY=${MEILI_MASTER_KEY:-}
MEILI_HTTP_ADDR=${MEILI_HTTP_ADDR:-0.0.0.0:7700}
MEILI_DB_PATH=${MEILI_DB_PATH:-/meili_data}

if [ "$MEILI_ENV" = "production" ]; then
  if [ -z "$MEILI_MASTER_KEY" ]; then
    echo "FATAL: MEILI_MASTER_KEY is not set (required in production)." >&2
    exit 1
  fi
  # length in bytes
  KEYLEN=$(printf '%s' "$MEILI_MASTER_KEY" | wc -c)
  if [ "$KEYLEN" -lt 16 ]; then
    echo "FATAL: MEILI_MASTER_KEY too short (length=${KEYLEN}), must be >= 16 bytes." >&2
    exit 1
  fi
fi

# Start MeiliSearch. Pass explicit args derived from env to be deterministic.
# If MEILI_MASTER_KEY is empty (e.g., dev), we still pass --master-key "" which Meili treats as no master key.
exec meilisearch --http-addr "$MEILI_HTTP_ADDR" --db-path "$MEILI_DB_PATH" --master-key "$MEILI_MASTER_KEY"

