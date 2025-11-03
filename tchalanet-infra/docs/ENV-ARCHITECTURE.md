# 📋 Architecture des variables d'environnement - Tchalanet

**Date:** 14 novembre 2025  
**Version:** Finale optimisée

---

## 🎯 Vue d'ensemble

L'architecture sépare clairement **3 types de variables** :

1. **Build-time / Interpolation** (`compose.env`) → Utilisées par Docker Compose pour parser le YAML
2. **Runtime** (`.env`) → Utilisées par les applications dans les conteneurs
3. **Secrets** (`.secrets`) → Mots de passe, tokens, etc. (Doppler)

---

## 📁 Structure des fichiers

```
envs/
├── common/
│   ├── compose.env       # Variables d'interpolation communes (IMAGE_TAG, DOCKER_PREFIX, etc.)
│   └── .env              # Variables runtime communes (TZ, POSTGRES_DB, KC_DB, etc.)
│
├── dev/
│   ├── compose.env       # Overrides build-time pour dev (ENV=dev, DOCKER_NETWORK_*, etc.)
│   ├── .env              # Overrides runtime pour dev (API_HOST, SPRING_PROFILES_ACTIVE=local, etc.)
│   └── .secrets          # Secrets dev (POSTGRES_PASSWORD, KC_BOOTSTRAP_ADMIN_PASSWORD, etc.)
│
├── staging/
│   ├── compose.env       # Overrides build-time pour staging
│   ├── .env              # Overrides runtime pour staging
│   └── .secrets          # Secrets staging (Doppler)
│
└── prod/
    ├── compose.env       # Overrides build-time pour prod
    ├── .env              # Overrides runtime pour prod
    └── .secrets          # Secrets prod (Doppler)
```

---

## 🔄 Flux de fusion

### 1. **`merge-env.sh`** → Génère `.env.merged`

```bash
# Fusionne tous les .env (SAUF compose.env et .secrets)
# Ordre de priorité (dernier gagne) :
envs/common/.env
envs/<env>/.env
  ↓
envs/<env>/.env.merged (dédupliqué)
```

**Utilisé par :** `make env-merge ENV=staging`

### 2. **`run-compose.sh`** → Génère fichier temporaire pour interpolation

```bash
# Fusionne compose.env + .secrets pour l'interpolation Docker Compose
# Ordre de priorité :
envs/common/compose.env  (IMAGE_TAG, DOCKER_PREFIX, etc.)
envs/<env>/compose.env   (ENV=staging, DOCKER_NETWORK_*, etc.)
envs/<env>/.secrets      (POSTGRES_PASSWORD, KC_DB_PASSWORD, etc.)
  ↓
/tmp/tchalanet-envs.XXXXXX (temporaire, passé via --env-file)
```

**Utilisé par :** `make up-staging` → `run-compose.sh`

### 3. **Docker Compose** → Utilise les fichiers fusionnés

```yaml
services:
  postgres:
    image: postgres:${POSTGRES_VERSION:-18} # ← Interpolé depuis compose.env
    container_name: ${POSTGRES_CONTAINER}-${ENV} # ← Interpolé depuis compose.env + .secrets
    env_file:
      - ../envs/${ENV}/.env.merged # ← Runtime (APP_DB_NAME, TZ, etc.)
      - ../envs/${ENV}/.secrets # ← Secrets (POSTGRES_PASSWORD, etc.)
```

**Résultat :** Les conteneurs reçoivent TOUTES les variables (interpolation + runtime + secrets)

---

## 📝 Règles de séparation

### ✅ Variables `compose.env` (Build-time / Interpolation)

**Utilisées dans `${}` dans les fichiers YAML :**

- `ENV` (dev, staging, prod)
- `IMAGE_TAG`, `API_IMAGE_BASE`, `KEYCLOAK_IMAGE`, etc.
- `DOCKER_PREFIX`, `DOCKER_NETWORK_EDGE`, `DOCKER_NETWORK_BACK`
- `POSTGRES_CONTAINER`, `REDIS_CONTAINER`, `API_CONTAINER`, etc.
- `POSTGRES_VOLUME`, `KEYCLOAK_DATA_VOLUME`
- `POSTGRES_VERSION`, `REDIS_IMAGE`, `MEILI_IMAGE`, etc.
- `PG_LOG_MAX_SIZE`, `KC_LOG_MAX_FILES`, etc.
- `API_TRAEFIK_ENABLE`, `UNLEASH_TRAEFIK_ENABLE`
- `KC_XMS`, `KC_XMX`, `KC_METASPACE_SIZE`, etc.
- Noms de databases/users NON-SECRETS (POSTGRES_DB, POSTGRES_USER, APP_DB_NAME, KC_DB_USERNAME, etc.)

**Exemples :**

```bash
# envs/common/compose.env
IMAGE_TAG=stg-20251114
DOCKER_PREFIX=tchl
POSTGRES_VERSION=18
POSTGRES_CONTAINER=tchl-postgres
POSTGRES_DB=postgres
APP_DB_NAME=tchalanet_db
```

### ✅ Variables `.env` (Runtime)

**Utilisées par les applications dans les conteneurs :**

- Configuration Spring Boot (SPRING_PROFILES_ACTIVE, SPRING_DATASOURCE_URL, etc.)
- Configuration Keycloak (KC_DB, KC_DB_URL, KC_LOG_LEVEL, etc.)
- Configuration Unleash (NODE_ENV, DATABASE_HOST, LOG_LEVEL, etc.)
- Configuration Meilisearch (MEILI_NO_ANALYTICS, MEILI_DB_PATH, etc.)
- URLs et hostnames (API_HOST, FLAGS_HOST, TCH_KC_ISSUER, etc.)
- Configuration Postgres (TZ, PGDATA, POSTGRES_INITDB_ARGS, etc.)
- Flags et features (ENABLE_METRICS, OTEL_ENABLED, etc.)

**Exemples :**

```bash
# envs/common/.env
TZ=UTC
POSTGRES_INITDB_ARGS=--data-checksums --encoding=UTF8 --locale=C.UTF-8
KC_DB=postgres
KC_LOG_LEVEL=INFO
SPRING_PROFILES_ACTIVE=prod

# envs/staging/.env (override)
SPRING_PROFILES_ACTIVE=staging
API_HOST=api.stg.tchalanet.com
```

### ✅ Variables `.secrets` (Secrets Doppler)

**Mots de passe, tokens, clés sensibles :**

- `POSTGRES_PASSWORD`
- `APP_DB_PASSWORD`, `KC_DB_PASSWORD`, `UNLEASH_DB_PASSWORD`
- `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`
- `REDIS_PASSWORD`
- `MEILI_MASTER_KEY`
- `API_JWT_SECRET`, `KC_CLIENT_SECRET`
- `UNLEASH_ADMIN_TOKEN`, `UNLEASH_CLIENT_TOKEN`

**⚠️ Jamais versionné !** Téléchargé via Doppler :

```bash
make doppler-download-staging
```

---

## 🔍 Exemple complet : Postgres

### 1. **compose.env** (interpolation)

```bash
# envs/common/compose.env
POSTGRES_VERSION=18
POSTGRES_CONTAINER=tchl-postgres
POSTGRES_VOLUME=pgdata

# envs/staging/compose.env
ENV=staging
```

### 2. **.env** (runtime)

```bash
# envs/common/.env
TZ=UTC
PGDATA=/var/lib/postgresql/data
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_INITDB_ARGS=--data-checksums --encoding=UTF8 --locale=C.UTF-8
APP_DB_NAME=tchalanet_db
APP_DB_USER=app_user
KC_DB_NAME=keycloak_db
KC_DB_USERNAME=kc_user
UNLEASH_DB_NAME=unleash_db
UNLEASH_DB_USER=unleash_user
```

### 3. **.secrets** (Doppler)

```bash
# envs/staging/.secrets
POSTGRES_PASSWORD=<secret>
APP_DB_PASSWORD=<secret>
KC_DB_PASSWORD=<secret>
UNLEASH_DB_PASSWORD=<secret>
```

### 4. **docker-compose-postgres.yml**

```yaml
services:
  postgres:
    image: postgres:${POSTGRES_VERSION:-18} # ← compose.env
    container_name: ${POSTGRES_CONTAINER:-tchl-postgres}-${ENV} # ← compose.env
    env_file:
      - ../envs/${ENV}/.env.merged # ← TZ, PGDATA, POSTGRES_DB, APP_DB_NAME, etc.
      - ../envs/${ENV}/.secrets # ← POSTGRES_PASSWORD, APP_DB_PASSWORD, etc.
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ../scripts/docker/postgres-init.sh:/docker-entrypoint-initdb.d/00-init.sh:ro
    # ...

volumes:
  postgres-data:
    name: ${POSTGRES_VOLUME:-pgdata}-${ENV} # ← compose.env
```

### 5. **Résultat dans le conteneur**

Le conteneur `tchl-postgres-staging` reçoit :

```bash
# Variables d'interpolation (compose.env)
ENV=staging

# Variables runtime (.env.merged)
TZ=UTC
PGDATA=/var/lib/postgresql/data
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_INITDB_ARGS=--data-checksums --encoding=UTF8 --locale=C.UTF-8
APP_DB_NAME=tchalanet_db
APP_DB_USER=app_user
KC_DB_NAME=keycloak_db
KC_DB_USERNAME=kc_user

# Secrets (.secrets)
POSTGRES_PASSWORD=<valeur-depuis-doppler>
APP_DB_PASSWORD=<valeur-depuis-doppler>
KC_DB_PASSWORD=<valeur-depuis-doppler>
```

---

## 🛠️ Commandes utiles

### Générer .env.merged

```bash
# Fusionne common/.env + dev/.env → dev/.env.merged
make env-merge ENV=dev
```

### Télécharger les secrets Doppler

```bash
# Télécharge les secrets dans staging/.secrets
DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging
```

### Déployer (fait tout automatiquement)

```bash
# 1. env-merge
# 2. render-traefik
# 3. get-realm
# 4. up-staging (run-compose.sh)
make up-staging
```

### Vérifier les variables dans un conteneur

```bash
# Voir toutes les variables
docker exec tchl-postgres-staging printenv | sort

# Vérifier une variable spécifique
docker exec tchl-postgres-staging printenv POSTGRES_PASSWORD
```

---

## ✅ Avantages de cette architecture

1. **Séparation claire** : Build-time vs Runtime vs Secrets
2. **Pas de duplication** : Chaque variable a un seul endroit logique
3. **Déduplication automatique** : `merge-env.sh` et `run-compose.sh` gèrent les priorités
4. **Sécurité** : `.secrets` jamais versionné, toujours via Doppler
5. **Simplicité des compose YAML** : Pas de blocs `environment:` qui dupliquent `env_file:`
6. **Maintenabilité** : Variables communes dans `common/`, overrides dans `<env>/`

---

## 📚 Fichiers modifiés (14 nov 2025)

- ✅ `scripts/utils/run-compose.sh` - Fusion compose.env + .secrets seulement
- ✅ `scripts/utils/merge-env.sh` - Déjà OK (fusionne .env seulement)
- ✅ `envs/common/compose.env` - Enrichi avec toutes les variables d'interpolation
- ✅ `envs/common/.env` - Enrichi avec toutes les variables runtime communes
- ✅ `envs/dev/.env` - Nettoyé, variables runtime seulement
- ✅ `envs/staging/.env` - Nettoyé, overrides staging seulement
- ✅ `envs/.secrets.example` - Template pour référence
- ✅ `compose/docker-compose-*.yml` - Retrait des blocs `environment:` redondants, utilisation de `env_file:` uniquement

---

**🎉 Architecture finale optimisée et prête pour la production !**
