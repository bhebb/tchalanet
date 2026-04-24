---
name: infrastructure
description: >
  Use when writing or reviewing Docker Compose files, CI/CD workflows, environment configuration, secret management, or Traefik routing in tchalanet-infra — enforces pinned image versions, declarative environments, no-latest policy, and Doppler secret injection.
---

# Infrastructure — Règles et conventions

> Source de vérité : `tchalanet-infra/docs/OPERATIONS.md`
> Context pack : `openspec/context/60-infra-rules.md`

## Principes fondamentaux

- Infrastructure **déclarative**
- Environnements **reproductibles**
- Pas d'étapes manuelles cachées
- Pas de valeurs implicites

---

## Images Docker (NON-NÉGOCIABLE)

- ❌ Jamais `:latest` en production (ni en staging)
- ✅ Toutes les images pinées dans `tchalanet-infra/envs/common/compose.env`
- ✅ Toute image référencée dans `VERSIONS.md`

### Versions actuelles

| Service     | Image                                             |
| ----------- | ------------------------------------------------- |
| PostgreSQL  | `postgres:18.1`                                   |
| Redis       | `redis:8.4.0`                                     |
| Meilisearch | `getmeili/meilisearch:v1.11`                      |
| Keycloak    | `ghcr.io/bhebb/tchalanet-keycloak:stg-20251116-2` |
| Traefik     | `traefik:v3.6.5`                                  |
| Unleash     | `unleashorg/unleash-server:7.4.0`                 |
| API         | `ghcr.io/bhebb/tchalanet-api:${IMAGE_TAG}`        |
| Doppler CLI | `dopplerhq/cli:3.75.1`                            |

---

## Structure des fichiers Compose

```
tchalanet-infra/
├─ compose/
│  ├─ docker-compose-project.yml     ← base (réseaux, labels Traefik)
│  ├─ docker-compose-postgres.yml
│  ├─ docker-compose-redis.yml
│  ├─ docker-compose-keycloak.yml
│  ├─ docker-compose-traefik.yml
│  ├─ docker-compose-unleash.yml
│  ├─ docker-compose-api.yml
│  └─ docker-compose-doppler.yml     ← optionnel
└─ envs/
   ├─ common/compose.env             ← valeurs partagées (images, ports)
   ├─ dev/compose.env                ← overrides dev
   ├─ staging/compose.env            ← overrides staging
   └─ prod/compose.env               ← overrides prod
```

---

## Environnements

| Environnement | Usage                             |
| ------------- | --------------------------------- |
| `local`       | Dev sans services cloud           |
| `dev`         | Dev avec Compose complet          |
| `staging`     | Hetzner VPS — validation pré-prod |
| `prod`        | Hetzner — images GHCR pinées      |

**Règle** : même topologie partout — seule la configuration diffère, jamais le code.

---

## Réseaux Docker

- `edge-<env>` — edge service + intégrations externes
- `back-<env>` — backend API + infra (Postgres, Redis, Keycloak, etc.)

---

## Secrets

- ❌ Jamais de secrets dans le code, les specs, ni la documentation
- ❌ Fichier `.secrets` ne doit jamais être committé
- ✅ Secrets injectés via **Doppler** (prioritaire) ou variables d'environnement
- ✅ GitHub Secrets pour CI/CD

---

## Mise à jour d'une version

Toute mise à jour de version nécessite :

1. Mise à jour de `VERSIONS.md`
2. Mise à jour du wrapper/pin (`compose.env`, `mvnw`, `package.json#packageManager`)
3. Mise à jour des images Docker concernées
4. Note d'impact si production-facing
5. PR dédiée pour les upgrades majeurs

---

## CI/CD

- Build doit échouer sur : version drift, migrations manquantes, configs invalides
- Pas de déploiement prod manuel
- Logs structurés + request IDs traçables

### Workflows GitHub Actions

| Workflow                | Rôle                            |
| ----------------------- | ------------------------------- |
| `build-and-publish.yml` | Build API + Keycloak, push GHCR |
| `deploy-prod.yml`       | Deploy production Hetzner       |
| `deploy-stg.yml`        | Deploy staging (rsync + Docker) |
| `scan-security.yml`     | Snyk security scan              |
| `check-envs-pr.yml`     | Validation des envs en PR       |

---

## Commandes locales

```bash
cd tchalanet-infra

make up-all ENV=dev      # démarre tous les services
make down ENV=dev        # arrête tous les services
make logs ENV=dev        # logs de tous les services

# Démarrage rapide
open https://app.localtest.me
```
