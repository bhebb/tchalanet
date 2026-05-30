#!/usr/bin/env bash
set -euo pipefail

# generate-secrets.sh - Génère tous les secrets nécessaires pour Doppler

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${BLUE}→${NC} $*"; }
success() { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC}  $*"; }

ENV="${1:-staging}"

if [[ ! "$ENV" =~ ^(staging|prod|dev)$ ]]; then
  echo "Usage: $0 <staging|prod|dev>"
  exit 1
fi

echo "# Génération des secrets pour l'environnement: $ENV"
echo ""

# Fonction pour générer un secret sécurisé
generate_secret() {
  local length="${1:-32}"
  openssl rand -base64 "$length" | tr -d '\n'
}

# Génération des secrets en variables pour réutilisation
POSTGRES_PASSWORD="$(generate_secret 32)"
KC_DB_PASSWORD="$(generate_secret 32)"
APP_DB_PASSWORD="$(generate_secret 32)"
KC_BOOTSTRAP_ADMIN_PASSWORD="$(generate_secret 24)"
REDIS_PASSWORD="$(generate_secret 32)"

echo "# ======================================"
echo "# Secrets Tchalanet - Environment: $ENV"
echo "# Généré le: $(date '+%Y-%m-%d %H:%M:%S')"
echo "# ======================================"
echo ""
echo "# Instructions:"
echo "# 1. Copier ces secrets dans Doppler (projet: tchalanet, config: $ENV)"
echo "# 2. Ou sauvegarder dans envs/$ENV/.secrets (chmod 600)"
echo "# 3. Ne JAMAIS committer ce fichier !"
echo ""

echo "# === Base de données ==="
echo "POSTGRES_PASSWORD=$POSTGRES_PASSWORD"
echo "KC_DB_PASSWORD=$KC_DB_PASSWORD"
echo "APP_DB_PASSWORD=$APP_DB_PASSWORD"
echo ""

echo "# === SPRING_DATASOURCE_PASSWORD ==="
echo "SPRING_DATASOURCE_PASSWORD=$APP_DB_PASSWORD"
echo ""

echo "# === Keycloak ==="
echo "KC_BOOTSTRAP_ADMIN_PASSWORD=$KC_BOOTSTRAP_ADMIN_PASSWORD"
echo ""

echo "# === Redis ==="
echo "REDIS_PASSWORD=$REDIS_PASSWORD"
echo ""

echo "# === Analytics & Monitoring (optionnel) ==="
echo "GA_MEASUREMENT_ID="
echo "SENTRY_DSN="
echo ""

echo "# === Email/SMTP (optionnel) ==="
echo "SMTP_PASSWORD="
echo ""

echo "# Secrets générés avec succès !"
echo ""

