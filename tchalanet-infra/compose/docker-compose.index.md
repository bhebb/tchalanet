# Index des Docker Compose (infra)

Vue d'ensemble des services, fichiers Compose, réseaux, ports exposés et dépendances.

| Service          | Compose file                   | Réseaux        | Ports exposés                           | Dépendances               |
| ---------------- | ------------------------------ | -------------- | --------------------------------------- | ------------------------- |
| Traefik          | `docker-compose-traefik.yml`   | `edge`         | 80/443 (host)                           | -                         |
| Postgres         | `docker-compose-postgres.yml`  | `back`         | 5432 (host, dev-local)                  | -                         |
| Redis            | `docker-compose-redis.yml`     | `back`         | 6379 (host, dev-local)                  | -                         |
| Keycloak         | `docker-compose-keycloak.yml`  | `edge`, `back` | 8080 (container), 8082 (host, dev-local)| Postgres                  |
| API              | `docker-compose-api.yml`       | `edge`, `back` | 8080 (container, via Traefik)           | Keycloak, Redis, Postgres |
| Edge service     | `docker-compose-edge-service.yml`      | `back`         | 3000 (container, interne)               | -                         |
| Web              | `docker-compose-web.yml`       | `edge`         | 80 (container, via Traefik)             | -                         |
| OTel Collector   | `docker-compose-otel.yml`      | `back`         | 4317/4318 (host, dev-local)             | Jaeger                    |
| Jaeger           | `docker-compose-otel.yml`      | `back`         | 16686 UI (host, dev-local)              | -                         |

## Rappels utiles

- Fichiers Compose: chaque service a son propre fichier dans `compose/`.
- Variables:
  - Build-time (interpolation): `envs/<env>/compose.env` (ex: `ENV`, `DOCKER_PREFIX`, réseaux).
  - Runtime (conteneurs): `envs/common/*.env` + `envs/<env>/*.env` + `envs/<env>/.secrets` via `env_file:`.
- Réseaux:
  - `edge-<ENV>` (exposition publique via Traefik)
  - `back-<ENV>` (réseau interne des services)

## Démarrage rapide

```bash
# Stack complète
make up-all ENV=dev

# Edge service local (build from source)
make rebuild-edge ENV=dev   # build + start
make up-edge ENV=dev        # start (image déjà buildée)
make down-edge ENV=dev      # stop

# Stack complète avec edge + web
make local-product-up ENV=dev
```

## Stack observabilité (local)

```bash
# Démarrer OTel Collector + Jaeger (à ajouter à n'importe quel make target)
docker compose \
  -f compose/docker-compose-otel.yml \
  -f compose/docker-compose-dev-local.yml \
  --env-file envs/dev/compose.env up -d otel-collector jaeger

# Jaeger UI → http://localhost:16686
# OTLP HTTP (IDE) → http://localhost:4318/v1/traces
# OTLP gRPC (IDE) → localhost:4317
```

Config collector : `compose/otel/otel-collector-config.yaml`

## Overrides locaux

- `docker-compose-dev-local.yml` — expose des ports host (5432, 6379, 8082, etc.) pour dev local. Inclus automatiquement par les targets `local-*` du Makefile.
- `docker-compose.local-build.yml` — override `build:` pour construire les images localement (API, Keycloak). Dev uniquement.
