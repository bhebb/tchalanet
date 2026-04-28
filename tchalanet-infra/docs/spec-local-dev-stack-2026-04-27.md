# spec-local-dev-stack-2026-04-27.md

> **Type** : Spec opérationnelle — environnement de développement local
> **Date** : 2026-04-27
> **Scope** : `tchalanet-infra` + `tchalanet-server` > **Auteur** : Généré par analyse automatique du repo

---

## Résumé

Ce document décrit l'état actuel de la stack de développement local (Postgres 18.1,
Keycloak custom, Traefik v3.6.5, Redis 8.4.0) et identifie les blocages, incohérences
et actions nécessaires pour qu'un développeur puisse démarrer en moins de 10 minutes.

---

## 1. Images Docker

### 1.1 État actuel

| Service     | Source                                | Version effective                                                       | Statut                |
| ----------- | ------------------------------------- | ----------------------------------------------------------------------- | --------------------- |
| Postgres    | `compose/docker-compose-postgres.yml` | `postgres:${POSTGRES_VERSION:-18.1}` → **18.1**                         | ✅ Aligné VERSIONS.md |
| Keycloak    | `compose/docker-compose-keycloak.yml` | `${KEYCLOAK_IMAGE}` = `ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2` | ✅ Image custom pinée |
| Traefik     | `compose/docker-compose-traefik.yml`  | `${TRAEFIK_IMAGE:-traefik:v3.5.4}` mais `compose.env` → **v3.6.5**      | ⚠️ Voir §1.2          |
| Redis       | `envs/common/compose.env`             | `redis:8.4.0`                                                           | ✅ Pinée              |
| Unleash     | `envs/common/compose.env`             | `unleashorg/unleash-server:7.4.0`                                       | ✅ Pinée              |
| Meilisearch | `envs/common/compose.env`             | `getmeili/meilisearch:v1.11`                                            | ✅ Pinée              |

### 1.2 Incohérence Traefik

`docker-compose-traefik.yml` déclare un fallback `traefik:v3.5.4`
alors que `envs/common/compose.env` définit `TRAEFIK_IMAGE=traefik:v3.6.5`.

- Si `up-seq.sh` est lancé sans `make env-merge` au préalable → fallback v3.5.4 utilisé.
- **Fix** : s'assurer que `make env-merge` précède toujours `up`.

### 1.3 Keycloak — image custom obligatoire

Le realm `tchalanet-realm.json` utilise le mapper `tch-json-claim-mapper`
(scope `tch`) qui est un **provider custom** (`tchalanet-keycloak-provider`).
Sans l'image custom `ghcr.io/bhebb/tchalanet-keycloak:*`, le démarrage de Keycloak
échoue avec `Provider 'tch-json-claim-mapper' not found`.

→ **Ne jamais utiliser** `quay.io/keycloak/keycloak:26.x` directement pour ce projet.
→ En dev : `make rebuild-keycloak` pour builder l'image localement.

---

## 2. Configuration Postgres

### 2.1 Bases de données et utilisateurs

Le script `scripts/docker/postgres-init.sh` crée idempotamment :

| Base           | Utilisateur    | Variable secret       |
| -------------- | -------------- | --------------------- |
| `tchalanet_db` | `app_user`     | `APP_DB_PASSWORD`     |
| `keycloak_db`  | `kc_user`      | `KC_DB_PASSWORD`      |
| `unleash_db`   | `unleash_user` | `UNLEASH_DB_PASSWORD` |

**Alignement** : `application.yaml` pointe sur `tchalanet_db`/`app_user` ✅
(`SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tchalanet_db`)

### 2.2 Extensions PostgreSQL

Extensions créées via **Flyway** (V1) — pas dans `postgres-init.sh` :

```sql
CREATE EXTENSION IF NOT EXISTS citext;    -- V1
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- V1 + V34 (idempotent)
```

⚠️ **Blocage potentiel** : `CREATE EXTENSION` requiert `SUPERUSER` ou `rds_superuser`.
L'utilisateur `app_user` ne dispose pas de ce privilege par défaut.

**Fix** : ajouter dans `postgres-init.sh` après la création de `tchalanet_db` :

```sql
\c tchalanet_db
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
GRANT USAGE ON SCHEMA public TO app_user;
```

Ou accorder le rôle pg nommé `pg_extension_owner` en PostgreSQL 15+.
Sur PostgreSQL 18, `CREATE EXTENSION` par un non-superuser est possible
si l'extension est dans la liste `trusted` ET que l'utilisateur possède la DB.
`pgcrypto` et `citext` sont des extensions **trusted** → `app_user` (owner de `tchalanet_db`)
peut les créer. ✅ **Pas de blocage** sur PG 18 — à valider si migration vers PG < 15.

### 2.3 RLS (Row Level Security)

- **V1** : fonctions RLS (`set_current_tenant`, `current_tenant`, `set_deleted_visibility`)
- **V2** : `ALTER TABLE address ENABLE ROW LEVEL SECURITY`
- **V40** : activation RLS + FORCE sur toutes les tables tenant-scoped

✅ Conformité architecture : isolation tenant par RLS, pas par code Java.

**Point d'attention** : `app_user` doit avoir `BYPASSRLS = false` (défaut).
Le superuser Postgres (`POSTGRES_USER=postgres`) bypass RLS — ne jamais utiliser
dans l'application.

### 2.4 Schéma `batch`

`application.yaml` déclare :

```yaml
spring:
  flyway:
    schemas: public,batch
```

Flyway crée le schéma `batch` automatiquement s'il n'existe pas, mais
`app_user` doit avoir le droit `CREATE SCHEMA`. En PostgreSQL, le owner
d'une base peut créer des schémas → ✅ OK si `app_user` est owner de `tchalanet_db`.

---

## 3. Configuration Keycloak

### 3.1 Realm

✅ `keycloak/realms/tchalanet-realm.json` présent et monté dans le conteneur :

```
/opt/keycloak/data/import/tchalanet-realm.json
```

### 3.2 Clients configurés

| Client ID           | Type        | Usage                         |
| ------------------- | ----------- | ----------------------------- |
| `tchalanet-web`     | Public PKCE | Frontend Angular              |
| `tchalanet-api`     | Bearer-only | Resource server (Spring Boot) |
| `tchalanet-swagger` | Public PKCE | Swagger UI Authorize          |

### 3.3 Audience claim

Le client `tchalanet-web` a un mapper `aud-tchalanet-api` qui injecte
`tchalanet-api` dans le claim `aud`. L'API vérifie :

```yaml
app.security.required-audience: tchalanet-api
```

✅ Cohérent.

### 3.4 Alignement issuer-uri

| Contexte                          | Valeur                                                |
| --------------------------------- | ----------------------------------------------------- |
| `application.yaml` (défaut)       | `http://keycloak:8080/realms/tchalanet` (Docker)      |
| `application-local-ide.yaml`      | `https://auth.localtest.me/realms/tchalanet` (IDE)    |
| `docker-compose-keycloak.yml` env | `KC_HOSTNAME=auth.localtest.me`                       |
| `.env.merged`                     | `TCH_KC_ISSUER=http://keycloak:8080/realms/tchalanet` |

⚠️ **Blocage IDE local** : le profil `local-ide` utilise `https://auth.localtest.me`
qui doit être accessible depuis la machine hôte. Cela nécessite :

- Traefik en cours d'exécution (port 443 ouvert)
- Certificats mkcert valides pour `*.localtest.me`
- `localtest.me` résout `127.0.0.1` par design (DNS public) ✅

### 3.5 Utilisateurs de test pré-configurés dans le realm

| Username      | Password   | Rôle         |
| ------------- | ---------- | ------------ |
| `super_admin` | `changeme` | SUPER_ADMIN  |
| `admin`       | `changeme` | TENANT_ADMIN |
| `agent`       | `changeme` | OPERATOR     |

---

## 4. Traefik

### 4.1 Dashboard

- **Dev** : accessible sur `http://localhost:8080` (insecure, `TRAEFIK_API_INSECURE=true` dans `envs/dev/compose.env`)
- Aussi via `https://traefik.localtest.me` (websecure + mkcert)

### 4.2 Routing

`traefik/dynamic/10-routers.yaml` définit :

| Host                   | Service backend                    |
| ---------------------- | ---------------------------------- |
| `api.localtest.me`     | `http://api:8080`                  |
| `auth.localtest.me`    | `http://keycloak:8080`             |
| `flags.localtest.me`   | `http://unleash:4242`              |
| `app.localtest.me`     | `http://host.docker.internal:4200` |
| `mob.localtest.me`     | `http://host.docker.internal:4201` |
| `traefik.localtest.me` | `api@internal`                     |

### 4.3 Certificats locaux (mkcert)

Présents dans `traefik/certs/` :

- `local-cert.pem` / `local-key.pem`

Générés via `make mkcert-local` (utilise `mkcert`).

⚠️ Si les certs sont expirés ou absents → `make mkcert-recreate`.

Prérequis : `mkcert` installé + CA mkcert dans le trust store système.

```bash
brew install mkcert
mkcert -install
```

### 4.4 Ping endpoint

`traefik.yml` configure :

```yaml
ping:
  entryPoint: 'websecure'
```

Le healthcheck `CMD traefik healthcheck --ping` interroge l'endpoint ping.
Si Traefik démarre avant que les certs soient chargés, le healthcheck peut
échouer temporairement. Le `start_period: 5s` est suffisant en pratique.

---

## 5. application.yaml — Profil local/dev

### 5.1 Paramètres critiques

| Paramètre                                              | Valeur                                         | Statut                                             |
| ------------------------------------------------------ | ---------------------------------------------- | -------------------------------------------------- |
| `spring.datasource.hikari.jdbc-url`                    | `jdbc:postgresql://postgres:5432/tchalanet_db` | ✅ (Docker) / override `localhost` via `local-ide` |
| `spring.flyway.enabled`                                | `true`                                         | ✅                                                 |
| `spring.jpa.hibernate.ddl-auto`                        | `validate`                                     | ✅                                                 |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://keycloak:8080/realms/tchalanet`        | ✅ Docker / override `local-ide`                   |
| `spring.batch.job.enabled`                             | `false`                                        | ✅                                                 |
| `spring.flyway.schemas`                                | `public,batch`                                 | ✅                                                 |

### 5.2 ⚠️ Anomalie dans `setup-api-env.sh`

Le script `scripts/local/setup-api-env.sh` génère un `.env` incorrect pour l'API :

- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres` → mauvaise DB (`postgres` au lieu de `tchalanet_db`) et mauvais user (`postgres` au lieu de `app_user`)
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8082/realms/tchalanet-dev` → realm inexistant (`tchalanet-dev` au lieu de `tchalanet`)
- `SPRING_JPA_HIBERNATE_DDL_AUTO=update` → **interdit** par convention (doit être `validate`)

**Ce script ne doit pas être utilisé** pour le démarrage. Utiliser le profil `local-ide`
avec les valeurs de `application-local-ide.yaml`.

---

## 6. Config manquante / Blocages identifiés

| #   | Problème                                                                                         | Gravité          | Impact                                                             |
| --- | ------------------------------------------------------------------------------------------------ | ---------------- | ------------------------------------------------------------------ |
| B1  | `make env-merge` non exécuté → `.env.merged` absent → `KEYCLOAK_IMAGE` non résolu                | 🔴 BLOQUANT      | Keycloak ne démarre pas                                            |
| B2  | `make mkcert-local` non exécuté → certs absents                                                  | 🔴 BLOQUANT      | Traefik ne démarre pas (pas de TLS pour les entrypoints websecure) |
| B3  | `make mkcert-local` → `mkcert` non installé                                                      | 🔴 BLOQUANT      | Idem                                                               |
| B4  | Keycloak image custom non buildée (`ghcr.io/bhebb/tchalanet-keycloak:*` non pull-able sans auth) | 🔴 BLOQUANT      | `docker pull` échoue si GHCR non accessible                        |
| B5  | Réseaux Docker `edge-dev`/`back-dev` absents                                                     | 🔴 BLOQUANT      | `docker compose up` échoue sur `network not found`                 |
| W1  | `setup-api-env.sh` génère des valeurs incorrectes                                                | 🟠 AVERTISSEMENT | Spring Boot démarre sur la mauvaise DB/Keycloak                    |
| W2  | `TRAEFIK_IMAGE` fallback `v3.5.4` vs `compose.env` `v3.6.5`                                      | 🟡 MINEUR        | Version différente si env-merge sauté                              |
| W3  | `traefik.yml` `ping` sur `websecure` — healthcheck sensible au boot TLS                          | 🟡 MINEUR        | Healthcheck peut timeout la première fois                          |

---

## 7. Fichiers à créer/modifier

### 7.1 Aucune modification requise sur les compose files

L'infrastructure existe et est cohérente. Les blocages sont opérationnels (env non mergé,
mkcert non installé, réseaux non créés), pas structurels.

### 7.2 Fix recommandé : corriger `setup-api-env.sh`

Le script génère le `.env` API avec des valeurs incorrectes.
Remplacer les lignes problématiques :

```bash
# Avant (incorrect) :
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8082/realms/tchalanet-$ENV
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Après (correct) :
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tchalanet_db
SPRING_DATASOURCE_USERNAME=app_user
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.localtest.me/realms/tchalanet
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

### 7.3 Fix recommandé : ajouter `SPRING_PROFILES_ACTIVE` dans `application-local-ide.yaml`

Actuellement le fichier ne se déclenche que si le profil `local-ide` est actif.
La procédure de démarrage doit explicitement passer `-Dspring.profiles.active=local-ide`.

---

## 8. Procédure de démarrage complète

### Prérequis (une seule fois par machine)

```bash
# 1. Installer mkcert et enregistrer la CA dans le trust store
brew install mkcert
mkcert -install

# 2. S'authentifier sur GHCR pour puller l'image Keycloak custom
echo "$GHCR_PAT" | docker login ghcr.io -u <github_username> --password-stdin
# OU builder l'image localement (sans accès GHCR) :
cd tchalanet-infra
make rebuild-keycloak ENV=dev
```

### Démarrage quotidien

```bash
cd tchalanet-infra

# Étape 1 — Fusionner les variables d'environnement
make env-merge ENV=dev

# Étape 2 — (Si pas fait) Générer les certificats mkcert locaux
make mkcert-local

# Étape 3 — Créer les réseaux Docker (idempotent)
make networks ENV=dev

# Étape 4 — Démarrer toute la stack (Traefik → Postgres → Redis → Keycloak → Unleash → API)
make up-all ENV=dev
# Note: up-all remplit automatiquement les étapes 1-3 + realm Keycloak

# Vérifier que tous les services sont healthy
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Option A — API dans Docker (image pre-built)

La stack `make up-all` démarre l'API via `docker-compose-api.yml` avec l'image GHCR.

### Option B — API en local IDE (Spring Boot natif)

```bash
# Stack infra seulement (pas l'API dans Docker)
cd tchalanet-infra
make env-merge ENV=dev
make mkcert-local
make networks ENV=dev

# Démarrer Traefik + Postgres + Redis seulement
docker compose \
  --project-name tch-dev \
  --env-file envs/dev/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-traefik.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose.override.yml \
  up -d traefik postgres redis

# Démarrer Keycloak (attendre qu'il soit healthy avant l'API)
docker compose \
  --project-name tch-dev \
  --env-file envs/dev/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose.local-build.yml \
  up -d keycloak

# Attendre que Keycloak soit prêt
./scripts/utils/wait-keycloak.sh tchalanet https://auth.localtest.me 240

# Démarrer l'API Spring Boot en local (profil local-ide)
cd ../tchalanet-server
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local-ide \
  -Dspring-boot.run.jvmArguments="-DSPRING_DATASOURCE_PASSWORD=devpass -DREDIS_PASSWORD=devredis"
```

### Option C — Démarrage minimal (Postgres + Redis uniquement, sans auth)

Pour développement d'endpoints publics sans token Keycloak :

```bash
cd tchalanet-infra
make env-merge ENV=dev && make networks ENV=dev

docker compose \
  --project-name tch-dev \
  --env-file envs/dev/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose.override.yml \
  up -d postgres redis

# Spring Boot : désactiver OAuth2 resource server en local (ajouter property temporaire)
cd ../tchalanet-server
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local-ide \
  -Dspring.security.oauth2.resourceserver.jwt.issuer-uri="" \
  -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
```

---

## 9. Vérification post-démarrage

### Healthchecks Docker

```bash
# Tous les services doivent afficher "healthy" ou "Up"
docker ps --format "table {{.Names}}\t{{.Status}}"

# Logs en cas de problème
docker logs -f tchalanet-postgres-dev
docker logs -f tch-keycloak-dev
docker logs -f tchl-traefik-dev
```

### Flyway

```bash
# Vérifier que toutes les migrations sont appliquées
docker exec tchalanet-postgres-dev \
  psql -U app_user -d tchalanet_db \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"
```

### Endpoints de santé

```bash
# 1. Postgres accessible
psql "postgresql://app_user:devpass@localhost:5432/tchalanet_db" -c "SELECT version();"

# 2. Keycloak realm disponible
curl -s https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq .issuer

# 3. API Spring Boot health
curl -s http://localhost:8083/api/v1/actuator/health | jq .status
# Attendu : "UP"

# 4. Endpoint public
curl -s https://api.localtest.me/api/v1/public/draws | jq .

# 5. Token Keycloak + appel authentifié
TOKEN=$(curl -s -X POST \
  https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token \
  -d "grant_type=password&client_id=tchalanet-web&username=super_admin&password=changeme&scope=openid" \
  | jq -r .access_token)

curl -s -H "Authorization: Bearer $TOKEN" \
  https://api.localtest.me/api/v1/tenant/draws | jq .
```

---

## 10. Checklist DoD

```
[ ] Prérequis machine
    [ ] mkcert installé + mkcert -install exécuté
    [ ] Docker Desktop actif (Compose V2)
    [ ] make disponible
    [ ] Accès GHCR ou image Keycloak buildée localement

[ ] Stack Docker
    [ ] make env-merge ENV=dev → envs/dev/.env.merged généré
    [ ] make mkcert-local → traefik/certs/*.pem présents
    [ ] make networks ENV=dev → edge-dev / back-dev créés
    [ ] docker compose up -d → tous les services healthy
        [ ] postgres (healthy)
        [ ] redis (healthy)
        [ ] keycloak (healthy — peut prendre 2-3 min au 1er démarrage)
        [ ] traefik (healthy)

[ ] Flyway
    [ ] ./mvnw spring-boot:run démarre sans erreur Flyway
    [ ] Aucune migration en erreur (success=true pour toutes)
    [ ] Schémas public + batch présents dans tchalanet_db

[ ] API
    [ ] GET http://localhost:8083/api/v1/actuator/health → {"status":"UP"}
    [ ] GET https://api.localtest.me/api/v1/public/draws → 200 (via Traefik)

[ ] Keycloak + Auth
    [ ] https://auth.localtest.me accessible (cert mkcert valide, pas de warning browser)
    [ ] Token obtenu avec super_admin/changeme via grant_type=password
    [ ] GET /api/v1/tenant/draws avec le token → 200 (non 401/403)

[ ] Swagger UI
    [ ] https://api.localtest.me/api/v1/swagger-ui → accessible
    [ ] Bouton Authorize → flow PKCE → login Keycloak → token injecté
```

---

## 11. Questions ouvertes / ADR potentiels

| #   | Sujet                                                                                                                              | Recommandation                                                                       |
| --- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Q1  | `setup-api-env.sh` génère des valeurs incorrectes                                                                                  | Corriger ou déprécier au profit du profil `local-ide`                                |
| Q2  | Keycloak custom image requise même en dev                                                                                          | Documenter la procédure `make rebuild-keycloak` dans `QUICK-START.md`                |
| Q3  | `ping` Traefik sur `websecure` — boot time sensible                                                                                | Passer `ping.entryPoint: web` en dev ou ajouter `start_period: 15s`                  |
| Q4  | `app.security.required-audience: tchalanet-api` dans `application.yaml` mais `app.security.required-audience: ""` dans `local-ide` | Documenter que le profil `local-ide` désactive la validation audience (pratique dev) |
