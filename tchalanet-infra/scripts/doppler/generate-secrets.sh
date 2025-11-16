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

if [[ ! "$ENV" =~ ^(staging|production|dev)$ ]]; then
  echo "Usage: $0 <staging|production|dev>"
  exit 1
fi

log "Génération des secrets pour l'environnement: $ENV"
echo ""

# Fonction pour générer un secret sécurisé
generate_secret() {
  local length="${1:-32}"
  openssl rand -base64 "$length" | tr -d '\n'
}

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

# Génération des secrets
log "Génération des secrets de base de données..."
echo "# === Base de données ==="
echo "POSTGRES_PASSWORD=$(generate_secret 32)"
echo "KC_DB_PASSWORD=$(generate_secret 32)"
echo "APP_DB_PASSWORD=$(generate_secret 32)"
echo "UNLEASH_DB_PASSWORD=$(generate_secret 32)"
echo ""

log "Génération des secrets Keycloak..."
echo "# === Keycloak ==="
echo "KC_BOOTSTRAP_ADMIN_PASSWORD=$(generate_secret 24)"
echo ""

log "Génération des secrets Redis..."
echo "# === Redis ==="
echo "REDIS_PASSWORD=$(generate_secret 32)"
echo ""

log "Génération des secrets Meilisearch..."
echo "# === Meilisearch ==="
echo "MEILI_MASTER_KEY=$(generate_secret 32)"
echo ""

log "Secrets optionnels (à remplir manuellement si nécessaire)..."
echo "# === Analytics & Monitoring (optionnel) ==="
echo "GA_MEASUREMENT_ID="
echo "SENTRY_DSN="
echo ""

echo "# === Email/SMTP (optionnel) ==="
echo "SMTP_PASSWORD="
echo ""

warn "Tokens Unleash à générer APRÈS démarrage d'Unleash:"
echo "# === Unleash Tokens (à générer dans Unleash UI) ==="
echo "# 1. Démarrer Unleash: docker compose up -d unleash"
echo "# 2. Se connecter à https://flags.${ENV}.tchalanet.com (ou http://localhost:4242)"
echo "# 3. Créer un Personal Access Token (Profile → Access Tokens)"
echo "# 4. Créer les tokens suivants:"
echo "#    - Server Token (API access)"
echo "#    - Frontend Token (Client SDK)"
echo ""
echo "UNLEASH_PERSONAL_TOKEN=  # Format: user:xxxxx..."
echo "UNLEASH_SERVER_TOKEN=    # Format: *:development.xxxxx... ou *:production.xxxxx..."
echo "UNLEASH_FRONTEND_TOKEN=  # Format: *:development.xxxxx... ou *:production.xxxxx..."
echo ""

success "Secrets générés avec succès !"
echo ""
log "Prochaines étapes:"
echo "  1. Copier ces secrets dans Doppler:"
echo "     → https://dashboard.doppler.com/workplace/tchalanet/$ENV"
echo ""
echo "  2. Ou sauvegarder localement (test uniquement):"
echo "     → ./scripts/doppler/generate-secrets.sh $ENV > envs/$ENV/.secrets"
echo "     → chmod 600 envs/$ENV/.secrets"
echo ""
echo "  3. Créer un Service Token dans Doppler:"
echo "     → doppler configs tokens create ${ENV}-server --project tchalanet --config $ENV"
echo ""
echo "  4. Ajouter le token dans GitHub Secrets:"
echo "     → DOPPLER_TOKEN_$(echo $ENV | tr '[:lower:]' '[:upper:]' | sed 's/STAGING/STG/')"

