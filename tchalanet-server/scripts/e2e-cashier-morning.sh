#!/usr/bin/env zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/.tmp"
mkdir -p "$LOG_DIR"

# Support pour fichiers .env par environnement
# - ENV_FILE=".env.local" pour développement local
# - ENV_FILE=".env.staging" pour staging
# - ENV_FILE=".env.prod" pour production
# Défaut: scripts/.env
ENV_FILE="${ENV_FILE:=$ROOT_DIR/scripts/.env}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

: "${TCH_BASE_URL:=http://localhost:8083/api/v1}"
: "${TCH_AUTH_CLIENT_ID:=tchalanet-swagger}"
: "${TCH_GAME_PROFILES:=BOLET,MARYAJ,LOTO3}"
: "${TCH_SELECTION_PLAN:=BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1}"
: "${TCH_SELECTIONS_PER_TICKET:=3}"
: "${TCH_DRAW_LIMIT:=20}"
: "${TCH_SELL_MODE:=SINGLE_TICKET_MULTI_GAME}"
export TCH_BASE_URL TCH_AUTH_CLIENT_ID TCH_GAME_PROFILES TCH_SELECTION_PLAN TCH_SELECTIONS_PER_TICKET TCH_DRAW_LIMIT TCH_SELL_MODE

require_env() {
  local name="$1"
  local value="$2"
  if [[ -z "$value" ]]; then
    print -u2 -- "[ERREUR] Variable obligatoire manquante: $name"
    exit 1
  fi
}

if [[ -z "${TCH_SUPER_ADMIN_TOKEN:-}" ]]; then
  require_env 'TCH_SUPER_ADMIN_USERNAME' "${TCH_SUPER_ADMIN_USERNAME:-}"
  require_env 'TCH_SUPER_ADMIN_PASSWORD' "${TCH_SUPER_ADMIN_PASSWORD:-}"
fi
if [[ -z "${TCH_SELLER_TOKEN:-}" ]]; then
  require_env 'TCH_SELLER_USERNAME' "${TCH_SELLER_USERNAME:-}"
  require_env 'TCH_SELLER_PASSWORD' "${TCH_SELLER_PASSWORD:-}"
fi

cd "$ROOT_DIR"
exec zsh "$ROOT_DIR/scripts/e2e-phase2-sell-pdf.sh" >> "$LOG_DIR/e2e-morning.log" 2>&1
