#!/usr/bin/env bash
set -euo pipefail

# 03-rotate-meili-master-key.sh <env>
# À exécuter SUR LE SERVEUR (dans /opt/tchalanet-infra).
# Génère une nouvelle clé maître Meilisearch, met à jour envs/<env>/.secrets et redémarre Meilisearch.
# ATTENTION: la rotation invalide les API keys dérivées de l'ancienne master key.

ENV=${1:-staging}
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SECRETS_FILE="$ROOT_DIR/envs/$ENV/.secrets"
BACKUP_DIR="$ROOT_DIR/envs/$ENV/backups"

command -v openssl >/dev/null 2>&1 || { echo "openssl requis (apt install openssl)" >&2; exit 1; }

if [ ! -f "$SECRETS_FILE" ]; then
  echo "Secrets file not found: $SECRETS_FILE" >&2
  exit 1
fi

# générer une nouvelle clé (url-safe base64 ~32 chars)
NEW_KEY=$(openssl rand -base64 24 | tr '+/' '-_' | tr -d '=')
if [ ${#NEW_KEY} -lt 16 ]; then
  echo "Generated key too short (${#NEW_KEY}); aborting" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
cp "$SECRETS_FILE" "$BACKUP_DIR/.secrets.$TIMESTAMP"

# remplacer ou ajouter MEILI_MASTER_KEY
if grep -q '^MEILI_MASTER_KEY=' "$SECRETS_FILE"; then
  awk -v k="$NEW_KEY" 'BEGIN{FS=OFS="="} $1=="MEILI_MASTER_KEY"{$2=k;print;next} {print}' "$SECRETS_FILE" > "$SECRETS_FILE.tmp" && mv "$SECRETS_FILE.tmp" "$SECRETS_FILE"
else
  printf "MEILI_MASTER_KEY=%s\n" "$NEW_KEY" >> "$SECRETS_FILE"
fi
chmod 600 "$SECRETS_FILE"

# redémarrer Meilisearch avec docker compose (project + meilisearch)
MERGED=$(mktemp)
[ -f "$ROOT_DIR/envs/$ENV/compose.env" ] && cat "$ROOT_DIR/envs/$ENV/compose.env" > "$MERGED" || true
[ -f "$ROOT_DIR/envs/common/.env" ] && echo >> "$MERGED" && cat "$ROOT_DIR/envs/common/.env" >> "$MERGED" || true
[ -f "$ROOT_DIR/envs/$ENV/.env" ] && echo >> "$MERGED" && cat "$ROOT_DIR/envs/$ENV/.env" >> "$MERGED" || true
[ -f "$SECRETS_FILE" ] && echo >> "$MERGED" && cat "$SECRETS_FILE" >> "$MERGED" || true

cd "$ROOT_DIR"
echo "Restarting MeiliSearch for env $ENV..."
ENV="$ENV" docker compose --env-file "$MERGED" \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-meilisearch.yml \
  up -d --remove-orphans
rm -f "$MERGED"

echo "✔ Rotation ok. Nouvelle clé longueur: ${#NEW_KEY}. Backup: $BACKUP_DIR/.secrets.$TIMESTAMP"

