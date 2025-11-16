# Docker Compose Files

Fichiers de configuration Docker Compose pour l'infrastructure Tchalanet.

## 📁 Structure

Chaque service a son propre fichier compose pour permettre un démarrage granulaire :

```
compose/
├── docker-compose-postgres.yml      # PostgreSQL 18
├── docker-compose-redis.yml         # Redis 8.2
├── docker-compose-meilisearch.yml   # Meilisearch
├── docker-compose-keycloak.yml      # Keycloak 26.4
├── docker-compose-unleash.yml       # Unleash (feature flags)
├── docker-compose-unleash-seeds.yml # Unleash seeds (one-shot)
├── docker-compose-umami.yml         # Umami (analytics)
├── docker-compose-traefik.yml       # Traefik (reverse proxy)
└── docker-compose-api.yml           # API Tchalanet
```

## 🚀 Usage

### Fichier `.env` dans compose/

**Important** : Le fichier `compose/.env` est **auto-généré** par les scripts de déploiement et ne doit **JAMAIS être édité manuellement**.

#### Pourquoi ce fichier existe ?

Docker Compose charge automatiquement un fichier `.env` dans le répertoire où il est exécuté **AVANT** de résoudre les références d'images. Ce fichier contient **uniquement** les variables nécessaires à la résolution d'images :

```dotenv
# compose/.env (auto-généré)
ENV=staging
API_IMAGE_BASE=ghcr.io/bhebb/tchalanet-api
KEYCLOAK_IMAGE=ghcr.io/bhebb/tchalanet-keycloak:stg-20251114
IMAGE_TAG=stg-20251114
DOCKER_PREFIX=tchl
```

#### Différence avec les autres fichiers env

| Fichier                  | Usage                              | Timing               | Contenu                       |
| ------------------------ | ---------------------------------- | -------------------- | ----------------------------- |
| `compose/.env`           | Résolution d'images Docker Compose | **Avant** résolution | Variables d'images uniquement |
| `envs/<env>/compose.env` | Variables build-time               | Build / Compose      | Versions, images, build args  |
| `envs/<env>/.env`        | Variables runtime (non-secrets)    | Container runtime    | Config app (hosts, etc.)      |
| `envs/<env>/.secrets`    | Secrets runtime                    | Container runtime    | Passwords, tokens, keys       |
| `envs/<env>/.env.merged` | Fusion de `.env` + secrets runtime | Container runtime    | Toutes vars runtime           |

#### Ordre de chargement Docker Compose

1. ✅ `compose/.env` (auto, pour résolution d'images)
2. ✅ Variables d'environnement shell
3. ✅ `--env-file` (si spécifié en CLI)
4. ✅ `env_file:` dans le service (injecté dans le conteneur)

**Note** : Les variables dans `env_file:` du service ne sont **PAS** utilisées pour la résolution d'images, elles sont injectées dans le conteneur au runtime.

### Build-time vs Runtime (reminder)

- Build-time variables (interpolated into compose/build args) are read from `envs/common/compose.env` then `envs/<ENV>/compose.env` and passed to `docker compose --env-file` when building images.
- Runtime variables (injected into containers) come from `envs/<ENV>/.env.merged` and `envs/<ENV>/.secrets` via `env_file:` in the service definitions.

### Local build override

`compose/docker-compose.local-build.yml` contains `build:` blocks that let you build images from local Dockerfiles (source trees). This file is meant for local development only and should NOT be used by CI/staging/prod workflows. In CI we build in the pipeline and publish images to GHCR and update `envs/<ENV>/compose.env` accordingly.

### Depuis la racine (avec Makefile)

```bash
# Stack complète
make up-all ENV=staging

# Service individuel
make up-postgres ENV=staging
make up-keycloak ENV=staging

# Logs
make logs-postgres ENV=staging

# Arrêt
make down-all ENV=staging
```

### Directement avec docker compose

```bash
# Service unique
docker compose -f compose/docker-compose-postgres.yml up -d

# Multiple services
docker compose \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-keycloak.yml \
  up -d

# Avec fichier env
docker compose --env-file envs/staging/compose.env \
  -f compose/docker-compose-postgres.yml up -d
```

## 🔧 Conventions

### Chemins relatifs

Les chemins dans les fichiers compose utilisent `../` car ils sont dans le sous-dossier `compose/` :

```yaml
volumes:
  - ../scripts/docker/postgres-init.sh:/docker-entrypoint-initdb.d/00-init.sh:ro
  - ../postgres/postgresql.conf:/etc/postgresql/postgresql.conf:ro
  - ../keycloak/realms:/opt/keycloak/data/import:ro
env_file:
  - ../envs/common/.env
  - ../envs/${ENV}/.env
```

### Nommage

- Fichier : `docker-compose-<service>.yml`
- Container : `${DOCKER_PREFIX:-tchl}-<service>-${ENV}`
- Network : `back-${ENV}` ou `edge-${ENV}`
- Volume : `<service>-data-${ENV}`

### Variables d'environnement

**Build-time** (compose interpolation) :

- Fichier : `envs/<env>/compose.env`
- Variables : `ENV`, `DOCKER_PREFIX`, network names, etc.

**Runtime** (container env) :

- Fichiers : `envs/common/*.env` + `envs/<env>/*.env` + `envs/<env>/.secrets`
- Chargés via `env_file:`

## 📝 Ordre de démarrage (dépendances)

1. **Postgres** + **Redis** + **Meilisearch** (bases de données)
2. **Unleash** (dépend de Postgres)
3. **Keycloak** (dépend de Postgres)
4. **Traefik** (reverse proxy)
5. **Umami** (dépend de Postgres)
6. **API** (dépend de Postgres, Keycloak, Redis, etc.)

L'ordre est géré par `depends_on` dans les fichiers compose.

## 🔍 Debugging

### Vérifier la config rendue

```bash
# Config finale d'un service
docker compose -f compose/docker-compose-postgres.yml config

# Avec env
docker compose --env-file envs/staging/compose.env \
  -f compose/docker-compose-postgres.yml config
```

### Valider syntaxe

```bash
# Tous les fichiers
for f in compose/*.yml; do
  echo "Validating $f..."
  docker compose -f "$f" config > /dev/null && echo "  ✓ OK"
done
```

## 📚 Voir aussi

- [Makefile](../Makefile) - Commandes Make
- [scripts/local/](../scripts/local/) - Scripts dev local
- [envs/](../envs/) - Configuration environnements
- [postgres/](../postgres/) - Config PostgreSQL
- [keycloak/](../keycloak/) - Config Keycloak
