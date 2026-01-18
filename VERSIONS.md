# VERSIONS.md — Tchalanet (Source de vérité)

Règle : aucune version (runtime/build/service) ne doit changer sans :

1. mise à jour de ce fichier
2. mise à jour du wrapper correspondant (Maven/Nx/pnpm)
3. mise à jour des images docker (compose/infra)
4. note dans le changelog si impact prod

---

## Sources de vérité (où lire/éditer)

- Backend runtime/build : `tchalanet-server/pom.xml` (+ `./mvnw`)
- Infra images/tags : `tchalanet-infra/envs/common/compose.env` + `compose/*`
- Web/Mobile (Nx) : `package.json` racine + `pnpm-lock.yaml`
- Version pnpm : `package.json#packageManager` + Corepack
- Edge service : `tchalanet-edge-service/package.json`

---

## 1) Backend (tchalanet-server)

- Java : 25 (défini dans `tchalanet-server/pom.xml`)
- Spring Boot : 4.0.1 (parent défini dans `tchalanet-server/pom.xml`)
- Build tool : Maven (wrapper présent : `./mvnw`)
- DB driver : PostgreSQL JDBC (version gérée via BOM ou dépendances Maven)
- Migration : Flyway (utilisé dans le POM)

### Notes backend

- `tchalanet-server/pom.xml` exige Java >= 25 via maven-enforcer.
- Certains modules (ex: `tchalanet-infra/keycloak/tchalanet-keycloak-provider/pom.xml`) sont actuellement configurés pour Java 21.
  - Toute homogénéisation Java 25 doit passer par une PR dédiée + build complet.

### Dépendances infra backend (defaults via `tchalanet-infra/envs/common/compose.env`)

- PostgreSQL : 18.1 (`POSTGRES_VERSION=18.1`)
  - ⚠️ À valider : tag officiel Docker Hub ou image custom (documenter la source de registry).
- Redis : 8.4.0 (`REDIS_IMAGE=redis:8.4.0`)
- Meilisearch : v1.11 (`MEILI_IMAGE=getmeili/meilisearch:v1.11`)
- Keycloak : `ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2` (`KEYCLOAK_IMAGE`)
- Traefik : `traefik:v3.6.5` (`TRAEFIK_IMAGE=traefik:v3.6.5`)
- Unleash : `unleashorg/unleash-server:7.4.0` (`UNLEASH_IMAGE=unleashorg/unleash-server:7.4.0`)
- API image base / tag defaults : `ghcr.io/bhebb/tchalanet-api:${IMAGE_TAG:-stg-20251116-2}`
- Doppler CLI (infra tooling) : `dopplerhq/cli:3.75.1` (`DOPPLER_IMAGE`)

---

## 2) Frontend Web (Nx / Angular)

### Runtime & package manager

- Node (runtime dev/CI) : 20.19.x (aligné avec `@types/node: 20.19.9`)
  - Reco repo : ajouter `.nvmrc` = `20.19.9`
- Package manager : pnpm 10.19.0 (pin via `package.json#packageManager` + corepack)

### Tooling

- Nx : 21.4.1 (`nx` pinned)
- TypeScript : 5.8.2 (`typescript` ~5.8.2)
- Vite : 6.x (`vite` ^6.0.0)

### Angular

- Angular runtime : 20.2.4 (`@angular/*` ~20.2.4)
- Angular tooling : 20.1.x (`@angular/cli`, `@angular/build`, `@angular-devkit/*` ~20.1.0)
- Angular Material/CDK : 20.2.x (`@angular/material` ^20.2.2, `@angular/cdk` ^20.2.2)

---

## 3) Mobile (Ionic / Capacitor)

- Ionic : 8.7.x (`@ionic/angular` ^8.7.3)
- Capacitor : 7.4.x (`@capacitor/*` ^7.4.3)
- Android Gradle plugin : défini dans le projet Android (si activé)
- iOS tools : Xcode (dépend de la cible iOS)

---

## 4) Infra / Docker / Edge

Valeurs extraites depuis `tchalanet-infra/envs/common/compose.env` et fichiers `compose/*` :

- Docker Engine : documenter min recommandé dans `tchalanet-infra/README`
- Docker Compose : compatible avec `docker compose` v2
- Traefik : `traefik:v3.6.5`
- Postgres : `postgres:${POSTGRES_VERSION}` (actuel = 18.1) ⚠️ valider tag/registry
- Redis : `redis:8.4.0`
- Meilisearch : `getmeili/meilisearch:v1.11`
- Keycloak : `ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2`
- Unleash : `unleashorg/unleash-server:7.4.0`
- API image base : `ghcr.io/bhebb/tchalanet-api` + `IMAGE_TAG` default `stg-20251116-2`
- Doppler CLI : `dopplerhq/cli:3.75.1`

---

## 5) Edge service (`tchalanet-edge-service`)

Valeurs extraites depuis `tchalanet-edge-service/package.json` :

- package version : 0.1.0
- dependencies :
  - axios: ^1.7.0
  - express: ^4.19.0
  - dotenv: ^16.4.0
  - json-rules-engine: ^7.0.0
  - liquidjs: ^10.10.0
  - morgan: ^1.10.0
- devDependencies :
  - @types/node: ^22.0.0
  - ts-node-dev: ^2.0.0
  - typescript: ^5.6.0

Notes :

- Runtime Node du repo reste 20.19.x ; les type defs edge peuvent être plus hauts mais ne doivent pas imposer un runtime Node différent sans décision explicite.

---

## 6) Politique de mise à jour

- Pas de `:latest` en production.
- Tags docker pinés.
- Upgrades majeures → PR dédiée + tests + plan de rollback.
- Toute PR qui change une version doit :
  - modifier ce fichier
  - modifier le wrapper concerné (mvnw / packageManager / compose.env)
  - documenter l’impact (changelog / release notes)

---

## Vérifications locales rapides

```bash
# Java / Maven
java -version
./mvnw -v

# Node / pnpm / nx
node -v
corepack --version || true
pnpm -v
npx nx --version

# Vérifier Spring Boot parent dans le POM
grep -n "spring-boot-starter-parent" -R tchalanet-server/pom.xml

# Vérifier java.version dans POMs
grep -n "<java.version>" -R tchalanet-server tchalanet-infra | sed -n '1,80p'

# Vérifier infra envs (images)
grep -n "POSTGRES_VERSION=\|REDIS_IMAGE=\|MEILI_IMAGE=\|KEYCLOAK_IMAGE=\|TRAEFIK_IMAGE=\|UNLEASH_IMAGE=\|DOPPLER_IMAGE=" \
  -R tchalanet-infra/envs/common/compose.env
```
