ens#!/usr/bin/env bash
set -euo pipefail

# setup-doppler.sh - Configuration interactive de Doppler pour Tchalanet

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}→${NC} $*"; }
success() { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC}  $*"; }
error() { echo -e "${RED}✖${NC} $*"; exit 1; }

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Configuration Doppler - Tchalanet Infrastructure      ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Vérifier que doppler CLI est installé
if ! command -v doppler >/dev/null 2>&1; then
  warn "Doppler CLI non installé"
  echo ""
  echo "Installation:"
  echo "  macOS:  brew install dopplerhq/cli/doppler"
  echo "  Linux:  curl -Ls https://cli.doppler.com/install.sh | sh"
  echo ""
  read -p "Continuer sans CLI (configuration manuelle) ? (y/N) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
  USE_CLI=false
else
  success "Doppler CLI détecté: $(doppler --version)"
  USE_CLI=true
fi

echo ""
log "Étape 1/5: Vérification de la connexion Doppler"

if [ "$USE_CLI" = true ]; then
  if ! doppler me >/dev/null 2>&1; then
    warn "Non connecté à Doppler"
    echo ""
    log "Exécutez: doppler login"
    exit 1
  fi
  success "Connecté à Doppler"
  doppler me 2>/dev/null | grep -E "(name|email)" || true
fi

echo ""
log "Étape 2/5: Création du projet 'tchalanet'"

if [ "$USE_CLI" = true ]; then
  if doppler projects get tchalanet >/dev/null 2>&1; then
    success "Projet 'tchalanet' existe déjà"
  else
    read -p "Créer le projet 'tchalanet' ? (Y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
      doppler projects create tchalanet --description "Tchalanet Infrastructure Secrets"
      success "Projet 'tchalanet' créé"
    fi
  fi
else
  echo "  → Créer manuellement sur: https://dashboard.doppler.com"
  echo "    Nom: tchalanet"
  echo "    Description: Tchalanet Infrastructure Secrets"
fi

echo ""
log "Étape 3/5: Vérification des configurations"

if [ "$USE_CLI" = true ]; then
  log "Affichage des configs disponibles dans le projet 'tchalanet'..."
  doppler configs --project tchalanet 2>/dev/null || {
    warn "Impossible de lister les configs. Le projet vient peut-être d'être créé."
    echo "  → Vérifier sur: https://dashboard.doppler.com/workplace/tchalanet"
    echo "  → Doppler crée automatiquement des environments par défaut (dev, stg, prd)"
  }
  echo ""
  success "Les configs Doppler par défaut:"
  echo "  - dev (Development)"
  echo "  - stg (Staging)"
  echo "  - prd (Production)"
  echo ""
  warn "Note: On utilisera 'staging' en interne mais 'stg' pour Doppler."
else
  echo "  → Les environments sont créés automatiquement par Doppler"
  echo "  → Vérifier sur: https://dashboard.doppler.com/workplace/tchalanet"
  echo "  → Configs Doppler: dev, stg, prd"
fi

echo ""
log "Étape 4/5: Génération et ajout des secrets"

ENV=""
while [[ ! "$ENV" =~ ^(dev|staging|production)$ ]]; do
  echo ""
  echo "Pour quel environnement voulez-vous configurer les secrets ?"
  echo "  1) dev"
  echo "  2) staging"
  echo "  3) production"
  echo "  4) Tous (dev, staging, production)"
  echo "  5) Passer cette étape"
  read -p "Choix (1-5): " choice

  case $choice in
    1) ENV="dev" ;;
    2) ENV="staging" ;;
    3) ENV="production" ;;
    4) ENV="all" ;;
    5) ENV="skip" ;;
    *) warn "Choix invalide" ;;
  esac
done

if [ "$ENV" != "skip" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

  # Fonction pour mapper les noms d'environnement internes vers les configs Doppler
  map_env_to_doppler_config() {
    case "$1" in
      staging) echo "stg" ;;
      production) echo "prd" ;;
      *) echo "$1" ;;  # dev reste dev
    esac
  }

  generate_for_env() {
    local env=$1
    local doppler_config=$(map_env_to_doppler_config "$env")

    echo ""
    log "Configuration des secrets pour: $env (config Doppler: $doppler_config)"

    # Vérifier si un fichier .secrets existe déjà
    SECRETS_FILE="$SCRIPT_DIR/../envs/$env/.secrets"
    if [ -f "$SECRETS_FILE" ]; then
      warn "Fichier de secrets existant détecté: $SECRETS_FILE"
      echo ""
      echo "Options:"
      echo "  1) Uploader le fichier existant vers Doppler (recommandé)"
      echo "  2) Générer de nouveaux secrets (écrasera dans Doppler)"
      echo "  3) Passer"
      read -p "Choix (1-3): " secrets_choice

      case $secrets_choice in
        1)
          # Upload via create-secrets-from-env.sh
          if [ "$USE_CLI" = true ]; then
            log "Upload des secrets existants vers Doppler..."
            if [ -x "$SCRIPT_DIR/create-secrets-from-env.sh" ]; then
              "$SCRIPT_DIR/create-secrets-from-env.sh" \
                "$SECRETS_FILE" \
                --project tchalanet \
                --config "$doppler_config"
              success "Secrets uploadés depuis $SECRETS_FILE vers config '$doppler_config'"
            else
              warn "Script create-secrets-from-env.sh non trouvé ou non exécutable"
              echo "Affichage du fichier pour copie manuelle:"
              cat "$SECRETS_FILE"
            fi
          else
            echo "Contenu de $SECRETS_FILE (à copier manuellement dans Doppler config '$doppler_config'):"
            cat "$SECRETS_FILE"
          fi
          return
          ;;
        2)
          log "Génération de nouveaux secrets (les anciens seront ignorés)..."
          ;;
        3)
          log "Secrets non modifiés pour $env"
          return
          ;;
        *)
          warn "Choix invalide, génération de nouveaux secrets..."
          ;;
      esac
    fi

    # Génération de nouveaux secrets
    if [ "$USE_CLI" = true ]; then
      read -p "Ajouter automatiquement dans Doppler ? (Y/n) " -n 1 -r
      echo
      if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
        doppler setup --project tchalanet --config "$doppler_config" --no-interactive

        log "Ajout des secrets..."
        doppler secrets set POSTGRES_PASSWORD="$(openssl rand -base64 32)" --silent
        doppler secrets set APP_DB_PASSWORD="$(openssl rand -base64 32)" --silent
        doppler secrets set REDIS_PASSWORD="$(openssl rand -base64 32)" --silent
        doppler secrets set GA_MEASUREMENT_ID="" --silent

        success "Secrets ajoutés dans Doppler (config: $doppler_config)"
      else
        "$SCRIPT_DIR/generate-secrets.sh" "$env"
      fi
    else
      "$SCRIPT_DIR/generate-secrets.sh" "$env"
    fi
  }

  if [ "$ENV" = "all" ]; then
    generate_for_env "dev"
    generate_for_env "staging"
    generate_for_env "production"
  else
    generate_for_env "$ENV"
  fi
fi

echo ""
log "Étape 5/5: Création des Service Tokens"

if [ "$USE_CLI" = true ]; then
  echo ""
  read -p "Créer les Service Tokens pour staging et production ? (Y/n) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then

    # Token staging (config 'stg')
    if doppler configs tokens list --project tchalanet --config stg 2>/dev/null | grep -q "staging-server"; then
      success "Token staging-server existe déjà"
    else
      log "Création du token staging..."
      TOKEN_STG=$(doppler configs tokens create staging-server \
        --project tchalanet \
        --config stg \
        --max-age 0 \
        --plain 2>/dev/null || echo "")

      if [ -n "$TOKEN_STG" ]; then
        success "Token staging créé"
        echo ""
        echo "╔════════════════════════════════════════════════════════╗"
        echo "║  DOPPLER_TOKEN_STG (à ajouter dans GitHub Secrets)   ║"
        echo "╠════════════════════════════════════════════════════════╣"
        echo "║  $TOKEN_STG"
        echo "╚════════════════════════════════════════════════════════╝"
        echo ""
        warn "⚠️  Sauvegarder ce token, il ne sera plus affiché !"
      fi
    fi

    # Token production (config 'prd')
    if doppler configs tokens list --project tchalanet --config prd 2>/dev/null | grep -q "production-server"; then
      success "Token production-server existe déjà"
    else
      log "Création du token production..."
      TOKEN_PROD=$(doppler configs tokens create production-server \
        --project tchalanet \
        --config prd \
        --max-age 0 \
        --plain 2>/dev/null || echo "")

      if [ -n "$TOKEN_PROD" ]; then
        success "Token production créé"
        echo ""
        echo "╔════════════════════════════════════════════════════════╗"
        echo "║  DOPPLER_TOKEN_PROD (à ajouter dans GitHub Secrets)  ║"
        echo "╠════════════════════════════════════════════════════════╣"
        echo "║  $TOKEN_PROD"
        echo "╚════════════════════════════════════════════════════════╝"
        echo ""
        warn "⚠️  Sauvegarder ce token, il ne sera plus affiché !"
      fi
    fi
  fi
else
  echo "  → Créer manuellement les Service Tokens sur:"
  echo "    https://dashboard.doppler.com/workplace/tchalanet/stg/access"
  echo "    https://dashboard.doppler.com/workplace/tchalanet/prd/access"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  ✅ Configuration Doppler terminée !                     ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

log "Prochaines étapes:"
echo ""
echo "1. Ajouter les tokens dans GitHub Secrets:"
echo "   → Settings → Secrets and variables → Actions"
echo "   → New secret: DOPPLER_TOKEN_STG"
echo "   → New secret: DOPPLER_TOKEN_PROD"
echo ""
echo "2. Tester le téléchargement des secrets:"
echo "   → doppler secrets --project tchalanet --config stg"
echo ""
echo "3. Déployer l'infra avec les secrets:"
echo "   → cd tchalanet-infra"
echo "   → ./scripts/remote/push-infra-bkup.sh <IP> staging"
echo "   → ssh tchalanet_stg"
echo "   → export DOPPLER_TOKEN='dp.st.xxx...'"
echo "   → docker run ... doppler secrets download ..."
echo ""

success "Documentation complète: docs/DOPPLER-SETUP-GUIDE.md"
