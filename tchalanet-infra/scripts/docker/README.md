# Scripts Docker

## publish-images.sh

Script pour builder et publier les images Docker (API + Keycloak) vers un registry (GHCR ou Docker Hub).

### Prérequis

1. Docker installé et fonctionnel
2. Authentification au registry configurée
3. Maven installé (pour builder l'API)

### Authentification au registry

**Avant d'exécuter le script**, vous devez vous connecter au registry :

#### GitHub Container Registry (GHCR)

```bash
# Option 1: Avec un token GitHub (recommandé)
echo "$GHCR_TOKEN" | docker login ghcr.io -u <votre-username> --password-stdin

# Option 2: Avec GitHub CLI
gh auth token | docker login ghcr.io -u <votre-username> --password-stdin
```

Pour créer un token GitHub avec les permissions nécessaires :

1. Allez sur https://github.com/settings/tokens
2. Créez un Personal Access Token (classic)
3. Cochez les permissions : `write:packages`, `read:packages`, `delete:packages` (optionnel)
4. Exportez le token : `export GHCR_TOKEN="ghp_votre_token"`

#### Docker Hub

```bash
echo "$DOCKERHUB_TOKEN" | docker login -u <votre-username> --password-stdin
```

### Usage

```bash
# Syntaxe de base
./scripts/docker/publish-images.sh <org> [tag] [registry]

# Exemples
./scripts/docker/publish-images.sh tchalanet                    # tag auto (stg-<git-sha>), ghcr.io par défaut
./scripts/docker/publish-images.sh tchalanet stg-20251114       # tag spécifique
./scripts/docker/publish-images.sh tchalanet stg-v1.0 ghcr.io   # tag + registry explicite
./scripts/docker/publish-images.sh monorg latest docker.io     # Docker Hub
```

### Comportement

Le script :

1. ✅ Vérifie que Docker est installé
2. ✅ Vérifie l'authentification au registry (sans login interactif)
3. 🏗️ Build l'API (maven package + docker build)
4. 🏗️ Build Keycloak custom (provider + themes)
5. 📤 Push les images vers le registry
6. 📝 Met à jour `envs/common/compose.env` avec les nouveaux tags
7. 📋 Affiche les commandes pour déployer sur staging

### Fichiers mis à jour

- `envs/common/compose.env` : IMAGE_TAG, API_IMAGE_BASE, KEYCLOAK_IMAGE
- Backup créé : `envs/common/compose.env.bak`

### Déploiement sur staging

Après avoir exécuté le script localement, sur le serveur staging :

```bash
cd /opt/tchalanet-infra/compose

# Pull les nouvelles images
docker compose -f docker-compose-project.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-unleash.yml \
  pull

# Redémarrer les services concernés
docker compose -f docker-compose-project.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-unleash.yml \
  up -d api keycloak
```

### Automatisation (CI/CD)

Pour automatiser la publication via GitHub Actions, voir `.github/workflows/publish-images.yml`.

### Troubleshooting

#### Erreur : "Cannot perform an interactive login from a non TTY device"

**Cause** : Vous n'êtes pas connecté au registry.

**Solution** : Connectez-vous d'abord avec `docker login` (voir section Authentification).

#### Erreur : "denied: permission_denied"

**Cause** : Token sans permissions suffisantes.

**Solution** :

- Vérifiez que le token a les permissions `write:packages`
- Vérifiez que l'organisation autorise le push de packages
- Pour GHCR : allez dans Settings → Packages → Package creation → autorisez les membres

#### Erreur : Maven build failed

**Cause** : Problème de compilation de l'API.

**Solution** :

- Vérifiez que le code compile localement : `cd tchalanet-server && ./mvnw clean package`
- Vérifiez les dépendances Maven

#### Erreur : Keycloak provider build failed

**Cause** : Problème de compilation du provider Keycloak dans le Dockerfile.

**Solution** :

- Vérifiez le Dockerfile : `tchalanet-infra/keycloak/Dockerfile`
- Testez le build manuellement : `cd tchalanet-infra/keycloak && docker build .`

### Conseils

- **Tags** : Utilisez des tags immutables pour staging/prod (ex: `stg-abc123` ou `v1.2.3`)
- **Versioning** : Le script utilise par défaut `stg-<git-sha>` pour garantir la traçabilité
- **Sécurité** : Ne committez jamais les tokens dans le repo
- **Backup** : Le script crée automatiquement un backup de `compose.env` avant modification
