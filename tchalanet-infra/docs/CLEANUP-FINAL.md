# 🧹 Nettoyage final - Variables d'environnement

**Date:** 14 novembre 2025  
**Action:** Suppression des duplications et optimisation finale

---

## ✅ Modifications effectuées

### 1. **`envs/common/compose.env`**

**Avant :** 73 lignes avec duplications  
**Après :** 65 lignes, uniquement variables d'interpolation

**Suppressions :**

- ❌ `POSTGRES_DB`, `POSTGRES_USER` → Dupliqués avec `.env`
- ❌ `APP_DB_NAME`, `APP_DB_USER` → Dupliqués avec `.env`
- ❌ `KC_DB_NAME`, `KC_DB_USERNAME` → Dupliqués avec `.env`
- ❌ `UNLEASH_DB_NAME`, `UNLEASH_DB_USER` → Dupliqués avec `.env`
- ❌ `API_BUILD_ARG_EXAMPLE` → Non utilisé

**Ajouts :**

- ✅ `ENV=dev` (défaut)
- ✅ `DOPPLER_IMAGE` (pour docker-compose-doppler.yml)
- ✅ `UNLEASH_EDGE_HOST_PORT` (pour unleash-host.yml)

---

### 2. **`envs/common/.env`**

**Avant :** 59 lignes  
**Après :** 53 lignes, variables runtime uniquement

**Réorganisation :**

- Variables regroupées par service
- Commentaires simplifiés
- Pas de duplications avec `compose.env`

---

### 3. **`envs/dev/.env`**

**Avant :** 108 lignes avec ÉNORMES duplications  
**Après :** 32 lignes, uniquement overrides dev

**Suppressions (toutes dupliquées avec `common/.env`) :**

- ❌ `KC_DB`, `KC_DB_URL`, `KC_LOG_LEVEL`, etc. (Keycloak runtime)
- ❌ `TZ`, `PGDATA`, `POSTGRES_DB`, etc. (Postgres runtime)
- ❌ `APP_DB_NAME`, `APP_DB_USER` (DB config)
- ❌ `KC_DB_NAME`, `KC_DB_USERNAME` (Keycloak DB)
- ❌ `UNLEASH_DB_NAME`, `UNLEASH_DB_USER`, `NODE_ENV`, etc. (Unleash runtime)
- ❌ `MEILI_NO_ANALYTICS`, `MEILI_DB_PATH`, etc. (Meilisearch)
- ❌ `REDIS_HOST`, `REDIS_PORT` (Redis, sauf si override nécessaire)
- ❌ `TCH_KC_AUDIENCE` (dupliqué)

**Conservé (overrides dev) :**

- ✅ Hosts locaux (`*.localtest.me`)
- ✅ `SPRING_PROFILES_ACTIVE=local`
- ✅ URLs dev-specific
- ✅ Tokens/credentials dev

---

### 4. **Fichiers compose YAML**

#### `docker-compose-postgres.yml`

**Supprimé :**

```yaml
environment:
  TZ: UTC
  PGDATA: /var/lib/postgresql/data
  POSTGRES_INITDB_ARGS: '${POSTGRES_INITDB_ARGS:---data-checksums...}'
```

**Raison :** Toutes ces variables sont déjà dans `.env.merged`

#### `docker-compose-api.yml`

**Supprimé :**

```yaml
environment:
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${APP_DB_NAME}
  SPRING_DATASOURCE_USERNAME: ${APP_DB_USER}
  SPRING_DATASOURCE_PASSWORD: ${APP_DB_PASSWORD}
  TCH_KC_ISSUER: ${KC_OIDC_ISSUER_URL}
  TCH_KC_AUDIENCE: tch-api
  TCH_REDIS_URL: redis://redis:6379
```

**Raison :** Toutes ces variables sont déjà dans `.env.merged` + `.secrets`

#### `docker-compose-unleash.yml`

**Supprimé :**

```yaml
environment:
  NODE_ENV: production
  LOG_LEVEL: info
  ENABLE_API_TOKEN: 'true'
  SECURE_HEADERS: 'true'
  UNLEASH_AUTH_TYPE: none
  DATABASE_HOST: 'postgres'
  DATABASE_PORT: '5432'
  DATABASE_NAME: ${UNLEASH_DB_NAME}
  DATABASE_USERNAME: ${UNLEASH_DB_USER}
  DATABASE_PASSWORD: ${UNLEASH_DB_PASSWORD}
  DATABASE_SSL: 'false'
```

**Raison :** Toutes ces variables sont déjà dans `.env.merged` + `.secrets`

#### `docker-compose-meilisearch.yml`

**Supprimé :**

```yaml
environment:
  MEILI_NO_ANALYTICS: 'true'
  MEILI_DB_PATH: /meili_data
  MEILI_HTTP_ADDR: 0.0.0.0:7700
```

**Raison :** Toutes ces variables sont déjà dans `.env.merged`

---

## 📊 Résumé des réductions

| Fichier                          | Avant         | Après     | Réduction |
| -------------------------------- | ------------- | --------- | --------- |
| `common/compose.env`             | 73 lignes     | 65 lignes | -11%      |
| `common/.env`                    | 59 lignes     | 53 lignes | -10%      |
| `dev/.env`                       | 108 lignes    | 32 lignes | **-70%**  |
| `docker-compose-postgres.yml`    | 6 lignes env  | 0 lignes  | -100%     |
| `docker-compose-api.yml`         | 17 lignes env | 0 lignes  | -100%     |
| `docker-compose-unleash.yml`     | 18 lignes env | 0 lignes  | -100%     |
| `docker-compose-meilisearch.yml` | 6 lignes env  | 0 lignes  | -100%     |

**Total :** ~287 lignes → ~150 lignes = **-48% de code**

---

## ✅ Vérifications effectuées

### 1. Toutes les variables d'interpolation sont définies

```bash
# Variables utilisées dans compose YAML
grep -oh '\${[A-Z_]*' compose/*.yml | sed 's/\${//g' | sort -u

# Variables définies dans compose.env
cat envs/common/compose.env envs/dev/compose.env | grep -E "^[A-Z_]+=" | cut -d= -f1 | sort -u

# Résultat : ✅ 100% de couverture, aucune variable manquante
```

### 2. Pas de duplications entre compose.env et .env

- `compose.env` → Variables d'interpolation (`IMAGE_TAG`, `DOCKER_PREFIX`, etc.)
- `.env` → Variables runtime (`TZ`, `KC_LOG_LEVEL`, `SPRING_PROFILES_ACTIVE`, etc.)
- **Aucune variable commune** entre les deux fichiers

### 3. Syntaxe YAML valide

```bash
# Vérification avec get_errors
✅ docker-compose-postgres.yml - No errors
✅ docker-compose-keycloak.yml - No errors
✅ docker-compose-api.yml - No errors
✅ docker-compose-unleash.yml - No errors
✅ docker-compose-redis.yml - No errors
✅ docker-compose-meilisearch.yml - No errors
```

### 4. Variables obligatoires présentes

**compose.env (interpolation) :**

- ✅ `ENV`, `IMAGE_TAG`, `DOCKER_PREFIX`
- ✅ Tous les noms d'images (`*_IMAGE`)
- ✅ Tous les noms de conteneurs (`*_CONTAINER`)
- ✅ Networks (`DOCKER_NETWORK_*`)
- ✅ Logging config (`*_LOG_MAX_*`)

**common/.env (runtime) :**

- ✅ Postgres config (`TZ`, `POSTGRES_DB`, `POSTGRES_INITDB_ARGS`)
- ✅ Keycloak config (`KC_DB`, `KC_LOG_LEVEL`, etc.)
- ✅ Unleash config (`NODE_ENV`, `DATABASE_HOST`, etc.)
- ✅ Meilisearch config (`MEILI_NO_ANALYTICS`, etc.)
- ✅ Spring Boot (`SPRING_PROFILES_ACTIVE`)

**.secrets (secrets) :**

- ✅ `POSTGRES_PASSWORD`, `APP_DB_PASSWORD`, `KC_DB_PASSWORD`, `UNLEASH_DB_PASSWORD`
- ✅ `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`
- ✅ `REDIS_PASSWORD`, `MEILI_MASTER_KEY`

---

## 🎯 Architecture finale propre

### Flux simplifié

```
┌─────────────────────────────────────────────────────────┐
│ compose.env (interpolation)                             │
│ - IMAGE_TAG, DOCKER_PREFIX, container names             │
│ - Utilisé par docker compose --env-file                 │
│                                                         │
│ common/compose.env  (65 lignes)                         │
│ dev/compose.env     (10 lignes, overrides)             │
└─────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────┐
│ .env (runtime)                                          │
│ - TZ, KC_LOG_LEVEL, SPRING_PROFILES_ACTIVE             │
│ - Fusionné par merge-env.sh → .env.merged              │
│                                                         │
│ common/.env  (53 lignes)                                │
│ dev/.env     (32 lignes, overrides)                     │
└─────────────────────────────────────────────────────────┐
                              ↓
┌─────────────────────────────────────────────────────────┐
│ .secrets (Doppler)                                      │
│ - POSTGRES_PASSWORD, KC_DB_PASSWORD, etc.              │
│ - Téléchargé via make doppler-download                 │
└─────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────┐
│ Docker Compose YAML                                     │
│ - Interpolation via --env-file (compose.env + .secrets)│
│ - Runtime via env_file: (.env.merged + .secrets)       │
│ - PAS de bloc environment: redondant                    │
└─────────────────────────────────────────────────────────┘
```

### Principe : DRY (Don't Repeat Yourself)

Chaque variable a **UN SEUL** emplacement :

| Variable                 | Emplacement               | Usage                            |
| ------------------------ | ------------------------- | -------------------------------- |
| `IMAGE_TAG`              | `compose.env`             | Interpolation YAML               |
| `POSTGRES_PASSWORD`      | `.secrets`                | Secret (interpolation + runtime) |
| `TZ`                     | `.env`                    | Runtime uniquement               |
| `SPRING_PROFILES_ACTIVE` | `.env` (override par env) | Runtime                          |

**Plus de duplications = Plus de maintenance facile = Moins d'erreurs**

---

## 🚀 Prochaines étapes

1. **Tester en local :**

   ```bash
   make env-merge ENV=dev
   make up-all ENV=dev
   docker ps
   ```

2. **Vérifier les variables dans les conteneurs :**

   ```bash
   docker exec tchl-postgres-dev printenv | grep POSTGRES
   docker exec tchl-api-dev printenv | grep SPRING
   docker exec tchl-keycloak-dev printenv | grep KC_
   ```

3. **Déployer en staging :**
   ```bash
   DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging
   make deploy-staging
   ```

---

**✨ Architecture finale : propre, simple, maintenable !**
