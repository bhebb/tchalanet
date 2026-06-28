#!/usr/bin/env bash
# Restore local dev PostgreSQL and optional archive-data from backup-dev.sh output.
#
# Usage:
#   bash tchalanet-infra/scripts/local/restore-dev.sh
#   BACKUP_FILE=/path/to/tch-dev-db-YYYYmmdd-HHMMSS.dump bash ...
#
# Optional encrypted input:
#   BACKUP_FILE=/path/to/tch-dev-db-....dump.enc BACKUP_PASSPHRASE='...' bash ...
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
REPO_ROOT="$(cd "$ROOT/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/dev}"

PG_CONTAINER="${PG_CONTAINER:-tchl-postgres-dev}"
PG_DATABASE="${PG_DATABASE:-tchalanet_db}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-devpass}"
PG_OWNER="${PG_OWNER:-app_user}"
ARCHIVE_ROOT="${ARCHIVE_ROOT:-$REPO_ROOT/tchalanet-server/archive-data}"

command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }

BACKUP_FILE="${BACKUP_FILE:-}"
if [[ -z "$BACKUP_FILE" ]]; then
  BACKUP_FILE="$(ls -t "$BACKUP_DIR"/tch-dev-db-*.dump "$BACKUP_DIR"/tch-dev-db-*.dump.enc 2>/dev/null | head -1 || true)"
fi
if [[ -z "$BACKUP_FILE" || ! -f "$BACKUP_FILE" ]]; then
  echo "No DB backup found. Set BACKUP_FILE=/path/to/tch-dev-db-....dump" >&2
  exit 1
fi

RESTORE_DB_FILE="$BACKUP_FILE"
TMP_DIR="$(mktemp -d /tmp/tch-restore.XXXXXX)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

if [[ "$BACKUP_FILE" == *.enc ]]; then
  [[ -n "${BACKUP_PASSPHRASE:-}" ]] || {
    echo "BACKUP_PASSPHRASE is required for encrypted backups" >&2
    exit 1
  }
  RESTORE_DB_FILE="$TMP_DIR/restore.dump"
  openssl enc -d -aes-256-cbc -pbkdf2 \
    -pass env:BACKUP_PASSPHRASE \
    -in "$BACKUP_FILE" \
    -out "$RESTORE_DB_FILE"
fi

echo "Will restore DB backup:"
echo "  $BACKUP_FILE"
echo "Target:"
echo "  container=$PG_CONTAINER database=$PG_DATABASE user=$PG_USER"
echo
echo "This will DROP and recreate the local dev database."
read -r -p "Type RESTORE to continue: " CONFIRM
[[ "$CONFIRM" == "RESTORE" ]] || { echo "Cancelled."; exit 1; }

echo "-> dropping active connections and database"
docker exec -e PGPASSWORD="$PG_PASSWORD" "$PG_CONTAINER" psql -h 127.0.0.1 -U "$PG_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '$PG_DATABASE'
  AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS "$PG_DATABASE";
CREATE DATABASE "$PG_DATABASE" OWNER "$PG_OWNER";
SQL

echo "-> restoring PostgreSQL custom dump"
docker exec -i -e PGPASSWORD="$PG_PASSWORD" "$PG_CONTAINER" \
  pg_restore -h 127.0.0.1 -U "$PG_USER" -d "$PG_DATABASE" --no-owner --role="$PG_OWNER" < "$RESTORE_DB_FILE"

ARCHIVE_FILE="${ARCHIVE_FILE:-}"
if [[ -z "$ARCHIVE_FILE" ]]; then
  BASE="$(basename "$BACKUP_FILE")"
  TS="$(echo "$BASE" | sed -E 's/^tch-dev-db-([0-9]{8}-[0-9]{6})\\.dump(\\.enc)?$/\\1/')"
  if [[ "$TS" != "$BASE" ]]; then
    if [[ -f "$BACKUP_DIR/tch-dev-archive-data-$TS.tar.gz" ]]; then
      ARCHIVE_FILE="$BACKUP_DIR/tch-dev-archive-data-$TS.tar.gz"
    elif [[ -f "$BACKUP_DIR/tch-dev-archive-data-$TS.tar.gz.enc" ]]; then
      ARCHIVE_FILE="$BACKUP_DIR/tch-dev-archive-data-$TS.tar.gz.enc"
    fi
  fi
fi

if [[ -n "$ARCHIVE_FILE" && -f "$ARCHIVE_FILE" ]]; then
  RESTORE_ARCHIVE_FILE="$ARCHIVE_FILE"
  if [[ "$ARCHIVE_FILE" == *.enc ]]; then
    [[ -n "${BACKUP_PASSPHRASE:-}" ]] || {
      echo "BACKUP_PASSPHRASE is required for encrypted archive-data" >&2
      exit 1
    }
    RESTORE_ARCHIVE_FILE="$TMP_DIR/archive-data.tar.gz"
    openssl enc -d -aes-256-cbc -pbkdf2 \
      -pass env:BACKUP_PASSPHRASE \
      -in "$ARCHIVE_FILE" \
      -out "$RESTORE_ARCHIVE_FILE"
  fi
  echo "-> restoring archive-data: $ARCHIVE_FILE"
  mkdir -p "$ARCHIVE_ROOT"
  tar -C "$ARCHIVE_ROOT" -xzf "$RESTORE_ARCHIVE_FILE"
else
  echo "-> no archive-data backup restored"
fi

echo "OK: restore complete"
