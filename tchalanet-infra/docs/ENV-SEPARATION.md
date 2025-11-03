le# Séparation des Variables d'Environnement - Tchalanet

**Date**: 14 novembre 2025

## 📋 Vue d'Ensemble

Ce document clarifie la séparation entre les variables **build-time** (versions, images) et **runtime** (configuration app, secrets).

## 🎯 Principe : Deux Mondes Distincts

```
┌─────────────────────────────────────────────────────────────────┐
│                    BUILD-TIME (compose.env)                     │
│  → Résolution d'images Docker Compose                          │
│  → Build args pour docker build                                │
│  → Versions, tags, registries                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    Docker Compose crée les conteneurs
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   RUNTIME (.env + .secrets)                     │
│  → Variables injectées DANS les conteneurs                     │
│  → Configuration applicative (DB, Redis, Keycloak...)          │
│  → Secrets (passwords, tokens, keys)                           │
└─────────────────────────────────────────────────────────────────┘
```

## 📁 Fichiers et Leur Rôle

### Build-Time (avant démarrage conteneurs)

| Fichier                   | Contenu                                         | Chargé par     | Usage                        |
| ------------------------- | ----------------------------------------------- | -------------- | ---------------------------- |
| `envs/common/compose.env` | Versions communes (Traefik, Postgres, Redis...) | Docker Compose | Résolution images            |
| `envs/<env>/compose.env`  | Versions spécifiques env (API, Keycloak custom) | Docker Compose | Résolution images + override |
| `compose/.env`            | **Auto-généré** depuis les `compose.env`        | Docker Compose | Résolution images localement |

**Variables typiques** :

```dotenv
# envs/common/compose.env
IMAGE_TAG=stg-20251114
API_IMAGE_BASE=ghcr.io/bhebb/tchalanet-api
KEYCLOAK_IMAGE=ghcr.io/bhebb/tchalanet-keycloak:stg-20251114
TRAEFIK_IMAGE=traefik:v3.5.4
POSTGRES_VERSION=18
REDIS_IMAGE=redis:8.2.3
```

### Runtime (dans les conteneurs)

| Fichier                  | Contenu                         | Chargé par                  | Usage            |
| ------------------------ | ------------------------------- | --------------------------- | ---------------- |
| `envs/<env>/.env`        | Variables runtime non-secrets   | Container (via `env_file:`) | Config app       |
| `envs/<env>/.secrets`    | Secrets (passwords, tokens)     | Container (via `env_file:`) | Authentification |
| `envs/<env>/.env.merged` | `.env` + compose vars fusionnés | Container (via `env_file:`) | Config complète  |

**Variables typiques** :

```dotenv
# envs/staging/.env
ENV=staging
API_HOST=api.stg.tchalanet.com
FLAGS_HOST=flags.stg.tchalanet.com

# envs/staging/.secrets
POSTGRES_PASSWORD=xxxxx
KC_DB_PASSWORD=xxxxx
REDIS_PASSWORD=xxxxx
GHCR_TOKEN=ghp_xxxxx
```

## 🔄 Flux de Déploiement

### 1. Publication des Images (Local)

```bash
# Script publish-images.sh met à jour compose.env avec les nouveaux tags
./scripts/docker/publish-images.sh bhebb stg-20251114

# Résultat : envs/common/compose.env mis à jour
IMAGE_TAG=stg-20251114
API_IMAGE_BASE=ghcr.io/bhebb/tchalanet-api
KEYCLOAK_IMAGE=ghcr.io/bhebb/tchalanet-keycloak:stg-20251114
```

### 2. Push vers Serveur Staging

```bash
./scripts/remote/push-infra-bkup.sh 91.98.194.162 staging
# → Copie tous les fichiers (compose/, envs/, scripts/) sur le serveur
```

### 3. Déploiement (Sur Serveur Staging)

```bash
ssh tchalanet_stg 'bash -s' < ./scripts/remote/deploy-staging.sh

# Le script fait :
# 1. Merge des envs runtime : make env-merge ENV=staging
#    → Crée envs/staging/.env.merged (runtime)
#
# 2. Génère compose/.env depuis compose.env (build-time)
#    → Extrait API_IMAGE_BASE, KEYCLOAK_IMAGE, IMAGE_TAG, etc.
#    → Docker Compose peut maintenant résoudre les images
#
# 3. Crée les réseaux Docker (edge-staging et back-staging)
#    → ./scripts/utils/setup-networks.sh staging
#    → Les réseaux sont créés s'ils n'existent pas
#
# 4. Pull des images
#    → docker compose -f docker-compose-project.yml ... pull api keycloak
#    → Utilise les variables de compose/.env
#    → docker-compose-project.yml définit les réseaux (external: true)
#
# 5. Redémarre les services
#    → docker compose -f docker-compose-project.yml ... up -d api keycloak
#    → Conteneurs reçoivent les vars de .env.merged + .secrets
```

## ⚠️ Pièges à Éviter

### ❌ Mettre des variables build-time dans `.env`

```dotenv
# ❌ INCORRECT - envs/staging/.env
API_IMAGE_BASE=ghcr.io/bhebb/tchalanet-api  # ← Build-time var !
```

**Pourquoi ?** : `.env` est pour runtime (conteneurs), pas pour Docker Compose.

**Bon emplacement** :

```dotenv
# ✅ CORRECT - envs/staging/compose.env
API_IMAGE_BASE=ghcr.io/bhebb/tchalanet-api
```

### ❌ Éditer manuellement `compose/.env`

Ce fichier est **auto-généré** par `deploy-staging.sh`. Toute modification sera écrasée.

**Bon workflow** :

1. Modifier `envs/<env>/compose.env`
2. Pusher sur le serveur
3. Relancer le script de déploiement (qui regénère `compose/.env`)

### ❌ Utiliser `env_file:` pour la résolution d'images

```yaml
# ❌ INCORRECT
services:
  api:
    image: ${API_IMAGE_BASE}:${IMAGE_TAG}
    env_file:
      - ../envs/staging/.env.merged # ← Trop tard, image déjà résolue !
```

**Pourquoi ?** : Docker Compose résout `image:` **AVANT** de charger `env_file:`.

**Solution** : Variables d'images dans `compose/.env` (chargé en premier).

## 🎓 Résumé

| Question                              | Réponse                                             |
| ------------------------------------- | --------------------------------------------------- |
| Où mettre les versions d'images ?     | `envs/<env>/compose.env`                            |
| Où mettre la config applicative ?     | `envs/<env>/.env`                                   |
| Où mettre les secrets ?               | `envs/<env>/.secrets`                               |
| Que fait `compose/.env` ?             | Auto-généré pour résolution d'images Docker Compose |
| Puis-je éditer `compose/.env` ?       | Non, il est regénéré à chaque déploiement           |
| Comment changer une version d'image ? | Éditer `envs/common/compose.env`, push, redéployer  |

## 📚 Références

- [Docker Compose environment precedence](https://docs.docker.com/compose/environment-variables/envvars-precedence/)
- [Docker Compose .env file](https://docs.docker.com/compose/environment-variables/set-environment-variables/#substitute-with-an-env-file)
- [FIX-COMPOSE-IMAGE-RESOLUTION.md](FIX-COMPOSE-IMAGE-RESOLUTION.md)
- [compose/README.md](../compose/README.md)
