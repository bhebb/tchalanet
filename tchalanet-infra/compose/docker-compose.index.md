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

## Overrides locaux

- `docker-compose-dev-local.yml` — expose des ports host (5432, 6379, 8082, etc.) pour dev local. Inclus automatiquement par les targets `local-*` du Makefile.
- `docker-compose.local-build.yml` — override `build:` pour construire les images localement (API, Keycloak). Dev uniquement.
