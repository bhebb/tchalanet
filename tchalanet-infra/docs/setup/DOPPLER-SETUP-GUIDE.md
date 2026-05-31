# Guide de configuration Doppler pour Tchalanet

Ce guide explique comment créer et configurer tous les secrets Doppler pour les environnements staging et production.

## 📋 Prérequis

1. **Compte Doppler** : Créer un compte sur [doppler.com](https://doppler.com)
2. **CLI Doppler** (optionnel mais recommandé) :

   ```bash
   # macOS
   brew install dopplerhq/cli/doppler

   # Linux
   curl -Ls https://cli.doppler.com/install.sh | sh
   ```

3. **Se connecter** :
   ```bash
   doppler login
   ```

## 🏗️ 1. Créer le projet Doppler

### Via Interface Web

1. Aller sur [dashboard.doppler.com](https://dashboard.doppler.com)
2. Cliquer sur **"Create Project"**
3. Nom du projet : `tchalanet`
4. Description : `Tchalanet Infrastructure Secrets`

### Via CLI

```bash
doppler projects create tchalanet --description "Tchalanet Infrastructure Secrets"
```

## 🌍 2. Créer les configurations (environments)

Créer 3 configurations dans le projet `tchalanet` :

### Via Interface Web

1. Dans le projet `tchalanet`, aller dans **Settings** → **Configs**
2. Créer les configs suivantes :
   - **dev** (Development)
   - **staging** (Staging)
   - **production** (Production)

### Via CLI

```bash
# Config dev (hérite de dev_default)
doppler configs create dev --project tchalanet

# Config staging
doppler configs create staging --project tchalanet

# Config production
doppler configs create production --project tchalanet
```

## 🔐 3. Liste complète des secrets à configurer

### Secrets essentiels (tous les environnements)

| Variable                      | Description                    | Exemple de génération     |
| ----------------------------- | ------------------------------ | ------------------------- |
| `POSTGRES_PASSWORD`           | Mot de passe PostgreSQL master | `openssl rand -base64 32` |
| `KC_DB_PASSWORD`              | Mot de passe DB Keycloak       | `openssl rand -base64 32` |
| `KC_BOOTSTRAP_ADMIN_PASSWORD` | Mot de passe admin Keycloak    | `openssl rand -base64 24` |
| `APP_DB_PASSWORD`             | Mot de passe DB application    | `openssl rand -base64 32` |
| `REDIS_PASSWORD`              | Mot de passe Redis             | `openssl rand -base64 32` |
| `UNLEASH_DB_PASSWORD`         | Mot de passe DB Unleash        | `openssl rand -base64 32` |
| `UNLEASH_PERSONAL_TOKEN`      | Token personnel Unleash        | Généré dans Unleash UI    |
| `UNLEASH_SERVER_TOKEN`        | Token serveur Unleash          | Généré dans Unleash UI    |
| `UNLEASH_FRONTEND_TOKEN`      | Token frontend Unleash         | Généré dans Unleash UI    |
| `MEILI_MASTER_KEY`            | Master key Meilisearch         | `openssl rand -base64 32` |

### Secrets optionnels (selon features activées)

| Variable            | Description           | Quand l'utiliser      |
| ------------------- | --------------------- | --------------------- |
| `GA_MEASUREMENT_ID` | Google Analytics ID   | Si analytics activé   |
| `SENTRY_DSN`        | Sentry error tracking | Si monitoring erreurs |
| `SMTP_PASSWORD`     | Mot de passe SMTP     | Si envoi emails       |

## 🔨 4. Générer et ajouter les secrets

### Option A : Via Interface Web (Recommandé pour la première fois)

1. Aller dans le projet `tchalanet`
2. Sélectionner la config `staging`
3. Cliquer sur **"Add Secret"**
4. Pour chaque secret :
   - Name : nom de la variable (ex: `POSTGRES_PASSWORD`)
   - Value : valeur générée (voir section suivante)
   - Cliquer sur **"Save"**

### Option B : Via CLI

```bash
# Se placer dans le bon projet/config
doppler setup --project tchalanet --config staging

# Ajouter les secrets un par un
doppler secrets set POSTGRES_PASSWORD="$(openssl rand -base64 32)"
doppler secrets set KC_DB_PASSWORD="$(openssl rand -base64 32)"
doppler secrets set KC_BOOTSTRAP_ADMIN_PASSWORD="$(openssl rand -base64 24)"
doppler secrets set APP_DB_PASSWORD="$(openssl rand -base64 32)"
doppler secrets set REDIS_PASSWORD="$(openssl rand -base64 32)"
doppler secrets set UNLEASH_DB_PASSWORD="$(openssl rand -base64 32)"
doppler secrets set MEILI_MASTER_KEY="$(openssl rand -base64 32)"
doppler secrets set GA_MEASUREMENT_ID=""
```

### Script de génération rapide

```bash
#!/bin/bash
# generate-secrets.sh - Génère tous les secrets nécessaires

echo "🔐 Génération des secrets Tchalanet"
echo ""
echo "# Copier/coller dans Doppler (staging)"
echo ""
echo "POSTGRES_PASSWORD=$(openssl rand -base64 32)"
echo "KC_DB_PASSWORD=$(openssl rand -base64 32)"
echo "KC_BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -base64 24)"
echo "APP_DB_PASSWORD=$(openssl rand -base64 32)"
echo "REDIS_PASSWORD=$(openssl rand -base64 32)"
echo "UNLEASH_DB_PASSWORD=$(openssl rand -base64 32)"
echo "MEILI_MASTER_KEY=$(openssl rand -base64 32)"
echo "GA_MEASUREMENT_ID="
echo ""
echo "⚠️  Tokens Unleash: à générer après démarrage d'Unleash"
echo "   - UNLEASH_PERSONAL_TOKEN"
echo "   - UNLEASH_SERVER_TOKEN"
echo "   - UNLEASH_FRONTEND_TOKEN"
```

Exécuter :

```bash
chmod +x scripts/doppler/generate-secrets.sh
./scripts/doppler/generate-secrets.sh
```

## 🎯 5. Créer les Service Tokens

Les Service Tokens permettent à la CI/CD et aux serveurs de télécharger les secrets.

### Pour Staging

1. **Via Interface Web** :

   - Projet `tchalanet` → Config `staging`
   - Onglet **"Access"** → **"Service Tokens"**
   - Cliquer **"Generate"**
   - Name : `staging-server-token`
   - Access : `Read` (ou `Read/Write` si besoin de mise à jour)
   - Environnements : `staging`
   - Copier le token généré (format : `dp.st.xxx...`)

2. **Via CLI** :
   ```bash
   doppler configs tokens create staging-server \
     --project tchalanet \
     --config staging \
     --max-age 0  # Pas d'expiration
   ```

### Pour Production

Répéter la même opération pour la config `production` :

```bash
doppler configs tokens create production-server \
  --project tchalanet \
  --config production \
  --max-age 0
```

## 📦 6. Ajouter les tokens dans GitHub Secrets

1. Aller sur GitHub : `Settings` → `Secrets and variables` → `Actions`
2. Ajouter les secrets suivants :

| Secret Name          | Value                           |
| -------------------- | ------------------------------- |
| `DOPPLER_TOKEN_STG`  | Token staging (dp.st.xxx...)    |
| `DOPPLER_TOKEN_PROD` | Token production (dp.st.xxx...) |

## 🚀 7. Tester le téléchargement des secrets

### En local (test)

```bash
# Exporter le token staging
export DOPPLER_TOKEN="dp.st.xxx..."

# Tester le téléchargement
doppler secrets download \
  --token="$DOPPLER_TOKEN" \
  --project tchalanet \
  --config staging \
  --format env
```

### Sur le serveur (staging)

```bash
# SSH vers le serveur
ssh tchalanet_stg

# Se placer dans l'infra
cd /opt/tchalanet-infra

# Exporter le token
export DOPPLER_TOKEN="dp.st.xxx..."

# Télécharger les secrets
docker run --rm \
  -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
  -v "$PWD":/work \
  -w /work \
  dopplerhq/cli:latest \
  sh -lc 'doppler secrets download --format env --project tchalanet --config staging > envs/staging/.secrets && chmod 600 envs/staging/.secrets'

# Vérifier
ls -la envs/staging/.secrets
cat envs/staging/.secrets | head -5
```

## ✅ 8. Vérification complète

### Checklist Doppler

- [ ] Projet `tchalanet` créé
- [ ] Configs `dev`, `staging`, `production` créées
- [ ] Tous les secrets essentiels ajoutés dans `staging`
- [ ] Tous les secrets essentiels ajoutés dans `production`
- [ ] Service Token `staging-server-token` créé
- [ ] Service Token `production-server-token` créé
- [ ] Tokens ajoutés dans GitHub Secrets
- [ ] Test de téléchargement réussi

### Commandes de vérification

```bash
# Lister les secrets (sans valeurs)
doppler secrets --project tchalanet --config staging

# Compter les secrets
doppler secrets --project tchalanet --config staging --json | jq 'length'
# Doit afficher au minimum 11 (secrets essentiels)

# Vérifier qu'aucun secret n'est vide
doppler secrets --project tchalanet --config staging --json | \
  jq -r 'to_entries[] | select(.value.computed == "") | .key'
# Ne doit rien afficher
```

## 🔄 9. Workflow de mise à jour des secrets

### Ajouter un nouveau secret

```bash
# Via CLI
doppler secrets set NEW_SECRET_NAME="nouvelle_valeur" \
  --project tchalanet \
  --config staging

# Sur le serveur, re-télécharger
ssh tchalanet_stg
cd /opt/tchalanet-infra
export DOPPLER_TOKEN="..."
# [commande docker run doppler secrets download...]
```

### Rotation d'un secret (ex: mot de passe DB)

1. **Mettre à jour dans Doppler** :

   ```bash
   doppler secrets set POSTGRES_PASSWORD="$(openssl rand -base64 32)" \
     --project tchalanet \
     --config staging
   ```

2. **Télécharger sur le serveur** :

   ```bash
   ssh tchalanet_stg 'cd /opt/tchalanet-infra && [doppler download]'
   ```

3. **Recréer les services affectés** :
   ```bash
   ssh tchalanet_stg
   cd /opt/tchalanet-infra
   make env-merge ENV=staging
   docker compose [...] up -d --force-recreate postgres
   ```

## 📚 10. Références

- [Doppler Documentation](https://docs.doppler.com/)
- [Doppler CLI Reference](https://docs.doppler.com/docs/cli)
- [Service Tokens Guide](https://docs.doppler.com/docs/service-tokens)
- [GitHub Actions Integration](https://docs.doppler.com/docs/github-actions)

## 🆘 Troubleshooting

### Erreur : "Invalid token"

- Vérifier que le token commence par `dp.st.`
- Vérifier que le token a les permissions `Read` sur la config
- Régénérer un nouveau token si nécessaire

### Secret non téléchargé

- Vérifier le nom exact dans Doppler (sensible à la casse)
- Vérifier que la config est bien `staging` ou `production`
- Tester avec `doppler secrets --project tchalanet --config staging`

### Valeur incorrecte

- Vérifier qu'il n'y a pas d'espaces avant/après la valeur
- Utiliser des quotes si la valeur contient des caractères spéciaux
- Re-télécharger les secrets sur le serveur après modification

---

✅ **Doppler est maintenant configuré et prêt à être utilisé !**
