ENV ?= dev
COMPOSE := docker compose --env-file envs/$(ENV)/.env -f docker-compose.yml

up:
	$(COMPOSE) -f docker-compose-$(ENV).yml up -d

down:
	$(COMPOSE) -f docker-compose-$(ENV).yml down

reset:
	$(COMPOSE) -f docker-compose-$(ENV).yml down -v
	$(MAKE) up ENV=$(ENV)

logs:
	$(COMPOSE) -f docker-compose-$(ENV).yml logs -f

render:
	./scripts/kc-render.sh $(ENV)

.PHONY: realm-reset realm-import kc-http-dev

realm-reset: render
	$(COMPOSE) exec -T keycloak /opt/keycloak/bin/kcadm.sh \
	  config credentials --server http://localhost:8080 --realm master \
	  --user "admin" --password "admin"
	$(COMPOSE) exec -T keycloak /opt/keycloak/bin/kcadm.sh delete realms/tchalanet || true
	$(COMPOSE) exec -T keycloak /opt/keycloak/bin/kcadm.sh create realms -f /opt/keycloak/data/import/tchalanet.json

kc-http-dev:
	$(COMPOSE) exec -T keycloak /opt/keycloak/bin/kcadm.sh \
	  config credentials --server http://localhost:8080 --realm master \
	  --user "admin" --password "admin"
	$(COMPOSE) exec -T keycloak /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE


