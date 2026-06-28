#!/usr/bin/env bash
# Backup local dev PostgreSQL and local archive objects.
#
# Usage:
#   bash tchalanet-infra/scripts/local/backup-dev.sh
#
# Optional:
#   BACKUP_DIR=/Volumes/External/tch-backups bash ...
#   BACKUP_PASSPHRASE='...' bash ...     # creates encrypted .enc copies
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
REPO_ROOT="$(cd "$ROOT/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/dev}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

PG_CONTAINER="${PG_CONTAINER:-tchl-postgres-dev}"
PG_DATABASE="${PG_DATABASE:-tchalanet_db}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-devpass}"
ARCHIVE_ROOT="${ARCHIVE_ROOT:-$REPO_ROOT/tchalanet-server/archive-data}"

command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }

mkdir -p "$BACKUP_DIR"

DB_FILE="$BACKUP_DIR/tch-dev-db-$TIMESTAMP.dump"
ARCHIVE_FILE="$BACKUP_DIR/tch-dev-archive-data-$TIMESTAMP.tar.gz"
MANIFEST_FILE="$BACKUP_DIR/tch-dev-manifest-$TIMESTAMP.txt"

echo "-> PostgreSQL backup: $DB_FILE"
docker exec -e PGPASSWORD="$PG_PASSWORD" "$PG_CONTAINER" \
  pg_dump -h 127.0.0.1 -U "$PG_USER" -d "$PG_DATABASE" -Fc --no-owner --no-privileges \
  > "$DB_FILE"

echo "-> archive-data backup: $ARCHIVE_FILE"
if [[ -d "$ARCHIVE_ROOT" ]]; then
  tar -C "$ARCHIVE_ROOT" -czf "$ARCHIVE_FILE" .
else
  tar -czf "$ARCHIVE_FILE" --files-from /dev/null
fi

{
  echo "timestamp=$TIMESTAMP"
  echo "postgres_container=$PG_CONTAINER"
  echo "postgres_database=$PG_DATABASE"
  echo "postgres_user=$PG_USER"
  echo "db_file=$(basename "$DB_FILE")"
  echo "archive_file=$(basename "$ARCHIVE_FILE")"
  echo "created_at=$(date -u +%FT%TZ)"
  echo "db_sha256=$(shasum -a 256 "$DB_FILE" | awk '{print $1}')"
  echo "archive_sha256=$(shasum -a 256 "$ARCHIVE_FILE" | awk '{print $1}')"
} > "$MANIFEST_FILE"

if [[ -n "${BACKUP_PASSPHRASE:-}" ]]; then
  echo "-> encrypting backup files with openssl aes-256-cbc"
  openssl enc -aes-256-cbc -pbkdf2 -salt \
    -pass env:BACKUP_PASSPHRASE \
    -in "$DB_FILE" \
    -out "$DB_FILE.enc"
  openssl enc -aes-256-cbc -pbkdf2 -salt \
    -pass env:BACKUP_PASSPHRASE \
    -in "$ARCHIVE_FILE" \
    -out "$ARCHIVE_FILE.enc"
  openssl enc -aes-256-cbc -pbkdf2 -salt \
    -pass env:BACKUP_PASSPHRASE \
    -in "$MANIFEST_FILE" \
    -out "$MANIFEST_FILE.enc"
fi

du -sh "$DB_FILE" "$ARCHIVE_FILE" "$MANIFEST_FILE"
echo "OK: backup written under $BACKUP_DIR"
