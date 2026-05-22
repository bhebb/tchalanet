# Tchalanet Infra Quick Start

> Scope: local development stack (`ENV=dev`).

This guide is the canonical path for starting the local infrastructure on a new
machine.

## Prerequisites

- Docker Desktop with Docker Compose V2
- `make`
- `mkcert`
- Access to GHCR for the custom Keycloak image, or a local rebuild

Install local certificates tooling:

```bash
brew install mkcert
mkcert -install
```

If you cannot pull the custom Keycloak image from GHCR, build it locally:

```bash
cd tchalanet-infra
make rebuild-keycloak ENV=dev
```

## Daily Start

Run these commands in order:

```bash
cd tchalanet-infra
make env-merge ENV=dev
make mkcert-local ENV=dev
make networks ENV=dev
make up-all ENV=dev
```

Check service state:

```bash
make ps ENV=dev
```

Postgres, Redis, Keycloak, Traefik, and Unleash should be healthy.

## Option B: API In Local IDE

Use this when Spring Boot runs from IntelliJ or the Maven wrapper, while infra
services stay in Docker.

```bash
cd tchalanet-infra
make env-merge ENV=dev
make mkcert-local ENV=dev
make networks ENV=dev
docker compose \
  --env-file envs/dev/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-traefik.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-keycloak.yml \
  up -d traefik postgres redis keycloak
```

Generate the API `.env`, then start Spring Boot:

```bash
cd tchalanet-infra
./scripts/local/setup-api-env.sh dev

cd ../tchalanet-server
set -a; source .env; set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide
```

## Post-Start Checks

Keycloak issuer:

```bash
curl -s https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq .issuer
```

Expected:

```text
"https://auth.localtest.me/realms/tchalanet"
```

API health:

```bash
curl -s http://localhost:8083/api/v1/actuator/health | jq .status
```

Expected:

```text
"UP"
```

Token and authenticated API call:

```bash
TOKEN="$(curl -sk -X POST https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=tchalanet-web' \
  -d 'username=super_admin' \
  -d 'password=Changeme1!' | jq -r .access_token)"

curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/v1/tenant/draws
```

The token must be a JWT string and `GET /tenant/draws` must return HTTP 200.

## Definition Of Done

- [ ] `make env-merge ENV=dev` creates `envs/dev/.env.merged`
- [ ] `make mkcert-local ENV=dev` creates local certificates
- [ ] `make networks ENV=dev` creates `edge-dev` and `back-dev`
- [ ] `make up-all ENV=dev` starts all services
- [ ] `make ps ENV=dev` shows services as healthy
- [ ] Keycloak issuer is `https://auth.localtest.me/realms/tchalanet`
- [ ] API health returns `"UP"`
- [ ] `super_admin` / `Changeme1!` can obtain a token
- [ ] `GET /tenant/draws` returns HTTP 200 with that token
- [ ] `./scripts/local/setup-api-env.sh dev` generates correct DB, realm, port, and `ddl-auto=validate`

## Troubleshooting

Service list:

```bash
make ps ENV=dev
```

Keycloak logs:

```bash
make logs-keycloak ENV=dev
```

Traefik logs:

```bash
make logs-traefik ENV=dev
```

Postgres logs:

```bash
make logs-postgres ENV=dev
```

If certificates are stale:

```bash
make mkcert-recreate ENV=dev
```

If Keycloak cannot start because the custom provider image is missing:

```bash
make rebuild-keycloak ENV=dev
```
