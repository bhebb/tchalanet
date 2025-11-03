#!/usr/bin/env bash
set -euo pipefail

# local-setup-env.sh [ENV]
# Prépare un environnement local pour l'infra Docker Compose.
# - par défaut ENV=dev
# - crée/valide les réseaux Docker edge-<env> et back-<env>
# - génère .env.merged (env-prepare)
# - crée envs/<env>/compose.env s'il est absent (ENV/DOCKER_NETWORK_*)
# - affiche un récapitulatif des fichiers pris en compte

ENV_INPUT=${1:-dev}
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
log() { echo "[local-setup:$ENV_INPUT] $*"; }

# 1) compose.env: créer s'il n'existe pas (AVANT setup-networks)
COMPOSE_ENV_PATH="envs/$ENV_INPUT/compose.env"
if [ ! -f "$COMPOSE_ENV_PATH" ]; then
  log "Création $COMPOSE_ENV_PATH (par défaut)"
  mkdir -p "envs/$ENV_INPUT"
  cat > "$COMPOSE_ENV_PATH" <<EOF
# Compose-time variables (auto-généré)
ENV=$ENV_INPUT
DOCKER_NETWORK_EDGE=edge-$ENV_INPUT
DOCKER_NETWORK_BACK=back-$ENV_INPUT
EOF
else
  log "compose.env existant détecté: $COMPOSE_ENV_PATH"
fi

# 2) Réseaux Docker
if [ -x scripts/utils/setup-networks.sh ]; then
  log "Création/validation des réseaux Docker (edge/back)"
  scripts/utils/setup-networks.sh "$ENV_INPUT"
else
  log "scripts/utils/setup-networks.sh introuvable (skip)"
fi

# 3) Préparation des envs (.env.merged)
log "Préparation des envs (.env.merged)"
make -s env-prepare ENV="$ENV_INPUT"

# 4) Récapitulatif
MERGED_PATH="envs/$ENV_INPUT/.env.merged"
SECRETS_PATH="envs/$ENV_INPUT/.secrets"

bold "Résumé"
echo " - ENV: $ENV_INPUT"
echo " - compose.env: ${COMPOSE_ENV_PATH} $( [ -f "$COMPOSE_ENV_PATH" ] && echo '(OK)' || echo '(manquant)' )"
echo " - .env.merged: ${MERGED_PATH} $( [ -f "$MERGED_PATH" ] && echo '(OK)' || echo '(manquant)' )"
echo " - .secrets: ${SECRETS_PATH} $( [ -f "$SECRETS_PATH" ] && echo '(OK)' || echo '(manquant)' )"

# 5) Conseils rapides
if [ ! -f "$SECRETS_PATH" ]; then
  echo "(note) Aucun .secrets pour $ENV_INPUT. Pensez à le créer (Doppler/CI) si nécessaire."
fi

log "Terminé. Vous pouvez lancer: make up-all ENV=$ENV_INPUT ou make env-local ENV=$ENV_INPUT"

