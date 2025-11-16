#!/usr/bin/env bash
set -euo pipefail

ENV="${1:-staging}"
source ./envs/"$ENV"/.env

# Fonction pour vérifier un service HTTP
check_http() {
  local name=$1
  local url=$2
  local max_retries=${3:-30}
  local retry=0

  echo -n "Checking $name ($url)... "
  while [ $retry -lt $max_retries ]; do
    if curl -fsSL "$url" >/dev/null 2>&1; then
      echo "✅"
      return 0
    fi
    echo -n "."
    retry=$((retry + 1))
    sleep 2
  done
  echo "❌"
  return 1
}

# Vérifier que les conteneurs sont up
echo "Checking containers..."
docker compose -f docker-compose.yml -f docker-compose-"$ENV".yml ps --format json | jq -r '.[] | "\(.Service): \(.State)"'

# Vérifier les services HTTP
check_http "Traefik" "http://localhost:8081/ping" || true
check_http "API" "https://api.$ENV.tchalanet.com/health" || true
check_http "Keycloak" "https://kc.$ENV.tchalanet.com/health" || true
check_http "Unleash" "https://$FLAGS_HOST/health" || true

# Vérifier Postgres
echo -n "Checking Postgres... "
if docker compose -f docker-compose.yml -f docker-compose-"$ENV".yml exec -T postgres pg_isready; then
  echo "✅"
else
  echo "❌"
fi

# Vérifier Redis
echo -n "Checking Redis... "
if docker compose -f docker-compose.yml -f docker-compose-"$ENV".yml exec -T redis redis-cli ping | grep -q "PONG"; then
  echo "✅"
else
  echo "❌"
fi

# Afficher les logs des services qui ne sont pas up
echo "Recent logs for failed services:"
docker compose -f docker-compose.yml -f docker-compose-"$ENV".yml ps --format json | \
  jq -r '.[] | select(.State != "running") | .Service' | \
  xargs -I {} docker compose -f docker-compose.yml -f docker-compose-"$ENV".yml logs --tail 50 {}
