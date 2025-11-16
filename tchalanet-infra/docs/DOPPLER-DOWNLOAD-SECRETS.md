# Guide : Télécharger les secrets Doppler dans l'infra

## 🎯 Objectif

Télécharger les secrets depuis Doppler vers le serveur staging pour démarrer l'infrastructure.

## 📋 Prérequis

- ✅ Secrets créés dans Doppler (config `stg`)
- ✅ Service Token Doppler créé
- ✅ Serveur staging accessible (91.98.194.162)
- ✅ Infrastructure pushée sur le serveur

## 🚀 Méthode 1 : Script automatique (Recommandé)

### Sur le serveur staging

```bash
# 1. Se connecter au serveur
ssh tchalanet_stg

# 2. Aller dans le dossier infra
cd /opt/tchalanet-infra

# 3. Exporter le token Doppler (à récupérer depuis Doppler dashboard)
export DOPPLER_TOKEN="dp.st.xxxxx..."

# 4. Télécharger les secrets avec le script
./scripts/doppler/download-doppler-secrets.sh staging "$DOPPLER_TOKEN"

# Le script va :
# - Télécharger les secrets depuis Doppler (config 'stg')
# - Les sauvegarder dans envs/staging/.secrets
# - Définir les permissions 600
# - Afficher le nombre de secrets téléchargés
```

## 🔧 Méthode 2 : Commande manuelle

### Option A : Via Docker (si Doppler CLI non installé)

```bash
# Sur le serveur staging
ssh tchalanet_stg
cd /opt/tchalanet-infra

# Exporter le token
export DOPPLER_TOKEN="dp.st.xxxxx..."

# Télécharger via Docker
docker run --rm \
  -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
  -v "$PWD":/work \
  -w /work \
  dopplerhq/cli:latest \
  sh -c 'doppler secrets download --project tchalanet --config stg --format env --no-file' \
  > envs/staging/.secrets

# Définir les permissions
chmod 600 envs/staging/.secrets

# Vérifier
ls -la envs/staging/.secrets
cat envs/staging/.secrets | head -5
```

### Option B : Via Doppler CLI local (si installé)

```bash
# Sur le serveur staging
ssh tchalanet_stg
cd /opt/tchalanet-infra

# Exporter le token
export DOPPLER_TOKEN="dp.st.xxxxx..."

# Télécharger
doppler secrets download \
  --token="$DOPPLER_TOKEN" \
  --project tchalanet \
  --config stg \
  --format env \
  --no-file \
  > envs/staging/.secrets

# Définir les permissions
chmod 600 envs/staging/.secrets
```

## ✅ Vérification

```bash
# Vérifier que le fichier existe
ls -la envs/staging/.secrets

# Devrait afficher :
# -rw------- 1 tch tch <size> <date> envs/staging/.secrets

# Vérifier le contenu (premières lignes)
head -10 envs/staging/.secrets

# Compter les secrets
grep -c "^[A-Z_]*=" envs/staging/.secrets
# Devrait afficher au moins 11
```

## 🔄 Après téléchargement : Merger et démarrer

```bash
# 1. Merger les variables d'environnement
make env-merge ENV=staging

# Vérifier que .env.merged a été créé
ls -la envs/staging/.env.merged

# 2. Démarrer l'infrastructure
make up-staging

# 3. Surveiller le démarrage
docker ps
docker compose logs -f
```

## 🆘 Dépannage

### Erreur : "Invalid token"

```bash
# Vérifier le format du token
echo $DOPPLER_TOKEN
# Doit commencer par "dp.st."

# Recréer le token si nécessaire
doppler configs tokens create staging-server \
  --project tchalanet \
  --config stg \
  --max-age 0
```

### Erreur : "Project not found"

```bash
# Vérifier que le projet existe
doppler projects

# Si absent, le créer
doppler projects create tchalanet
```

### Erreur : "Config not found"

```bash
# Lister les configs disponibles
doppler configs --project tchalanet

# La config 'stg' doit exister (créée automatiquement par Doppler)
```

### Fichier .secrets vide ou incomplet

```bash
# Vérifier dans Doppler UI
https://dashboard.doppler.com/workplace/tchalanet/stg

# Ou via CLI
doppler secrets --project tchalanet --config stg

# Compter les secrets
doppler secrets --project tchalanet --config stg --json | jq 'length'
```

### Permission denied lors de l'écriture

```bash
# Vérifier les permissions du dossier
ls -la envs/staging/

# Créer le dossier si nécessaire
mkdir -p envs/staging
chown -R tch:tch envs/staging
```

## 📦 Obtenir le Service Token Doppler

Si tu n'as pas encore le token :

### Via CLI

```bash
# Créer le token (en local, pas sur le serveur)
doppler configs tokens create staging-server \
  --project tchalanet \
  --config stg \
  --max-age 0

# Copier le token affiché (commence par dp.st.)
```

### Via Interface Web

1. Aller sur https://dashboard.doppler.com/workplace/tchalanet/stg
2. Onglet **"Access"** → **"Service Tokens"**
3. Cliquer **"Generate"**
4. Name: `staging-server-token`
5. Access: `Read`
6. **Copier le token** (dp.st.xxx...)

## 🔐 Sécurité du token

⚠️ **Important** : Le Service Token donne accès à tous les secrets de staging !

### Bonnes pratiques

```bash
# Ne JAMAIS committer le token
echo "DOPPLER_TOKEN=dp.st.xxx" >> .gitignore

# Utiliser des variables d'environnement éphémères
export DOPPLER_TOKEN="dp.st.xxx..."
./scripts/doppler/download-doppler-secrets.sh staging "$DOPPLER_TOKEN"
unset DOPPLER_TOKEN

# Ou via GitHub Secrets pour la CI/CD
# DOPPLER_TOKEN_STG dans les secrets du repo
```

### Rotation du token

```bash
# Révoquer l'ancien token dans Doppler UI
# Créer un nouveau token
doppler configs tokens create staging-server-new \
  --project tchalanet \
  --config stg \
  --max-age 0

# Mettre à jour dans GitHub Secrets
```

## 📋 Checklist complète

### Avant téléchargement

- [ ] Service Token Doppler créé
- [ ] Token copié et accessible
- [ ] Accès SSH au serveur staging
- [ ] Infrastructure pushée sur le serveur

### Téléchargement

- [ ] Token exporté (`export DOPPLER_TOKEN=...`)
- [ ] Script exécuté ou commande manuelle
- [ ] Fichier `envs/staging/.secrets` créé
- [ ] Permissions 600 vérifiées
- [ ] Secrets présents (au moins 11)

### Après téléchargement

- [ ] `make env-merge ENV=staging` exécuté
- [ ] `envs/staging/.env.merged` créé
- [ ] `make up-staging` lancé
- [ ] Tous les services démarrent correctement

## 🎯 Commandes rapides (copier-coller)

```bash
# === SUR LE SERVEUR STAGING ===

# 1. Se connecter
ssh tchalanet_stg

# 2. Aller dans l'infra
cd /opt/tchalanet-infra

# 3. Télécharger les secrets (remplacer TOKEN)
export DOPPLER_TOKEN="dp.st.YOUR_TOKEN_HERE"
./scripts/doppler/download-doppler-secrets.sh staging "$DOPPLER_TOKEN"

# 4. Merger et démarrer
make env-merge ENV=staging
make up-staging

# 5. Vérifier
docker ps
```

## 📚 Références

- [Doppler CLI Documentation](https://docs.doppler.com/docs/cli)
- [Service Tokens Guide](https://docs.doppler.com/docs/service-tokens)
- Guide complet : `docs/DOPPLER-SETUP-GUIDE.md`
- Script : `scripts/doppler/download-doppler-secrets.sh`

---

✅ **Tu es prêt à télécharger les secrets et démarrer l'infra !**
