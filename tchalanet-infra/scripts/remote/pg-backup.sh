#!/usr/bin/env bash
# Backup PostgreSQL chiffré vers Cloudflare R2, vérifié par restauration.
#
# Remplace staging-backup.sh, qui appelait `pg_dumpall -U postgres` alors que le
# superuser est `admin`, sans mot de passe, et déposait le résultat sur le
# portable du développeur — donc jamais planifié et jamais hors-site.
#
# Ce script est conçu pour tourner depuis GitHub Actions (voir
# .github/workflows/db-backup.yml) via SSH sur la VM cible.
#
# Usage:
#   ENV=staging ./pg-backup.sh
#
# Variables requises :
#   ENV                    staging | prod
#   CLOUDFLARE_ACCOUNT_ID  identifiant de compte Cloudflare — le même secret que
#                          celui utilisé par le worker lottery-proxy
#   R2_ACCESS_KEY_ID       clé d'accès R2
#   R2_SECRET_ACCESS_KEY   secret R2
#   R2_BUCKET              bucket de destination
#
# Note : R2_ACCESS_KEY_ID/SECRET sont un type de credential distinct de
# CLOUDFLARE_API_TOKEN. Le token API pilote l'API Cloudflare ; l'accès objet en
# S3 exige un token R2 dédié, créé depuis R2 » Manage API tokens.
#   BACKUP_AGE_PUBLIC_KEY  clé publique age — le chiffrement est obligatoire
#
# Variables optionnelles :
#   BACKUP_RETAIN_DAILY_DAYS      défaut 14
#   BACKUP_RETAIN_MONTHLY_MONTHS  défaut 12
#
# Dépendances sur la VM : docker, age, rclone.
# rclone parle nativement à R2 (binaire unique, multipart et reprises gérés) ;
# aucun compte ni outil AWS n'est impliqué — R2 expose simplement une API S3.
#
# Le chiffrement est asymétrique : la VM ne détient que la clé publique. Une
# compromission du serveur ne donne donc pas accès aux backups déjà poussés.
set -euo pipefail

ENV="${ENV:?ENV is required (staging|prod)}"
TS="$(date -u +%Y%m%dT%H%M%SZ)"
PREFIX="${ENV}/$(date -u +%Y/%m)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

log()  { printf '→ %s\n' "$*"; }
fail() { printf '❌ %s\n' "$*" >&2; exit 1; }

for v in CLOUDFLARE_ACCOUNT_ID R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY R2_BUCKET BACKUP_AGE_PUBLIC_KEY; do
  [ -n "${!v:-}" ] || fail "$v is required"
done

PG_CONTAINER="$(docker ps --filter "name=postgres-${ENV}" --format '{{.Names}}' | head -1)"
[ -n "$PG_CONTAINER" ] || fail "no postgres container found for ENV=$ENV"

# Credentials lus depuis le conteneur lui-même : une seule source de vérité,
# donc pas de dérive possible entre le script et ce qui tourne réellement.
PGUSER="$(docker inspect "$PG_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_USER=//p' | head -1)"
PGPASSWORD="$(docker inspect "$PG_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_PASSWORD=//p' | head -1)"
APP_DB="$(docker inspect "$PG_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^APP_DB_NAME=//p' | head -1)"
APP_DB="${APP_DB:-tchalanet_db}"
[ -n "$PGUSER" ] && [ -n "$PGPASSWORD" ] || fail "could not read POSTGRES_USER/PASSWORD from $PG_CONTAINER"

log "Backup ENV=$ENV container=$PG_CONTAINER db=$APP_DB"

# 1) Rôles et privilèges (globals) — pg_dump seul ne les inclut pas, et sans eux
#    une restauration produit une base dont les GRANT et le rôle app_user manquent.
log "Dumping globals"
docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
  pg_dumpall -U "$PGUSER" --globals-only > "$WORK/globals.sql" \
  || fail "pg_dumpall --globals-only failed"

# 2) Base applicative en format custom : permet une restauration sélective et
#    parallèle, contrairement au SQL brut.
log "Dumping $APP_DB"
docker exec -e PGPASSWORD="$PGPASSWORD" "$PG_CONTAINER" \
  pg_dump -U "$PGUSER" -d "$APP_DB" -Fc --no-owner > "$WORK/${APP_DB}.dump" \
  || fail "pg_dump failed"

[ -s "$WORK/${APP_DB}.dump" ] || fail "dump is empty"

# 3) Vérification par restauration réelle, dans un conteneur jetable.
#    Un contrôle de taille ne prouve rien : un gzip de message d'erreur pèse
#    aussi quelques octets. Seule une restauration atteste que le backup sert.
log "Verifying by restoring into a throwaway container"
PG_IMAGE="$(docker inspect "$PG_CONTAINER" --format '{{.Config.Image}}')"
VERIFY="tch-backup-verify-$$"
docker run -d --rm --name "$VERIFY" -e POSTGRES_PASSWORD=verify -e POSTGRES_DB=verify "$PG_IMAGE" >/dev/null
for _ in $(seq 1 60); do
  docker exec -e PGPASSWORD=verify "$VERIFY" pg_isready -U postgres -q && break
  sleep 1
done
docker exec -e PGPASSWORD=verify "$VERIFY" pg_isready -U postgres -q || {
  docker rm -f "$VERIFY" >/dev/null 2>&1 || true
  fail "verification container never became ready"
}
# Les rôles d'abord : le dump porte des GRANT sur app_user, qui n'existe pas
# dans un conteneur neuf. Restaurer globals puis la base reproduit fidèlement
# une vraie restauration — l'omettre faisait échouer la vérification sur des
# rôles manquants plutôt que sur un défaut réel du backup.
docker cp "$WORK/globals.sql" "$VERIFY:/tmp/globals.sql"
docker exec -e PGPASSWORD=verify "$VERIFY" \
  psql -U postgres -d verify -q -f /tmp/globals.sql >/dev/null 2>&1 || true

docker cp "$WORK/${APP_DB}.dump" "$VERIFY:/tmp/verify.dump"
RESTORE_LOG="$WORK/restore.log"
if ! docker exec -e PGPASSWORD=verify "$VERIFY" \
      pg_restore -U postgres -d verify --no-owner /tmp/verify.dump >"$RESTORE_LOG" 2>&1; then
  echo "── pg_restore output ──" >&2
  head -20 "$RESTORE_LOG" >&2
  docker rm -f "$VERIFY" >/dev/null 2>&1 || true
  fail "backup failed verification: pg_restore could not read the dump"
fi
TABLES="$(docker exec -e PGPASSWORD=verify "$VERIFY" \
  psql -U postgres -d verify -tAc \
  "select count(*) from information_schema.tables where table_schema='public'")"
docker rm -f "$VERIFY" >/dev/null 2>&1 || true
[ "${TABLES:-0}" -gt 0 ] || fail "restored database has no tables — backup is not usable"
log "Verified: $TABLES tables restored"

# 4) Archive + chiffrement (les dumps contiennent des données personnelles :
#    utilisateurs, téléphones, e-mails, ventes).
ARCHIVE="$WORK/tchalanet-${ENV}-${TS}.tar.gz"
tar -czf "$ARCHIVE" -C "$WORK" globals.sql "${APP_DB}.dump"
command -v age >/dev/null 2>&1 || fail "age is required for backup encryption"
age -r "$BACKUP_AGE_PUBLIC_KEY" -o "${ARCHIVE}.age" "$ARCHIVE"
rm -f "$ARCHIVE"

# 5) Envoi vers R2 via rclone (config entièrement par variables d'environnement,
#    donc aucun fichier de config à déposer ni à protéger sur la VM).
command -v rclone >/dev/null 2>&1 || fail "rclone is required to upload backups"
export RCLONE_CONFIG_R2_TYPE=s3
export RCLONE_CONFIG_R2_PROVIDER=Cloudflare
export RCLONE_CONFIG_R2_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export RCLONE_CONFIG_R2_ENDPOINT="https://${CLOUDFLARE_ACCOUNT_ID}.r2.cloudflarestorage.com"
export RCLONE_CONFIG_R2_NO_CHECK_BUCKET=true

OBJECT="${PREFIX}/$(basename "${ARCHIVE}.age")"
log "Uploading to r2:${R2_BUCKET}/${OBJECT}"
rclone copyto "${ARCHIVE}.age" "r2:${R2_BUCKET}/${OBJECT}" \
  --s3-no-check-bucket --retries 3 \
  || fail "upload to R2 failed"

# 6) Rétention à deux niveaux, bornée des deux côtés.
#    Les mensuelles étaient auparavant conservées sans limite : le stockage
#    croissait indéfiniment. Les deux niveaux sont désormais purgés.
RETAIN_DAILY_DAYS="${BACKUP_RETAIN_DAILY_DAYS:-14}"
RETAIN_MONTHLY_MONTHS="${BACKUP_RETAIN_MONTHLY_MONTHS:-12}"

# Le 1er du mois, promouvoir la copie du jour en archive mensuelle — avant la
# purge, pour qu'une archive fraîche ne soit jamais éligible à l'effacement.
if [ "$(date -u +%d)" = "01" ]; then
  log "Promoting to monthly archive"
  rclone copyto "r2:${R2_BUCKET}/${OBJECT}" \
    "r2:${R2_BUCKET}/${ENV}/monthly/$(basename "${ARCHIVE}.age")" --retries 3 \
    || log "⚠️  monthly copy failed"
fi

log "Pruning dailies older than ${RETAIN_DAILY_DAYS}d, monthlies older than ${RETAIN_MONTHLY_MONTHS}mo"
rclone delete "r2:${R2_BUCKET}/${ENV}" \
  --min-age "${RETAIN_DAILY_DAYS}d" --exclude "monthly/**" --retries 3 \
  || log "⚠️  daily retention pass failed (backup itself succeeded)"
rclone delete "r2:${R2_BUCKET}/${ENV}/monthly" \
  --min-age "$(( RETAIN_MONTHLY_MONTHS * 31 ))d" --retries 3 \
  || log "⚠️  monthly retention pass failed (backup itself succeeded)"

REMAINING="$(rclone lsf "r2:${R2_BUCKET}/${ENV}" --recursive --include '*.age' 2>/dev/null | wc -l | tr -d ' ')"
log "Objects retained for ${ENV}: ${REMAINING}"

SIZE="$(du -h "${ARCHIVE}.age" | cut -f1)"
log "✅ Backup complete: ${OBJECT} (${SIZE}, ${TABLES} tables verified)"
