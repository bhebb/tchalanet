# Tchalanet - Infrastructure (minimal)

Ce dossier contient les fichiers Docker Compose et les scripts d'aide pour démarrer une stack locale minimale (Postgres, Redis, Keycloak, API, Unleash, Meilisearch, Traefik).

Important — simplification

- Nous n'utilisons plus les `profiles` Docker Compose : les services peuvent être démarrés individuellement sans flags `--profile`.
- Le flux `post-install` automatique (bootstrap Unleash, génération automatique de tokens, unleash-edge) a été retiré de `scripts/utils/up-seq.sh` : ces tâches doivent être exécutées manuellement via les scripts dans `scripts/` ou via ton automation CI. Ceci simplifie le démarrage local et évite les erreurs liées aux dépendances de profil.

## Prérequis

- Docker Desktop (Compose V2)
- make
- (optionnel) Doppler CLI si tu veux injecter secrets

## Fichiers importants

- `compose/` : compose files (postgres, redis, keycloak, traefik, unleash, meili, api, ...)
- `envs/common/` et `envs/<env>/` : fichiers d'environnement
  - `compose.env` : variables utilisées au build (image tags, build args)
  - `.secrets` : secrets (non committés)
  - `.env.merged` : fichier généré (runtime) utilisé par les compose via `env_file`
- `Makefile` : commandes utiles (up, down, env-merge, etc.)
- `scripts/` : scripts d'aide (wait-keycloak, merge-env, up-seq, etc.)

## Principes d'utilisation

1. Frontend (Angular / Vite)

- Les appels HTTP depuis l'application frontend doivent utiliser des chemins relatifs, par exemple : `/api/health`.
- En développement, Vite proxye `/api` vers la target définie par `TCH_API_TARGET`.
  - développeur local : `TCH_API_TARGET=http://localhost:8080` (API lancée hors Docker)
  - via Traefik/Docker : `TCH_API_TARGET=https://api.localtest.me` (API dans Docker + Traefik)

2. Build vs Runtime

- Build-time : `envs/common/compose.env` et `envs/<env>/compose.env` (image tags, build args)
- Runtime : `envs/<env>/.env.merged` (généré par `env-merge`) et `envs/<env>/.secrets` (secrets privés)

## Démarrage local (très simple)

1. Fusion des envs (génère `envs/<ENV>/.env.merged`) :

```bash
cd tchalanet-infra
make env-merge ENV=dev
```

2. Démarrer la stack (build puis up — Keycloak import realm attendu) :

```bash
# full stack (build images si nécessaire puis up)
make up-all ENV=dev

# ou runtime minimal (utilise images existantes / tags définis dans compose.env)
make up-runtime ENV=dev
```

3. Arrêter la stack :

```bash
make down ENV=dev
```

## Notes - Unleash

- `compose/docker-compose-unleash.yml` fournit désormais uniquement le service `unleash` (server OSS) en mode "solo".
- Les opérations de bootstrap (création de tokens, seed de features) ne sont plus exécutées automatiquement par `up-seq.sh`.
  - Si tu veux générer des tokens automatiquement, utilise le script dédié `scripts/unleash-bootstrap.sh` en fournissant un PAT valide dans `envs/<env>/.secrets`.
  - Exemple (exécution manuelle) :

```bash
# après que Unleash soit démarré et accessible depuis le réseau compose
cd tchalanet-infra
# ex: UNLEASH_PERSONAL_TOKEN dans envs/dev/.secrets
./scripts/unleash-bootstrap.sh
```

## Exemples - Frontend dev

- Dev natif API hors Docker :

```bash
# start API via IntelliJ or CLI at http://localhost:8080
TCH_API_TARGET=http://localhost:8080 nx serve tchalanet-web
```

- API via Docker/Traefik (https://api.localtest.me) :

```bash
# Traefik et API doivent être up via make up-all
TCH_API_TARGET=https://api.localtest.me nx serve tchalanet-web
```

## Dépannage

- Vérifier l'état des services:

```bash
docker ps --format "table {{.Names}}	{{.Status}}	{{.Ports}}"
```

- Vérifier les logs d'un service :

```bash
# ex: Keycloak
docker logs -f tchl-keycloak-dev
```

- Healthchecks et import Keycloak : `up-seq.sh` attend que Keycloak soit prêt et que le realm soit disponible (via /q/health/ready et /.well-known/openid-configuration). Si l'import du realm prend du temps, augmente `start_period` dans `compose/docker-compose-keycloak.yml`.

## CI: Build & Publish (GitHub Actions)

A GitHub Actions workflow has been added (`.github/workflows/build-and-publish.yml`) to build and push the API and Keycloak images to GHCR (ghcr.io/tchalanet). The workflow:

- builds `tchalanet-server` image and tags it `ghcr.io/tchalanet/api:<IMAGE_TAG>`
- builds the custom Keycloak image and tags it `ghcr.io/tchalanet/keycloak:<IMAGE_TAG>`

How to use (manual dispatch):

1. In GitHub UI → Actions → "Build and Publish Images" → Run workflow → provide `image-tag` optionally.
2. Or push to `main` branch; a tag is auto-derived `stg-<shortsha>`.

## Deploying the pushed image

- After CI pushes an image tag, update `envs/<env>/compose.env` with `IMAGE_TAG=<tag>` then on the target server:

```bash
# on server or in CI remote deploy step
docker compose --env-file envs/staging/compose.env -f compose/docker-compose-project.yml -f compose/docker-compose-api.yml pull
docker compose --env-file envs/staging/compose.env -f compose/docker-compose-project.yml -f compose/docker-compose-api.yml up -d
```

## Deploy to Staging (Hetzner)

This project includes a GitHub Actions workflow `.github/workflows/deploy-stg.yml` that synchronises `tchalanet-infra/` to a target Hetzner server and runs `docker compose pull && up -d` there.

Prerequisites on the target server

1. A Linux server (Debian/Ubuntu recommended) with Docker and Docker Compose installed. Example quick install:

```bash
# on the Hetzner server
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
sudo apt-get update && sudo apt-get install -y docker-compose-plugin rsync
```

2. Create the directories and ensure permissions:

```bash
sudo mkdir -p /opt/tchalanet-infra
sudo chown $USER:$USER /opt/tchalanet-infra
```

3. Ensure the `edge` and `back` docker networks exist (the `deploy` workflow may create them in runtime, but you can pre-create):

```bash
docker network create edge-staging || true
docker network create back-staging || true
```

Required GitHub secrets (for the `deploy-stg` workflow)

- `STG_SSH_PRIVATE_KEY` : private SSH key (PEM) for the deploy user on the server.
- `STG_HOST` : IP or DNS name of the Hetzner server.
- `STG_USER` : username (eg `ubuntu` or `deploy`).
- `GHCR_PAT` (optional) : Personal Access Token to pull images from GHCR if private.
- `STG_SSH_PORT` (optional) : SSH port (defaults to 22).

Workflow behaviour

- The workflow `deploy-stg`:
  1. Checks-out the repo
  2. Uses `rsync` to copy `tchalanet-infra/` to the remote `REMOTE_DIR` (`/opt/tchalanet-infra`)
  3. SSH to remote and runs `docker compose --env-file envs/staging/.env.merged ... pull && up -d`
  4. Optionally logs in to GHCR using `GHCR_PAT` if provided.

Manual tasks you must perform before deploying

- Prepare `envs/staging/.env.merged` and `envs/staging/.secrets` locally in the repo (do not commit `.secrets`). The workflow will use the repo copy to deploy. The `.env.merged` should contain values coming from `envs/common/compose.env` and `envs/staging/compose.env` merged as described earlier.

- Ensure `envs/staging/compose.env` contains the correct `IMAGE_TAG` values (or CI sets it during the build workflow).

- If you use custom Keycloak or other provider images, ensure they're available on the server (pushed to GHCR or another registry) and that `envs/staging/.env.merged` points to correct `IMAGE` variables.

Testing the deploy locally (dry-run)

1. From your machine, test the rsync step:

```bash
rsync -avz --delete --exclude='.git' tchalanet-infra/ ${STG_USER}@${STG_HOST}:/opt/tchalanet-infra/
```

2. Then SSH and run the compose commands manually to observe logs:

```bash
ssh ${STG_USER}@${STG_HOST}
cd /opt/tchalanet-infra
docker compose --env-file envs/staging/.env.merged -f compose/docker-compose-project.yml -f compose/docker-compose-postgres.yml -f compose/docker-compose-redis.yml -f compose/docker-compose-keycloak.yml -f compose/docker-compose-unleash.yml -f compose/docker-compose-api.yml -f compose/docker-compose-traefik.yml pull
docker compose --env-file envs/staging/.env.merged -f compose/docker-compose-project.yml -f compose/docker-compose-postgres.yml -f compose/docker-compose-redis.yml -f compose/docker-compose-keycloak.yml -f compose/docker-compose-unleash.yml -f compose/docker-compose-api.yml -f compose/docker-compose-traefik.yml up -d
```

Rollback and failure handling

- To rollback to a previous image tag, update `envs/staging/compose.env` with the previous `IMAGE_TAG` and run the deploy workflow again (or run the `docker compose pull` + `up -d` sequence on the server).

Security considerations

- Keep `envs/staging/.secrets` out of Git. Use a secret store (Doppler, Vault or GitHub secrets) in CI to inject sensitive vars into the server if needed.
- Use a deploy user with limited privileges on the server and guard SSH key access.

## Local cleanup commands

If you want to clean the local Docker environment before a fresh run:

```bash
# stop and remove containers for this project
docker compose -f tchalanet-infra/compose/docker-compose-project.yml down --remove-orphans

# remove unused images, volumes and networks (be careful; affects host)
docker system prune -a --volumes
```

# Tchalanet Infrastructure

Ce dossier contient les fichiers Docker Compose, les scripts et la documentation pour l'infrastructure (local/staging/prod).

Documents essentiels:

- docs/OPERATIONS.md — guide opérationnel consolidé (local, staging, prod)
- docs/HETZNER.md — création/recréation d'une instance Hetzner et (re)déploiement
- docs/DOPPLER.md — gestion des secrets (service tokens, intégration CI)

Scripts clés:

- scripts/hcloud/\* — provisioning Hetzner (réseau, firewall, serveur, cloud-init)
- scripts/remote/\* — bootstrap machine distante, push infra, installation Docker
- scripts/utils/\* — merge env, up séquentiel, rendu traefik, réseaux

Pour un démarrage rapide local, voir docs/QUICKSTART.md. Pour une procédure complète de déploiement, voir docs/OPERATIONS.md. Pour (re)créer un serveur Hetzner en staging, suivre docs/HETZNER.md.
