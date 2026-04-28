## 1. Correctifs infra (déjà appliqués)

- [x] 1.1 `tchalanet-infra/scripts/local/setup-api-env.sh` — corriger `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (app_user/tchalanet_db)
- [x] 1.2 `tchalanet-infra/scripts/local/setup-api-env.sh` — corriger issuer-uri Keycloak (`https://auth.localtest.me/realms/tchalanet`), `KEYCLOAK_REALM=tchalanet`, port 8083
- [x] 1.3 `tchalanet-infra/scripts/local/setup-api-env.sh` — corriger `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (jamais `update`)
- [x] 1.4 `tchalanet-infra/traefik/traefik.yml` — `ping.entryPoint: web` (au lieu de `websecure`)

## 2. Fiabilité Traefik healthcheck

- [ ] 2.1 `tchalanet-infra/compose/docker-compose-traefik.yml` — passer `start_period` de `5s` à `15s`

## 3. Document QUICK-START.md

- [ ] 3.1 Créer `tchalanet-infra/QUICK-START.md` — section Prérequis (mkcert, Docker Desktop, make, GHCR ou rebuild-keycloak)
- [ ] 3.2 Remplir section Démarrage quotidien (`make env-merge` → `make mkcert-local` → `make networks` → `make up-all`)
- [ ] 3.3 Remplir section Option B — API en IDE local (commandes `docker compose` minimales + `./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide`)
- [ ] 3.4 Remplir section Vérification post-démarrage (commandes `curl` pour Keycloak, API health, token + appel authentifié)
- [ ] 3.5 Remplir checklist DoD (toutes les cases de la spec)
- [ ] 3.6 Remplir section Dépannage (logs Docker, `make ps`, `make logs-keycloak`)

## 4. Validation

- [ ] 4.1 `make env-merge ENV=dev` → `envs/dev/.env.merged` généré sans erreur
- [ ] 4.2 `make up-all ENV=dev` → tous les services `(healthy)` dans `docker ps`
- [ ] 4.3 `curl -s http://localhost:8083/api/v1/actuator/health | jq .status` → `"UP"`
- [ ] 4.4 Token Keycloak obtenu avec `super_admin`/`changeme` + appel `GET /tenant/draws` → HTTP 200
- [ ] 4.5 `./scripts/local/setup-api-env.sh dev` → `.env` sans valeurs incorrectes (vérifier DB, realm, ddl-auto)
