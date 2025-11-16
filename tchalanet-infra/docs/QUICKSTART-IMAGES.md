# 🚀 Guide Rapide : Publier les Images Docker

Ce guide vous permet de publier rapidement les images API et Keycloak vers GHCR.

## ⚡ Démarrage Rapide (5 minutes)

### 1. Créer un Token GitHub

1. Aller sur : https://github.com/settings/tokens
2. Cliquer "Generate new token" → "Generate new token (classic)"
3. Nom : `GHCR_TCHALANET`
4. Permissions : cocher `write:packages` et `read:packages`
5. Cliquer "Generate token"
6. **Copier le token** (commence par `ghp_...`)

### 2. Se Connecter à GHCR

```bash
# Remplacer par votre token et username GitHub
export GHCR_TOKEN="ghp_votre_token_ici"
echo "$GHCR_TOKEN" | docker login ghcr.io -u votre-username --password-stdin
```

**Résultat attendu** :

```
Login Succeeded
```

### 3. Builder et Publier

```bash
cd tchalanet-infra

# ⚠️ IMPORTANT: Utiliser votre username GitHub ou nom d'organisation
# Remplacer 'votre-username' par votre username GitHub (ex: bhebb)
# Les images seront publiées sous: ghcr.io/votre-username/tchalanet-api

# Option A : Tag automatique (stg-<git-sha>)
./scripts/docker/publish-images.sh votre-username

# Option B : Tag personnalisé
./scripts/docker/publish-images.sh votre-username stg-20251114

# Exemples concrets:
# ./scripts/docker/publish-images.sh bhebb stg-20251114
# ./scripts/docker/publish-images.sh mon-org stg-20251114
```

**Le script va** :

1. ✅ Vérifier l'authentification
2. 🏗️ Compiler l'API avec Maven
3. 🏗️ Builder l'image Docker API
4. 📤 Pusher vers `ghcr.io/votre-username/tchalanet-api:TAG`
5. 🏗️ Builder l'image Keycloak (avec provider + themes)
6. 📤 Pusher vers `ghcr.io/votre-username/tchalanet-keycloak:TAG`
7. 📝 Mettre à jour `envs/common/compose.env`

**Note** : Les images seront publiées sous **votre namespace GitHub** (username ou organisation), pas sous un namespace générique.

### 4. Vérifier les Images Publiées

Aller sur (remplacer `votre-username` par votre username GitHub) :

- https://github.com/votre-username/tchalanet/pkgs/container/tchalanet-api
- https://github.com/votre-username/tchalanet/pkgs/container/tchalanet-keycloak

Ou si c'est sous votre profil personnel :

- https://github.com/users/votre-username/packages/container/package/tchalanet-api
- https://github.com/users/votre-username/packages/container/package/tchalanet-keycloak

Vous devriez voir vos images avec le tag que vous avez choisi.

## 📦 Déployer sur Staging

Une fois les images publiées, sur le serveur staging :

```bash
# Se connecter
ssh tchalanet_stg

# Aller dans le dossier infra
cd /opt/tchalanet-infra

# Pull la dernière config (si nécessaire)
git pull origin main

# Télécharger les secrets Doppler
make doppler-download ENV=staging

# Merger les envs
make env-merge ENV=staging

# Pull les nouvelles images
cd compose
docker compose -f docker-compose-project.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-unleash.yml \
  pull

# Redémarrer les services
docker compose -f docker-compose-project.yml \
  -f docker-compose-keycloak.yml \
  -f docker-compose-api.yml \
  -f docker-compose-unleash.yml \
  up -d api keycloak

# Vérifier les logs
docker compose logs -f api keycloak
```

## 🔍 Vérifications

### Health Checks

```bash
# API
curl https://api.staging.tchalanet.com/actuator/health

# Keycloak
curl https://auth.staging.tchalanet.com/health/ready
```

### Version Déployée

```bash
docker inspect tchl-api-staging --format='{{.Config.Image}}'
docker inspect tchl-keycloak-staging --format='{{.Config.Image}}'
```

## 🐛 Problèmes Courants

### "denied: not_found: owner not found"

**Cause** : Le namespace (organisation ou user) n'existe pas ou vous n'avez pas les permissions pour y pusher.

**Solutions** :

1. **Utiliser votre propre username GitHub** :

   ```bash
   # Si votre username GitHub est 'bhebb'
   ./scripts/docker/publish-images.sh bhebb stg-20251114
   ```

2. **Vérifier le namespace de l'organisation** :

   - Si vous voulez pusher sous une organisation, assurez-vous :
     - Que l'organisation existe sur GitHub
     - Que vous êtes membre de cette organisation
     - Que l'organisation autorise les packages (Settings → Packages)

3. **Permissions du token** :
   - Le token doit avoir les permissions `write:packages` ET `read:packages`
   - Le token doit avoir accès à l'organisation (si applicable)

### "Cannot perform an interactive login"

**Cause** : Pas authentifié à GHCR

**Solution** :

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u <username> --password-stdin
```

### "tchalanet-server not found"

**Cause** : Script exécuté depuis le mauvais répertoire

**Solution** :

```bash
# Toujours exécuter depuis tchalanet-infra/
cd tchalanet-infra
./scripts/docker/publish-images.sh tchalanet
```

### "denied: permission_denied" lors du push

**Cause** : Token sans permissions ou organisation GitHub

**Solutions** :

1. Vérifier que le token a `write:packages`
2. Vérifier les settings de l'org GitHub :
   - Settings → Packages → Package creation
   - Autoriser les membres à créer des packages

### Images pas mises à jour après deploy

**Solution** :

```bash
docker compose pull --no-cache api keycloak
docker compose up -d --force-recreate api keycloak
```

## 📚 Documentation Complète

- [Guide Complet](IMAGES-DEPLOYMENT.md) - Déploiement détaillé
- [README Script](../scripts/docker/README.md) - Utilisation du script
- [Correctifs](../scripts/docker/FIXES.md) - Historique des corrections

## 💡 Conseils

### Tags Recommandés

- ✅ **Staging** : `stg-<sha>` (ex: `stg-a1b2c3d`)
- ✅ **Production** : `prd-v1.2.3` (semantic versioning)
- ❌ **À éviter** : `latest`, `dev` (non immutables)

### Sécurité

- ⚠️ Ne jamais committer le token GHCR dans le repo
- ✅ Utiliser des variables d'environnement : `export GHCR_TOKEN="..."`
- ✅ Rendre les packages privés par défaut
- ✅ Utiliser HTTPS pour tous les endpoints

### Performance

Le build complet prend environ :

- API : 2-3 minutes (Maven + Docker)
- Keycloak : 3-5 minutes (compilation provider + build image)
- **Total** : ~5-8 minutes

Pour accélérer :

- Utiliser le cache Maven : `~/.m2/repository`
- Utiliser le cache Docker : `docker builder prune` seulement si nécessaire
- Builder en parallèle (GitHub Actions fait ça automatiquement)

## 🎯 Workflow Complet Recommandé

### Développement Local

1. Faire vos modifications dans `tchalanet-server` ou `tchalanet-infra`
2. Tester localement : `nx serve web` + API locale
3. Commit et push sur une branche

### Publication Staging

1. Merger vers `main` (ou branche `staging`)
2. Publier les images :
   ```bash
   # Utiliser votre username GitHub
   ./scripts/docker/publish-images.sh votre-username
   ```
3. Déployer sur staging (voir commandes ci-dessus)
4. Tester : https://api.staging.tchalanet.com

### Promotion Production

1. Créer un tag git : `git tag v1.2.3`
2. Publier avec tag prod :
   ```bash
   # Utiliser votre username GitHub
   ./scripts/docker/publish-images.sh votre-username prd-v1.2.3
   ```
3. Déployer sur prod (même commandes, serveur prod)
4. Vérifier : https://api.tchalanet.com

## 🤖 Automatisation (GitHub Actions)

Pour automatiser via CI/CD, utiliser le workflow `.github/workflows/publish-images.yml` :

1. Aller sur https://github.com/tchalanet/tchalanet/actions
2. Sélectionner "Publish Images"
3. Cliquer "Run workflow"
4. Choisir `staging` ou `production`
5. Laisser le tag vide (automatique) ou spécifier un tag

Le workflow fait tout automatiquement (build + push + summary).
