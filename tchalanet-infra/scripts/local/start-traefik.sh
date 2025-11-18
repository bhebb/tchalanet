#!/bin/bash
# Script pour lancer Traefik avec le bon environnement
# Usage: ./start-traefik.sh [dev|staging|prod]

set -e

ENV="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
INFRA_DIR="$(dirname "$SCRIPTS_DIR")"
COMPOSE_DIR="$INFRA_DIR/compose"

# Validation de l'environnement
if [[ ! "$ENV" =~ ^(dev|staging|prod)$ ]]; then
  echo "❌ Erreur: environnement invalide '$ENV'"
  echo "Usage: $0 [dev|staging|prod]"
  exit 1
fi

echo "🚀 Démarrage de Traefik pour l'environnement: $ENV"
echo ""

# Sauvegarder l'ENV choisi (avant chargement des fichiers)
TARGET_ENV="$ENV"

# Charger les variables d'environnement
echo "→ Chargement des variables d'environnement..."
set -a
source "$INFRA_DIR/envs/common/compose.env"
source "$INFRA_DIR/envs/$TARGET_ENV/compose.env"
set +a

# Forcer l'ENV à la valeur passée en paramètre (override après source)
export ENV="$TARGET_ENV"
export DOCKER_NETWORK_EDGE="${DOCKER_NETWORK_EDGE}"
export DOCKER_NETWORK_BACK="${DOCKER_NETWORK_BACK}"

# Vérifier que le fichier de config existe
if [ ! -f "$INFRA_DIR/traefik/dynamic/env/${TARGET_ENV}.yaml" ]; then
  echo "❌ Erreur: fichier de configuration $INFRA_DIR/traefik/dynamic/env/${TARGET_ENV}.yaml introuvable"
  exit 1
fi

echo "✓ Configuration trouvée: traefik/dynamic/env/${TARGET_ENV}.yaml"
echo "✓ Variables chargées depuis envs/$TARGET_ENV/"
echo ""

# Créer le symlink active.yaml sur l'hôte (car le volume est en read-only)
echo "→ Création du lien symbolique vers la configuration active..."
ln -sf "$INFRA_DIR/traefik/dynamic/env/${TARGET_ENV}.yaml" "$INFRA_DIR/traefik/dynamic/active.yaml"
echo "✓ Lien créé: active.yaml -> env/${TARGET_ENV}.yaml"
echo ""

# Créer les réseaux via docker-compose-project.yml
cd "$COMPOSE_DIR"
echo "→ Création des réseaux Docker (via docker-compose-project.yml)..."
docker compose -f docker-compose-project.yml up --no-start 2>/dev/null || true
echo "✓ Réseaux créés/vérifiés"
echo ""

# Démarrer Traefik
echo "→ Démarrage du conteneur Traefik..."
docker compose -f docker-compose-project.yml -f docker-compose-traefik.yml up -d traefik

echo ""
echo "✅ Traefik démarré avec succès !"
echo ""
echo "📊 Informations:"
echo "   - Environment: $ENV"
echo "   - Container: ${DOCKER_PREFIX:-tchl}-traefik-${ENV}"
echo "   - Dashboard: http://localhost:8080 (ou https://traefik.${ENV}.tchalanet.com)"
echo ""
echo "📝 Voir les logs:"
echo "   docker logs -f ${DOCKER_PREFIX:-tchl}-traefik-${ENV}"
echo ""
echo "🔍 Vérifier la configuration active:"
echo "   docker exec ${DOCKER_PREFIX:-tchl}-traefik-${ENV} ls -la /etc/traefik/dynamic/"

