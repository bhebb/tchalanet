# Docker Compose Files

Fichiers de configuration Docker Compose pour l'infrastructure Tchalanet.

## 📁 Structure

```
compose/
├── docker-compose-project.yml           # Base : réseaux externes, labels, defaults
├── docker-compose.override.yml          # Ports locaux exposés (dev uniquement)
│
│   # P0 strict
├── docker-compose-traefik.yml           # Traefik (reverse proxy) — réseau edge
├── docker-compose-postgres.yml          # PostgreSQL 18 — réseau back
├── docker-compose-keycloak.yml          # Keycloak 26.4 — réseaux edge + back
│
│   # P0+
├── docker-compose-redis.yml             # Redis 8 — réseau back
│
│   # server-v0
├── docker-compose-api.yml               # API Spring Boot — réseaux edge + back
├── docker-compose-edge.yml              # Unleash Edge proxy — réseaux edge + back
├── docker-compose-edge-service.yml              # Tchalanet edge-service (build local) — réseau back
└── docker-compose-web.yml               # Web Angular — réseau edge
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

### Réseaux Docker — convention

`run-compose.sh` inclut **toujours** `docker-compose-project.yml` en premier. Ce fichier est l'autorité pour les déclarations réseau :

```yaml
# docker-compose-project.yml
networks:
  edge:
    name: ${DOCKER_NETWORK_EDGE:-edge-dev}
    external: true
  back:
    name: ${DOCKER_NETWORK_BACK:-back-dev}
    external: true
```

**Règle** : les fichiers de service ne redéclarent pas les réseaux au niveau racine. Ils référencent uniquement les réseaux au niveau service :

```yaml
# Correct — référence sans redéclaration
services:
  my-service:
    networks:
      back: {}
      edge:
        aliases: ["my-service"]

# Incorrect — redéclaration redondante (éviter)
networks:
  back:
    name: back-${ENV}
    external: true
```

Affectation réseau par service :

| Service    | edge | back |
|------------|------|------|
| Traefik      | ✅   |      |
| Web          | ✅   |      |
| API          | ✅   | ✅   |
| edge-service |      | ✅   |
| Keycloak     | ✅   | ✅   |
| PostgreSQL   |      | ✅   |
| Redis        |      | ✅   |

### IMAGE_TAG

`IMAGE_TAG` est **obligatoire** pour les services applicatifs (api, web). Pas de fallback `:latest` ni tag flottant. Définir dans `envs/common/compose.env` ou `envs/<env>/compose.env`.

`edge-service` utilise un build local (`TCH_EDGE_TAG=local`) — pas d'`IMAGE_TAG` requis.

### Variables d'environnement

**Build-time** (compose interpolation) :

- Fichier : `envs/<env>/compose.env`
- Variables : `ENV`, `DOCKER_PREFIX`, network names, etc.

**Runtime** (container env) :

- Fichiers : `envs/common/*.env` + `envs/<env>/*.env` + `envs/<env>/.secrets`
- Chargés via `env_file:`

## 📝 Ordre de démarrage (dépendances)

1. **Postgres** + **Redis** (bases de données)
2. **Keycloak** (dépend de Postgres)
3. **Traefik** (reverse proxy)
4. **API** (dépend de Postgres, Keycloak, Redis)

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
