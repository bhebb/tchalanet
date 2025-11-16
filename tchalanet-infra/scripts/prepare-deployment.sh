#!/usr/bin/env bash
set -euo pipefail

# prepare-deployment.sh
# Prépare tous les fichiers pour le déploiement staging

echo "🔧 Préparation du Déploiement Staging"
echo "════════════════════════════════════════"
echo ""

cd "$(dirname "$0")/../.."

# 1. Rendre tous les scripts exécutables
echo "1️⃣  Rendre les scripts exécutables..."
find scripts -name "*.sh" -type f -exec chmod +x {} \;
echo "✅ Scripts exécutables"

# 2. Vérifier les fichiers critiques
echo ""
echo "2️⃣  Vérification des fichiers critiques..."

CRITICAL_FILES=(
  "compose/docker-compose-postgres.yml"
  "envs/common/compose.env"
  "envs/staging/compose.env"
  "envs/dev/compose.env"
  "scripts/remote/deploy-staging.sh"
  "scripts/remote/reset-postgres-staging.sh"
  "scripts/remote/debug-postgres-env.sh"
)

ALL_OK=true
for file in "${CRITICAL_FILES[@]}"; do
  if [ -f "$file" ]; then
    echo "  ✅ $file"
  else
    echo "  ❌ $file MANQUANT"
    ALL_OK=false
  fi
done

if [ "$ALL_OK" = false ]; then
  echo ""
  echo "❌ Certains fichiers critiques sont manquants !"
  exit 1
fi

# 3. Vérifier que docker-compose-postgres.yml a env_file:
echo ""
echo "3️⃣  Vérification de docker-compose-postgres.yml..."
if grep -q "env_file:" compose/docker-compose-postgres.yml; then
  echo "  ✅ env_file: présent"
else
  echo "  ❌ env_file: MANQUANT - le fichier n'a pas été corrigé !"
  exit 1
fi

# 4. Vérifier compose/.env
echo ""
echo "4️⃣  Génération de compose/.env pour dev..."
if [ ! -f "compose/.env" ]; then
  cat > compose/.env <<'ENVHEADER'
# Auto-generated for local dev
ENV=dev
ENVHEADER

  for compose_file in envs/common/compose.env envs/dev/compose.env; do
    if [ -f "$compose_file" ]; then
      grep -E "^(API_IMAGE_BASE|KEYCLOAK_IMAGE|IMAGE_TAG|DOCKER_PREFIX|DOCKER_NETWORK_)" "$compose_file" 2>/dev/null >> compose/.env || true
    fi
  done

  tac compose/.env | awk -F= '!seen[$1]++' | tac > compose/.env.tmp && mv compose/.env.tmp compose/.env
  echo "  ✅ compose/.env créé"
else
  echo "  ✅ compose/.env existe"
fi

echo ""
echo "════════════════════════════════════════"
echo "✅ Préparation terminée !"
echo ""
echo "📋 Prochaines étapes:"
echo ""
echo "1. Pousser l'infra vers staging:"
echo "   ./scripts/remote/push-infra-bkup.sh 91.98.194.162 staging"
echo ""
echo "2. Debug Postgres (optionnel):"
echo "   ssh tchalanet_stg 'bash /opt/tchalanet-infra/scripts/remote/debug-postgres-env.sh'"
echo ""
echo "3. Reset Postgres:"
echo "   ssh tchalanet_stg 'bash /opt/tchalanet-infra/scripts/remote/reset-postgres-staging.sh'"
echo ""
echo "4. Déployer:"
echo "   ssh tchalanet_stg 'bash -s' < ./scripts/remote/deploy-staging.sh"

