#!/usr/bin/env bash
set -euo pipefail

# Couleurs pour les messages
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Fonctions pour les messages
log() { echo -e "${BLUE}→${NC} $*" >&2; }
success() { echo -e "${GREEN}✔${NC} $*" >&2; }
error() { echo -e "${RED}✖${NC} $*" >&2; exit 1; }

usage() {
  cat <<EOF
Usage: $(basename "$0") [staging|production]

Generate cloud-init.yml for the specified environment.

Examples:
  $(basename "$0") staging   # Génère cloud-init.yml pour staging
  $(basename "$0") production # Génère cloud-init.yml pour production
EOF
  exit "${1:-0}"
}

# Vérifier l'argument
ENV="${1:-}"
[[ -z "$ENV" ]] && usage 1

case "$ENV" in
  staging)
    SERVER_NAME="stg-app"
    SSH_KEY_FILE="$HOME/.ssh/tchalanet_stg.pub"
    ;;
  production)
    SERVER_NAME="prod-app"
    SSH_KEY_FILE="$HOME/.ssh/tchalanet_prod.pub"
    ;;
  *) error "Environment must be 'staging' or 'production'" ;;
esac

# Vérifier que la clé SSH existe
[[ -f "$SSH_KEY_FILE" ]] || error "SSH key file not found: $SSH_KEY_FILE"

# Lire la clé SSH
SSH_KEY=$(cat "$SSH_KEY_FILE")

# Vérifier que le template existe
TEMPLATE="$(dirname "$0")/cloud-init.template.yml"
[[ -f "$TEMPLATE" ]] || error "Template file not found: $TEMPLATE"

# Générer le fichier cloud-init.yml
OUTPUT="$(dirname "$0")/cloud-init.yml"
log "Generating cloud-init.yml for $ENV environment..."

# Remplacer les variables dans le template (ne PAS réinsérer 'users:' une seconde fois)
sed \
  -e "s/\${SERVER_NAME}/$SERVER_NAME/g" \
  -e "s/\${ENV}/$ENV/g" \
  -e "s|\${SSH_KEY}|$SSH_KEY|g" \
  "$TEMPLATE" > "$OUTPUT"

success "Generated $OUTPUT"
