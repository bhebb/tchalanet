# Publication et Déploiement des Images Docker

Ce guide explique comment builder, publier et déployer les images Docker personnalisées (API + Keycloak) pour Tchalanet.

## 📦 Architecture des Images

### Images Personnalisées

1. **tchalanet-api** : API Spring Boot

   - Source : `tchalanet-server/`
   - Build : Maven + Docker
   - Contenu : Application Java packagée

2. **tchalanet-keycloak** : Keycloak avec provider et thèmes personnalisés
   - Source : `tchalanet-infra/keycloak/`
   - Build : Multi-stage Dockerfile
   - Contenu :
     - Provider JAR compilé (`tchalanet-keycloak-provider/`)
     - Thèmes personnalisés (`themes/`)
     - Configuration de base (`realms/`)

### Images Tierces (non modifiées)

- Postgres, Redis, Traefik : utilisent les images officielles (voir `envs/common/compose.env`)

## 🚀 Publication des Images

### Option 1 : Script Local (Recommandé pour tests)

#### Prérequis

1. Docker installé et démarré
2. Authentification configurée (voir ci-dessous)

#### Authentification GHCR

```bash
# Créer un Personal Access Token sur GitHub
# https://github.com/settings/tokens
# Permissions nécessaires : write:packages, read:packages

export GHCR_TOKEN="ghp_YOUR_TOKEN_HERE"
echo "$GHCR_TOKEN" | docker login ghcr.io -u <votre-username> --password-stdin
```

#### Exécution

```bash
cd tchalanet-infra

# Avec tag auto (stg-<git-sha>)
./scripts/docker/publish-images.sh bhebb

# Avec tag spécifique

[//]: # (./scripts/docker/publish-images.sh bhebb stg-20251114)

# Avec registry différent (ex: Docker Hub)
./scripts/docker/publish-images.sh bhebb v1.0.0 docker.io
```

#### Que fait le script ?

1. ✅ Vérifie l'authentification Docker
2. 🏗️ Compile l'API (Maven)
3. 🏗️ Build les images Docker (API + Keycloak)
4. 📤 Push vers le registry
5. 📝 Met à jour `envs/common/compose.env`
6. 📋 Affiche les commandes pour le déploiement

### Option 2 : GitHub Actions (Recommandé pour prod)

#### Déclenchement Manuel

1. Aller sur https://github.com/<org>/tchalanet/actions
2. Sélectionner "Publish Images"
3. Cliquer "Run workflow"
4. Choisir :
   - Environment : `staging` ou `production`
   - Tag : laisser vide pour auto (`stg-<sha>` ou `prd-<sha>`)

#### Configuration des Secrets

Le workflow utilise `GITHUB_TOKEN` automatiquement. Pas de secret supplémentaire nécessaire si :

- Le repo appartient à l'organisation
- L'organisation autorise la publication de packages

Sinon, ajouter un secret `GHCR_TOKEN` dans les secrets du repo.

#### Permissions Requises

Le workflow nécessite :

```yaml
permissions:
  contents: read
  packages: write
```

## 📥 Utilisation des Images sur Staging

### Étape 1 : Vérifier la version disponible

Après publication, les images sont disponibles sur :

- https://github.com/<org>/tchalanet/pkgs/container/tchalanet-api
- https://github.com/<org>/tchalanet/pkgs/container/tchalanet-keycloak

### Étape 2 : Mettre à jour la configuration

Les images sont référencées via `envs/common/compose.env` :

```bash
# envs/common/compose.env
IMAGE_TAG=stg-abc123
API_IMAGE_BASE=ghcr.io/<org>/tchalanet-api
KEYCLOAK_IMAGE=ghcr.io/<org>/tchalanet-keycloak:stg-abc123
```

Ces valeurs sont automatiquement mises à jour par `publish-images.sh`.

### Étape 3 : Déployer sur le serveur

```bash
# Se connecter au serveur staging
ssh tchalanet_stg

# Naviguer vers le dossier infra
cd /opt/tchalanet-infra

# Pull la dernière configuration (si modifiée)
git pull origin main

# Télécharger les secrets depuis Doppler (si nécessaire)
make doppler-download ENV=staging

# Merger les variables d'environnement
make env-merge ENV=staging

# Pull les nouvelles images
cd compose
docker compose -f docker-compose-project.yml \
  -f docker-compose-postgres.yml \
  -f docker-compose-redis.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-traefik.yml \
  pull

# Redémarrer uniquement les services mis à jour
docker compose -f docker-compose-project.yml \
  -f docker-compose-postgres.yml \
  -f docker-compose-redis.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-traefik.yml \
  up -d api keycloak

# Vérifier les logs
docker compose logs -f api keycloak
```

### Redémarrage Ciblé (moins disruptif)

Si vous voulez redémarrer uniquement un service :

```bash
# API seulement
docker compose [...files...] pull api
docker compose [...files...] up -d api
docker compose logs -f api

# Keycloak seulement
docker compose [...files...] pull keycloak
docker compose [...files...] up -d keycloak
docker compose logs -f keycloak
```

## 🔍 Vérification Post-Déploiement

### Health Checks

```bash
# API
curl -fsS https://api.staging.tchalanet.com/actuator/health

# Keycloak
curl -fsS https://auth.staging.tchalanet.com/health/ready
curl -fsS https://auth.staging.tchalanet.com/realms/tchalanet/.well-known/openid-configuration
```

### Logs

```bash
# Derniers logs (100 lignes)
docker compose logs --tail=100 api keycloak

# Suivi en temps réel
docker compose logs -f api keycloak

# Logs avec timestamps
docker compose logs -f -t api keycloak
```

### État des Conteneurs

```bash
# Tous les conteneurs
docker compose ps

# Conteneurs spécifiques
docker compose ps api keycloak

# Détails d'un conteneur
docker inspect tchl-api-staging
docker inspect tchl-keycloak-staging
```

## 🔐 Sécurité et Bonnes Pratiques

### Tags Immutables

- ✅ **Recommandé** : `stg-abc123`, `prd-v1.2.3`, `stg-20251114`
- ❌ **À éviter en prod** : `latest`, `staging`, `dev`

### Visibilité des Packages

Par défaut, les packages GHCR sont privés. Pour les rendre publics :

1. Aller sur le package (ex: https://github.com/orgs/<org>/packages/container/tchalanet-api)
2. Package settings → Change visibility → Public

### Nettoyage des Anciennes Images

```bash
# Sur le serveur
docker system prune -a --filter "until=720h"  # Supprimer images > 30 jours

# Manuellement
docker images | grep tchalanet-api
docker rmi ghcr.io/<org>/tchalanet-api:old-tag
```

### Rollback

Si un problème survient après déploiement :

```bash
# Revenir à la version précédente
cd /opt/tchalanet-infra/compose

# Modifier envs/common/compose.env pour pointer vers l'ancien tag
# IMAGE_TAG=stg-previous-sha

# Ou utiliser un tag explicite
docker compose [...files...] pull
docker compose [...files...] up -d api keycloak
```

## 📊 Monitoring

### Vérifier la Version Déployée

```bash
# Depuis le serveur
docker inspect tchl-api-staging --format='{{.Config.Image}}'
docker inspect tchl-keycloak-staging --format='{{.Config.Image}}'

# Via l'API (si endpoint exposé)
curl https://api.staging.tchalanet.com/actuator/info
```

### Taille des Images

```bash
docker images | grep tchalanet
```

Tailles approximatives :

- tchalanet-api : ~200-300 MB
- tchalanet-keycloak : ~500-600 MB (avec provider + themes)

## 🐛 Troubleshooting

### Erreur : "Cannot perform an interactive login"

**Cause** : Pas authentifié au registry.

**Solution** :

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u <username> --password-stdin
```

### Erreur : "denied: permission_denied"

**Cause** : Token sans permissions ou org settings.

**Solutions** :

1. Vérifier les permissions du token (write:packages)
2. Vérifier les settings de l'org GitHub (Package creation permissions)

### Erreur : "manifest unknown"

**Cause** : Image pas encore pushée ou tag incorrect.

**Solution** :

1. Vérifier que l'image existe : https://github.com/<org>/tchalanet/pkgs
2. Vérifier le tag dans `envs/common/compose.env`

### Images non mises à jour après pull

**Cause** : Docker cache ou tag `latest` non actualisé.

**Solution** :

```bash
docker compose pull --no-cache api keycloak
docker compose up -d --force-recreate api keycloak
```

## 📚 Ressources

- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [GHCR Documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Script publish-images.sh](../scripts/docker/README.md)
- [Workflow GitHub Actions](../../.github/workflows/publish-images.yml)
