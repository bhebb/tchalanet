#!/usr/bin/env bash
set -euo pipefail

# generate-meili-master-key.sh <env>
# Usage LOCAL uniquement (dev/staging/prod en local).
# Génère une MeiliSearch master key si absente (ou trop courte) et l'écrit dans envs/<env>/.secrets
# Pour serveur remote, utiliser scripts/remote/04-generate-meili-master-key.sh

ENV=${1:-dev}
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SECRETS_FILE="$ROOT_DIR/envs/$ENV/.secrets"
mkdir -p "$ROOT_DIR/envs/$ENV"

command -v openssl >/dev/null 2>&1 || { echo "openssl requis (brew install openssl)" >&2; exit 1; }

if [ ! -f "$SECRETS_FILE" ]; then
  echo "# Generated secrets for env: $ENV" > "$SECRETS_FILE"
fi

EXISTING=$(grep -m1 '^MEILI_MASTER_KEY=' "$SECRETS_FILE" 2>/dev/null || true)
if [ -n "$EXISTING" ]; then
  VAL=${EXISTING#MEILI_MASTER_KEY=}
  LEN=${#VAL}
  if [ $LEN -ge 16 ]; then
    echo "MEILI_MASTER_KEY déjà présente (longueur=$LEN), inchangé: $SECRETS_FILE"
    exit 0
  else
    echo "MEILI_MASTER_KEY trop courte (len=$LEN) → remplacement"
    grep -v '^MEILI_MASTER_KEY=' "$SECRETS_FILE" > "$SECRETS_FILE.tmp" && mv "$SECRETS_FILE.tmp" "$SECRETS_FILE"
  fi
fi

NEW=$(openssl rand -base64 24 | tr '+/' '-_' | tr -d '=')
if [ ${#NEW} -lt 16 ]; then
  echo "generated key too short: ${#NEW}" >&2
  exit 1
fi
printf "MEILI_MASTER_KEY=%s\n" "$NEW" >> "$SECRETS_FILE"
chmod 600 "$SECRETS_FILE"
echo "✔ MEILI_MASTER_KEY écrite dans $SECRETS_FILE (longueur=${#NEW})"
