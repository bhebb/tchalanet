SHELL := /bin/bash
INFRA_DIR := tchalanet-infra
ENV ?= dev

.PHONY: help up up-all down down-all up-api print-env doppler-seed
.PHONY: local-ide-up local-ide-up-redis local-ide-down
.PHONY: local-api-up local-api-down local-product-up local-product-down
.PHONY: p0-up p0-down p0-plus-up p0-plus-down compose-build

help:
	@echo "Usage: make <target> ENV=dev|staging|prod"
	@echo ""
	@echo "Infrastructure:"
	@echo "  up, up-all, down, down-all"
	@echo ""
	@echo "Local Development Modes:"
	@echo "  local-ide-up         - API dans IDE, infra P0 (Traefik+Postgres+Keycloak) dans Docker"
	@echo "  local-ide-up-redis   - API dans IDE, infra P0+Redis dans Docker"
	@echo "  local-ide-down       - Arrêter infra P0"
	@echo "  local-api-up         - API dans Docker, infra P0+Redis"
	@echo "  local-api-down       - Arrêter API + infra"
	@echo "  local-product-up     - Stack complète (API+Edge+Web)"
	@echo "  local-product-down   - Arrêter stack complète"
	@echo ""
	@echo "Infrastructure P0:"
	@echo "  p0-up                - Traefik + Postgres + Keycloak"
	@echo "  p0-down              - Arrêter P0"
	@echo "  p0-plus-up           - P0 + Redis"
	@echo "  p0-plus-down         - Arrêter P0+"
	@echo ""
	@echo "Other:"
	@echo "  up-api, doppler-seed, print-env"

# Infrastructure de base
up:         ; @$(MAKE) -C $(INFRA_DIR) up ENV=$(ENV)
up-all:     ; @$(MAKE) -C $(INFRA_DIR) up-all ENV=$(ENV)
down:       ; @$(MAKE) -C $(INFRA_DIR) down ENV=$(ENV)
down-all:   ; @$(MAKE) -C $(INFRA_DIR) down-all ENV=$(ENV)
up-api:     ; @$(MAKE) -C $(INFRA_DIR) up-api ENV=$(ENV)

# P0 modes
p0-up:      ; @$(MAKE) -C $(INFRA_DIR) p0-up ENV=$(ENV)
p0-down:    ; @$(MAKE) -C $(INFRA_DIR) p0-down ENV=$(ENV)
p0-plus-up: ; @$(MAKE) -C $(INFRA_DIR) p0-plus-up ENV=$(ENV)
p0-plus-down: ; @$(MAKE) -C $(INFRA_DIR) p0-plus-down ENV=$(ENV)

# Local development modes
local-ide-up:       ; @$(MAKE) -C $(INFRA_DIR) local-ide-up ENV=$(ENV)
local-ide-up-redis: ; @$(MAKE) -C $(INFRA_DIR) local-ide-up-redis ENV=$(ENV)
local-ide-down:     ; @$(MAKE) -C $(INFRA_DIR) local-ide-down ENV=$(ENV)

local-api-up:       ; @$(MAKE) -C $(INFRA_DIR) local-api-up ENV=$(ENV)
local-api-down:     ; @$(MAKE) -C $(INFRA_DIR) local-api-down ENV=$(ENV)

local-product-up:   ; @$(MAKE) -C $(INFRA_DIR) local-product-up ENV=$(ENV)
local-product-down: ; @$(MAKE) -C $(INFRA_DIR) local-product-down ENV=$(ENV)

# Keycloak development
keycloak-build:
	@echo "→ Building Keycloak image with custom providers..."
	@cd $(INFRA_DIR) && docker build --progress=plain -t tchl/keycloak:local-dev -f keycloak/Dockerfile keycloak/
	@echo "✅ Keycloak image built: tchl/keycloak:local-dev"

keycloak-rebuild:
	@echo "→ Rebuilding and restarting Keycloak..."
	@$(MAKE) keycloak-build
	@cd $(INFRA_DIR) && docker compose -f compose/docker-compose-postgres.yml -f compose/docker-compose-keycloak.yml --env-file envs/$(ENV)/compose.env --env-file envs/common/compose.env up -d --force-recreate keycloak
	@echo "✅ Keycloak restarted"

keycloak-logs:
	@docker logs -f tchl-keycloak-$(ENV)

keycloak-restart:
	@echo "→ Restarting Keycloak..."
	@cd $(INFRA_DIR) && docker compose -f compose/docker-compose-postgres.yml -f compose/docker-compose-keycloak.yml --env-file envs/$(ENV)/compose.env --env-file envs/common/compose.env restart keycloak
	@echo "✅ Keycloak restarted"

# Utilities
doppler-seed: ; @$(MAKE) -C $(INFRA_DIR) doppler-seed ENV=$(ENV)
print-env:    ; @$(MAKE) -C $(INFRA_DIR) print-env ENV=$(ENV)

