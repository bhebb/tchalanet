# OPERATIONS — Quickstart & Deploy (local / staging / prod)

But: document opérationnel concentré pour un nouvel opérateur. Regroupe Quickstart, Déploiement et Doppler en un seul fichier.

Rappels rapides

- Repo root: `tchalanet-infra/` contient les compose files, envs, scripts.
- Secrets: gérés via Doppler (recommended). Ne jamais committer `envs/*/.secrets`.
- Scripts utiles (dans `scripts/` et `scripts/remote/`) :
  - `./scripts/utils/merge-env.sh` (ou `make env-merge`) → génère `envs/<env>/.env.merged`
  - `./scripts/remote/push-infra-bkup.sh <HOST> <ENV> [--no-bootstrap]` → rsync infra → remote (/opt/tchalanet-infra). Exclut `.secrets`.
  - `./scripts/remote/01-bootstrap.sh <ENV>` → installe Docker (si absent), crée réseaux `edge-<env>` & `back-<env>` et prépare Traefik
  - `./scripts/remote/install-docker.sh` → installe Docker & test
  - `./scripts/utils/setup-networks.sh <ENV>` → crée réseaux si absent (local / remote usage)

Voir aussi: `docs/HETZNER.md` pour la (re)création d'une VM Hetzner (staging/prod).

1. Quickstart local (dev)

Objectif : monter la stack local (Postgres, Redis, Keycloak, API, Unleash, Traefik) pour le développement.

Commands (copy/paste):

```bash
# choisir le répertoire infra
cd tchalanet-infra
# générer .env.merged pour dev
make env-merge ENV=dev

# monter toute la stack (build si besoin)
make up-all ENV=dev

# ou démarrer runtime uniquement
make up-runtime ENV=dev

# arrêter
make down ENV=dev
```

Front local (vite) avec proxy API

```bash
# Exécuter le frontend en le pointant vers l'API Docker/Traefik
TCH_API_TARGET=https://api.localtest.me nx serve tchalanet-web
```

2. Staging (Hetzner) — opérateur (manuel)

Préparer les envs locaux :

```bash
cd tchalanet-infra
make env-merge ENV=staging
# éditer envs/staging/compose.env (IMAGE_TAG, DOCKER_NETWORK_*) si besoin
# préparer localement envs/staging/.secrets si tu veux tester (mais prefer Doppler)
```

Pousser infra + bootstrap remote :

```bash
# copie le dossier tchalanet-infra -> /opt/tchalanet-infra sur la VM
./scripts/remote/push-infra-bkup.sh <STG_HOST> staging
# ou push sans bootstrap
./scripts/remote/push-infra-bkup.sh <STG_HOST> staging --no-bootstrap
```

Configurer secrets via Doppler (sur remote) — exemple manual :

```bash
# sur la VM (ssh tch@<STG_HOST>)
export DOPPLER_TOKEN="<DOPPLER_TOKEN_STG>"
docker run --rm -e DOPPLER_TOKEN="$DOPPLER_TOKEN" -v "$PWD":/work -w /work dopplerhq/cli:latest \
  sh -lc 'doppler secrets download --format env --project tchalanet --config staging > envs/staging/.secrets && chmod 600 envs/staging/.secrets'
```

Générer .env.merged, puis démarrer la stack (pull + up) sur la VM :

```bash
cd /opt/tchalanet-infra
make env-merge ENV=staging

# pull
docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose-unleash.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-traefik.yml pull

# up
docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose-unleash.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-traefik.yml up -d
```

Vérifications rapides (sur la VM) :

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker logs -f tchl-api-staging
curl -fsS http://api:8080/actuator/health
curl -fsS http://unleash:4242/health
```

3. Production (procédure courte)

- Même flux que staging mais :
  - Utiliser `envs/prod/compose.env` et Doppler `production` config
  - Restreindre firewall (`hcloud/02-create-firewall.sh --admin-ips "<your_ip>"`)
  - Déployer via workflow CI (dispatch) ou manuellement via SSH

Commands (CI flow recommandé):

```bash
# build and push images (CI) -> build-and-publish.yml
# then run infra.yml dispatch (manual) or via gh CLI
# gh workflow run infra.yml -f env=prod -f mode=update -f services="api"
```

4. Déploiement régulier de l'API (fast path)

- Build/push (GitHub Actions) sur `develop` (stg) et `main` (prod)
- Pull & restart API on remote:
  - Manual: SSH and run `docker compose pull` + `docker compose up -d api`
  - Or via workflow `deploy-stg.yml` / `deploy-prod.yml`

5. Doppler — points pratiques

- Stocker secrets dans Doppler (`tchalanet` project) with configs `staging` and `production`.
- Create a Service Token (secrets:download) for CI and store it in GitHub Secrets as `DOPPLER_TOKEN_STG` / `DOPPLER_TOKEN_PROD`.
- Sur les serveurs, utilisez `doppler secrets download` pour produire `envs/<env>/.secrets`.

6. Makefile usage (raccourcis utiles)

- `make env-merge ENV=...` → génère `.env.merged`
- `make up-all ENV=...` → build/pull & up full stack
- `make up-runtime ENV=...` → start using existing images
- `make down ENV=...` → stop

7. Tips & troubleshooting

- Si Keycloak est `unhealthy` mais répond, vérifier la commande de healthcheck (certaines images n'ont pas `curl`/`wget`). Adapter le healthcheck dans compose.
- Si Traefik retourne 502, vérifier l'attachement réseau (`edge-<env>`) et la cible du service (port interne correct).
- Utiliser `docker inspect --format '{{json .State}}' <container>` pour obtenir l'état précis et les derniers logs de health.

---

Contact rapide: pour toute erreur critique, récupère les logs et partage le snippet (`docker logs -f <container>`). Pour les déploiements sensibles (prod), privilégier une fenêtre MEP et ne pas automatiser le pipeline prod sans revue humaine.
