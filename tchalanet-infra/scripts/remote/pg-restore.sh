#!/usr/bin/env bash
# Restaure un backup PostgreSQL depuis Cloudflare R2.
#
# Usage:
#   ENV=staging ./pg-restore.sh                     # dernier backup disponible
#   ENV=staging ./pg-restore.sh <objet-r2>          # backup précis
#   ENV=staging DRY_RUN=1 ./pg-restore.sh           # répétition : restaure dans un
#                                                   # conteneur jetable, ne touche à rien
#
# Variables requises :
#   ENV, CLOUDFLARE_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET
#   BACKUP_AGE_PRIVATE_KEY_FILE  chemin vers la clé privée age
#
# Dépendances : rclone, age, docker. Aucun outil ni compte AWS — R2 expose
# simplement une API S3, et rclone a un backend Cloudflare natif.
#
# La clé privée ne vit pas sur les serveurs applicatifs : elle n'est nécessaire
# qu'ici, au moment de restaurer.
set -euo pipefail

ENV="${ENV:?ENV is required (staging|prod)}"
OBJECT="${1:-}"
DRY_RUN="${DRY_RUN:-0}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

log()  { printf '→ %s\n' "$*"; }
fail() { printf '❌ %s\n' "$*" >&2; exit 1; }

for v in CLOUDFLARE_ACCOUNT_ID R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY R2_BUCKET BACKUP_AGE_PRIVATE_KEY_FILE; do
  [ -n "${!v:-}" ] || fail "$v is required"
done
[ -f "$BACKUP_AGE_PRIVATE_KEY_FILE" ] || fail "age private key not found: $BACKUP_AGE_PRIVATE_KEY_FILE"

command -v rclone >/dev/null 2>&1 || fail "rclone is required"
export RCLONE_CONFIG_R2_TYPE=s3
export RCLONE_CONFIG_R2_PROVIDER=Cloudflare
export RCLONE_CONFIG_R2_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export RCLONE_CONFIG_R2_ENDPOINT="https://${CLOUDFLARE_ACCOUNT_ID}.r2.cloudflarestorage.com"

if [ -z "$OBJECT" ]; then
  log "Resolving latest backup for ENV=$ENV"
  # Tri lexicographique : les noms portent un horodatage ISO en UTC, donc
  # l'ordre alphabétique est l'ordre chronologique.
  OBJECT="${ENV}/$(rclone lsf "r2:${R2_BUCKET}/${ENV}" --recursive --include '*.age' \
            | sort | tail -1)"
  [ "$OBJECT" != "${ENV}/" ] || fail "no backup found for ENV=$ENV"
fi
log "Using $OBJECT"

rclone copyto "r2:${R2_BUCKET}/${OBJECT}" "$WORK/backup.age" --retries 3 \
  || fail "download failed"

age -d -i "$BACKUP_AGE_PRIVATE_KEY_FILE" -o "$WORK/backup.tar.gz" "$WORK/backup.age" \
  || fail "decryption failed"
tar -xzf "$WORK/backup.tar.gz" -C "$WORK"
DUMP="$(find "$WORK" -name '*.dump' | head -1)"
[ -n "$DUMP" ] || fail "no .dump inside archive"

if [ "$DRY_RUN" = "1" ]; then
  # Répétition de restauration : prouve que le backup est exploitable sans
  # toucher à l'environnement réel. C'est ce mode que le workflow planifié
  # exécute, pour qu'un backup mort soit découvert avant d'en avoir besoin.
  log "DRY RUN — restoring into a throwaway container"
  VERIFY="tch-restore-rehearsal-$$"
  docker run -d --rm --name "$VERIFY" -e POSTGRES_PASSWORD=verify -e POSTGRES_DB=verify postgres:18 >/dev/null
  for _ in $(seq 1 60); do
    docker exec -e PGPASSWORD=verify "$VERIFY" pg_isready -U postgres -q && break
    sleep 1
  done
  docker cp "$DUMP" "$VERIFY:/tmp/r.dump"
  docker exec -e PGPASSWORD=verify "$VERIFY" pg_restore -U postgres -d verify --no-owner /tmp/r.dump >/dev/null 2>&1 \
    || { docker rm -f "$VERIFY" >/dev/null 2>&1 || true; fail "rehearsal failed: dump is not restorable"; }
  TABLES="$(docker exec -e PGPASSWORD=verify "$VERIFY" psql -U postgres -d verify -tAc \
            "select count(*) from information_schema.tables where table_schema='public'")"
  docker rm -f "$VERIFY" >/dev/null 2>&1 || true
  [ "${TABLES:-0}" -gt 0 ] || fail "rehearsal restored 0 tables"
  log "✅ Rehearsal OK — $TABLES tables restored from $OBJECT"
  exit 0
fi

PG_CONTAINER="$(docker ps --filter "name=postgres-${ENV}" --format '{{.Names}}' | head -1)"
[ -n "$PG_CONTAINER" ] || fail "no postgres container found for ENV=$ENV"

printf '⚠️  This overwrites the %s database in %s.\n' "$ENV" "$PG_CONTAINER"
printf "Type 'restore %s' to confirm: " "$ENV"
read -r CONFIRM
[ "$CONFIRM" = "restore $ENV" ] || { echo "Aborted."; exit 1; }

PGUSER="$(docker inspect "$PG_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_USER=//p' | head -1)"
PGPASSWORD="$(docker inspect "$PG_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_PASSWORD=//p' | head -1)"
APP_DB="$(basename "$DUMP" .dump)"

if [ -f "$WORK/globals.sql" ]; then
  log "Restoring globals (roles, grants)"
  docker cp "$WORK/globals.sql" "$PG_CONTAINER:/tmp/globals.sql"
  docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
    psql -U "$PGUSER" -d postgres -f /tmp/globals.sql >/dev/null 2>&1 || true
fi

log "Restoring $APP_DB"
docker cp "$DUMP" "$PG_CONTAINER:/tmp/restore.dump"
docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
  pg_restore -U "$PGUSER" -d "$APP_DB" --clean --if-exists --no-owner /tmp/restore.dump \
  || fail "pg_restore failed"

log "✅ Restored $APP_DB from $OBJECT"
