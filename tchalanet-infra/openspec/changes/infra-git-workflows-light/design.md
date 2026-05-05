# Design: infra-git-workflows-light

## Workflow cible

```text
server-pr.yml       Backend Java/Spring uniquement
web-pr.yml          Angular/Nx uniquement
edge-pr.yml         Fastify/TypeScript uniquement
infra-check.yml     Compose/scripts/realm/env validation uniquement
docs.yml            MkDocs uniquement
deploy-staging.yml  Manuel, build/push/deploy staging
```

## Règles CI

### PR checks

Chaque workflow PR doit utiliser `paths` :

```yaml
on:
  pull_request:
    branches: [main]
    paths:
      - 'tchalanet-server/**'
      - '.github/workflows/server-pr.yml'
```

### Concurrency

Chaque workflow non-déploiement doit annuler l'exécution précédente sur la même branche :

```yaml
concurrency:
  group: server-pr-${{ github.ref }}
  cancel-in-progress: true
```

Pour `deploy-staging.yml` :

```yaml
concurrency:
  group: deploy-staging
  cancel-in-progress: false
```

## `server-pr.yml`

- Trigger : PR touchant `tchalanet-server/**` + manuel.
- JDK : Temurin 25.
- Maven cache.
- Commande cible finale : `./mvnw -B -DskipITs=true verify`.
- Transition possible : `package -DskipTests` si les tests sont temporairement instables, mais documenter comme dette.

## `web-pr.yml`

- Trigger : PR touchant `tchalanet-web/**` + manuel.
- Node `20.19.x`.
- pnpm `10.19.0`.
- `pnpm install --frozen-lockfile`.
- Build Nx cible web.

## `edge-pr.yml`

Edge fait partie de `server-v0`.

- Trigger : PR touchant `tchalanet-edge-service/**` + manuel.
- Node `20.19.x`.
- pnpm `10.19.0`.
- `pnpm lint --if-present`.
- `pnpm test --if-present`.
- `pnpm build`.

## `infra-check.yml`

- Trigger : PR touchant `tchalanet-infra/**` + manuel.
- Ne déploie rien.
- Ne télécharge pas Doppler.
- Ne build pas d'image Docker.
- Valide seulement le scope v0 :

```text
Traefik
PostgreSQL
Keycloak
Redis
API
Edge
Web
```

Les services post-v0 ne doivent pas bloquer ce workflow :

```text
Meilisearch
Unleash
Umami
Mailpit
```

Vérifications :

- `make env-prepare ENV=staging`.
- `docker compose config` sur les fichiers v0.
- génération realm Keycloak staging via `scripts/keycloak/get-realm.sh staging` ou chemin compatible.

## `docs.yml`

- Un seul workflow Docs CI, pas de doublon.
- Build strict sur PR.
- Deploy GitHub Pages seulement sur push `main` et changement `tchalanet-docs/**`.

## `deploy-staging.yml`

Manuel uniquement.

Inputs recommandés :

```yaml
build_api: boolean
build_web: boolean
build_edge: boolean
deploy_infra: boolean
image_tag: string optional
```

Règles :

- Calculer `IMAGE_TAG=sha-${GITHUB_SHA::8}` si absent.
- Utiliser le même `IMAGE_TAG` pour API/Web/Edge.
- Publier GHCR sans `latest`.
- Déployer via SSH et Makefile côté serveur.
- Ne pas déployer prod.
- Ne pas supprimer realm Keycloak.

## Workflows à archiver

Archiver ou supprimer après sauvegarde :

```text
CI/CD Staging - tchalanet-server
Manage Keycloak Realm
Deploy Infra
Build and Publish Images
Check envs on PR
Deploy Production API Only
Docs CI duplicate
Manual Prod Deploy - tchalanet-server
Security Scans automatic on push/PR
```

## Sécurité et coûts

- Les déploiements sont action manuelle explicite.
- Les scans sécurité lourds passent en manuel ou hebdomadaire plus tard.
- Aucun workflow ne doit utiliser `dopplerhq/cli:latest`; si un conteneur Doppler est utilisé plus tard, pinner une version.
- Aucun workflow ne doit publier `:latest`.
