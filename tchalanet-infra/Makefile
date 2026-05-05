# tchalanet-infra/Makefile
SHELL := /bin/bash
.ONESHELL:

ENV ?= dev
ROOT := $(abspath $(CURDIR))
SVC  := $(ROOT)/scripts/utils/service-up.sh
WRAP := $(ROOT)/scripts/utils/run-compose-wrapper.sh

# Ajoute les chemins Docker fréquents (Homebrew/Intel/ARM)
export PATH := /opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:$(PATH)
DOCKER_BIN := $(shell command -v docker || echo docker)

ensure-exec:
	@chmod +x $(SVC) $(WRAP) $(ROOT)/scripts/utils/run-compose.sh || true

print-env:
	@echo "ENV=$(ENV)"
	@echo "ROOT=$(ROOT)"
	@echo "DOCKER_BIN=$(DOCKER_BIN)"

networks:
	@docker network create edge-$(ENV) 2>/dev/null || true
	@docker network create back-$(ENV) 2>/dev/null || true
	@docker network ls --format "table {{.ID}}\t{{.Name}}\t{{.Driver}}\t{{.Scope}}" | grep -E "(edge|back)-$(ENV)" || true

# 1) Génère traefik/dynamic/10-routers.yaml depuis env/<ENV>.yaml
render-traefik:
	@chmod +x ./scripts/utils/pre-render-traefik.sh || true
	@./scripts/utils/pre-render-traefik.sh $(if $(filter $(ENV),stg staging),staging.yaml,$(if $(filter $(ENV),prod production),prod.yaml,dev.yaml))

# 2) Génère le realm JSON Keycloak (désactive le scope custom par défaut)
get-realm:
	@chmod +x ./scripts/keycloak/get-realm.sh || true
	@ENABLE_TCH_SCOPE=0 ./scripts/keycloak/get-realm.sh $(if $(filter $(ENV),stg staging),staging,$(if $(filter $(ENV),prod production),prod,dev))

# 3) (Local) génère les certs mkcert si absents
mkcert-local:
	@[ -f ./traefik/certs/local-cert.pem ] && [ -f ./traefik/certs/local-key.pem ] \
	 || { echo "→ Generating mkcert dev certs"; mkdir -p ./traefik/certs; mkcert -install >/dev/null; \
	      mkcert -key-file ./traefik/certs/local-key.pem -cert-file ./traefik/certs/local-cert.pem \
	      traefik.localtest.me auth.localtest.me api.localtest.me flags.localtest.me stats.localtest.me app.localtest.me mob.localtest.me; }

# Force la régénération des certificats locaux mkcert (dev)
mkcert-recreate:
	@rm -f ./traefik/certs/local-cert.pem ./traefik/certs/local-key.pem || true
	@$(MAKE) mkcert-local

# 3b) (Staging) génère des certificats auto-signés
certs-staging:
	@chmod +x ./scripts/utils/generate-staging-certs.sh || true
	@./scripts/utils/generate-staging-certs.sh 91.98.194.162

# 4) ACME storage perms (utile stg/prod)
acme-perms:
	@touch ./traefik/acme.json && chmod 600 ./traefik/acme.json || true

# Core (legacy "up" : traefik + postgres + redis via service-up.sh)
up: env-merge render-traefik ensure-exec networks
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up traefik $(ENV)
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up postgres $(ENV)
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up redis $(ENV)

# Full (local): merge env -> render traefik -> mkcert (si local) -> get-realm -> up seq
up-all: env-merge render-traefik mkcert-local get-realm ensure-exec networks
	@chmod +x scripts/utils/up-seq.sh || true
	@scripts/utils/up-seq.sh $(ENV)

# Staging/Prod: ajoute acme-perms
up-staging: ENV=staging
up-staging: env-merge render-traefik certs-staging acme-perms get-realm ensure-exec networks
	@chmod +x scripts/utils/up-seq.sh || true
	@scripts/utils/up-seq.sh $(ENV)

up-prod: ENV=prod
up-prod: env-merge render-traefik acme-perms get-realm ensure-exec networks
	@chmod +x scripts/utils/up-seq.sh || true
	@scripts/utils/up-seq.sh $(ENV)

down:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down api $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down keycloak $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down unleash $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down postgres $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down redis $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down traefik $(ENV) || true

down-all:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down api $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down unleash $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down keycloak $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down redis $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down postgres $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down traefik $(ENV) || true

# ============================================================================
# Rebuild local (dev) - Build images avec Dockerfile locaux
# ============================================================================

rebuild-keycloak: env-merge get-realm
	@echo "→ Rebuild de l'image Keycloak locale..."
	@docker compose -f compose/docker-compose-project.yml \
	                -f compose/docker-compose.local-build.yml up -d --build keycloak
	@echo "✅ Keycloak rebuilé et redémarré"

rebuild-api:
	@echo "→ Build du JAR Spring Boot..."
	@cd ../tchalanet-server && ./mvnw -q -DskipTests package
	@echo "→ Rebuild de l'image API locale..."
	@docker compose -f compose/docker-compose-project.yml \
	                -f compose/docker-compose.local-build.yml up -d --build api
	@echo "✅ API rebuildée et redémarrée"

rebuild-all: rebuild-api rebuild-keycloak
	@echo "✅ API et Keycloak rebuildés"

# ============================================================================

# Per service (legacy, via service-up.sh)
up-api:      ensure-exec networks ; @ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up api $(ENV)
down-api:      ensure-exec networks ; @ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down api $(ENV)
up-keycloak: ensure-exec networks ; @ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up keycloak $(ENV)
up-unleash:  ensure-exec networks ; @ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up unleash $(ENV)
down-unleash:  ensure-exec networks ; @ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down unleash $(ENV)

logs-%:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" logs $* $(ENV)

config-%:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" config $* $(ENV)

ps:
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

env-merge:
	@chmod +x ./scripts/utils/merge-env.sh || true
	@./scripts/utils/merge-env.sh $(ENV)

# SSH helpers (requires hcloud CLI and server name)
ssh-staging:
	@ssh -i ~/.ssh/tchalanet_stg tch@$$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip')

ssh-prod:
	@ssh -i ~/.ssh/tchalanet_prod tch@$$(hcloud server describe prod-app -o json | jq -r '.public_net.ipv4.ip')

# Push infra to remote
push-staging:
	@./scripts/remote/push-infra-bkup.sh $$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip') staging

push-prod:
	@./scripts/remote/push-infra-bkup.sh $$(hcloud server describe prod-app -o json | jq -r '.public_net.ipv4.ip') prod

# Remote deploy (SSH + commands)
deploy-staging: push-staging
	@echo "→ Deploying to staging..."
	@ssh -i ~/.ssh/tchalanet_stg tch@$$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip') \
		'cd /opt/tchalanet-infra && make env-merge ENV=staging && make up-staging'

deploy-prod: push-prod
	@echo "→ Deploying to production..."
	@ssh -i ~/.ssh/tchalanet_prod tch@$$(hcloud server describe prod-app -o json | jq -r '.public_net.ipv4.ip') \
		'cd /opt/tchalanet-infra && make env-merge ENV=prod && make up-prod'

# Doppler: Download secrets
doppler-download:
	@if [ -z "$(DOPPLER_TOKEN)" ]; then \
		echo "❌ Erreur: DOPPLER_TOKEN non défini" >&2; \
		echo "   Usage: DOPPLER_TOKEN=dp.st.xxx make doppler-download ENV=staging" >&2; \
		exit 1; \
	fi
	@chmod +x ./scripts/doppler/download-doppler-secrets.sh || true
	@./scripts/doppler/download-doppler-secrets.sh $(ENV) "$(DOPPLER_TOKEN)"

doppler-download-staging:
	@$(MAKE) doppler-download ENV=staging

doppler-download-prod:
	@$(MAKE) doppler-download ENV=prod

up-all-local: ENV?=dev
up-all-local: env-merge render-traefik mkcert-local get-realm ensure-exec networks
	@echo "→ Démarrage stack (build local forcé) ENV=$(ENV)"
	@LOCAL_BUILD=1 scripts/utils/up-seq.sh $(ENV) --local-build


# ============================================================================
# P0 strict — Traefik + PostgreSQL + Keycloak
# ============================================================================

p0-up: env-merge render-traefik mkcert-local get-realm ensure-exec networks
	@echo "→ P0 up ENV=$(ENV)"
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up traefik $(ENV)
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up postgres $(ENV)
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up keycloak $(ENV)

p0-smoke:
	@echo "→ Smoke P0 ENV=$(ENV)"
	@curl -sf --max-time 5 https://traefik.localtest.me/ping \
	  || { echo "❌ Traefik KO"; exit 1; }
	@docker exec $$(docker ps --filter "name=.*postgres.*$(ENV)" --format "{{.Names}}" | head -1) \
	  pg_isready -U $${POSTGRES_USER:-postgres} \
	  || { echo "❌ PostgreSQL KO"; exit 1; }
	@curl -sf --max-time 10 https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration > /dev/null \
	  || { echo "❌ Keycloak KO"; exit 1; }
	@echo "✅ P0 smoke OK"

p0-down:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down keycloak $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down postgres $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down traefik $(ENV) || true

# ============================================================================
# P0+ — P0 + Redis
# ============================================================================

p0-plus-up: p0-up
	@echo "→ P0+ adding Redis ENV=$(ENV)"
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up redis $(ENV)

p0-plus-down:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down redis $(ENV) || true
	@$(MAKE) p0-down

# ============================================================================
# Modes locaux
# ============================================================================

# local-env : génère .env.merged
local-env: env-merge

# Ensure Keycloak image exists for local dev
ensure-keycloak-image:
	@if ! docker image inspect tchl/keycloak:local-dev >/dev/null 2>&1; then \
		echo "→ Keycloak image not found, building..."; \
		docker build --progress=plain -t tchl/keycloak:local-dev -f keycloak/Dockerfile keycloak/; \
		echo "✅ Keycloak image built"; \
	else \
		echo "✓ Keycloak image exists: tchl/keycloak:local-dev"; \
	fi

# local-ide : API dans l'IDE, infra P0 dans Docker
local-ide-up: ensure-keycloak-image p0-up

# local-ide avec Redis
local-ide-up-redis: ensure-keycloak-image p0-plus-up

local-ide-down: p0-down

# local-api : API dans Docker (P0+ + API)
local-api-up: p0-plus-up
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up api $(ENV)

local-api-down:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down api $(ENV) || true
	@$(MAKE) p0-plus-down

local-api-logs:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" logs api $(ENV)

local-api-smoke:
	@echo "→ Smoke API ENV=$(ENV)"
	@$(MAKE) p0-smoke
	@curl -sf --max-time 5 http://127.0.0.1:${REDIS_PORT:-6379} 2>/dev/null || \
	  docker exec $$(docker ps --filter "name=.*redis.*$(ENV)" --format "{{.Names}}" | head -1) \
	    redis-cli --no-auth-warning -a "$${REDIS_PASSWORD:-}" ping 2>/dev/null | grep -q PONG \
	  || { echo "❌ Redis KO"; exit 1; }
	@curl -sf --max-time 10 https://api.localtest.me/actuator/health \
	  || curl -sf --max-time 10 http://127.0.0.1:8081/actuator/health \
	  || { echo "❌ API /actuator/health KO"; exit 1; }
	@echo "✅ API smoke OK"

# local-product : server-v0 complet en local (API + Edge + Web)
local-product-up: local-api-up
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up edge $(ENV)
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" up web $(ENV)

local-product-down:
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down web $(ENV) || true
	@ENV=$(ENV) DOCKER_BIN="$(DOCKER_BIN)" "$(SVC)" down edge $(ENV) || true
	@$(MAKE) local-api-down

# ============================================================================
# Staging Hetzner disposable
# ============================================================================

staging-create:
	@chmod +x ./scripts/hcloud/staging-create.sh
	@./scripts/hcloud/staging-create.sh

staging-backup:
	@chmod +x ./scripts/remote/staging-backup.sh
	@./scripts/remote/staging-backup.sh

staging-destroy:
	@chmod +x ./scripts/hcloud/staging-destroy.sh
	@./scripts/hcloud/staging-destroy.sh

staging-restore-latest:
	@chmod +x ./scripts/remote/staging-restore-latest.sh
	@./scripts/remote/staging-restore-latest.sh

smoke-staging:
	@chmod +x ./scripts/utils/smoke-staging.sh
	@ENV=staging ./scripts/utils/smoke-staging.sh

.PHONY: p0-up p0-smoke p0-down p0-plus-up p0-plus-down
.PHONY: local-env local-ide-up local-ide-up-redis local-ide-down
.PHONY: local-api-up local-api-down local-api-logs local-api-smoke
.PHONY: local-product-up local-product-down
.PHONY: staging-create staging-backup staging-destroy staging-restore-latest smoke-staging
.PHONY: doppler-download doppler-download-staging doppler-download-prod
