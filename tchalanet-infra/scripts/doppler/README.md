# Scripts Doppler - Guide d'utilisation

Ce dossier contient 3 scripts pour gérer les secrets Doppler.

## 📋 Vue d'ensemble

| Script                       | Objectif                               | Quand l'utiliser                   |
| ---------------------------- | -------------------------------------- | ---------------------------------- |
| `generate-secrets.sh`        | Génère de nouveaux secrets aléatoires  | Setup initial, rotation de secrets |
| `create-secrets-from-env.sh` | Upload un `.env` existant vers Doppler | Migration depuis fichiers locaux   |
| `setup-doppler.sh`           | Configuration interactive complète     | Setup initial du projet            |

## 🔧 1. generate-secrets.sh

**Objectif** : Génère de nouveaux secrets aléatoires pour un environnement.

**Usage** :

```bash
./scripts/doppler/generate-secrets.sh <staging|production|dev>
```

**Sortie** : Format `.env` avec secrets générés aléatoirement

```
POSTGRES_PASSWORD=xyz...
KC_DB_PASSWORD=abc...
...
```

**Cas d'usage** :

- ✅ Setup initial d'un nouvel environnement
- ✅ Rotation de tous les secrets
- ✅ Génération pour copier/coller dans Doppler UI

**Exemple** :

```bash
# Générer pour staging
./scripts/doppler/generate-secrets.sh staging

# Copier la sortie dans Doppler ou sauvegarder
./scripts/doppler/generate-secrets.sh staging > /tmp/staging-secrets.env
```

---

## 📤 2. create-secrets-from-env.sh

**Objectif** : Upload un fichier `.env` existant vers Doppler via CLI.

**Usage** :

```bash
./scripts/doppler/create-secrets-from-env.sh <env-file> \
  --project <project> \
  --config <config> \
  [--dry-run] [--all] [--include KEY1,KEY2] [--exclude KEY3]
```

**Options** :

- `--dry-run` : Affiche ce qui serait fait sans l'exécuter
- `--all` : Upload toutes les variables (pas seulement les secrets)
- `--include KEY1,KEY2` : Force l'upload de ces clés spécifiques
- `--exclude KEY3,KEY4` : Exclut ces clés

**Comportement par défaut** :

- Upload uniquement les clés contenant : `PASSWORD|PASS|TOKEN|KEY|SECRET|ADMIN|DSN|JWT`
- Ignore les autres variables

**Cas d'usage** :

- ✅ Migration depuis un fichier `.env` existant
- ✅ Synchronisation masse de secrets
- ✅ Rotation d'un fichier `.secrets` entier

**Exemples** :

```bash
# Dry-run pour voir ce qui serait uploadé
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging \
  --dry-run

# Upload réel
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging

# Upload tout (pas seulement les secrets)
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.env.merged \
  --project tchalanet \
  --config staging \
  --all

# Upload seulement certaines clés
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging \
  --include POSTGRES_PASSWORD,REDIS_PASSWORD

# Exclure certaines clés
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging \
  --exclude GA_MEASUREMENT_ID
```

---

## 🤖 3. setup-doppler.sh

**Objectif** : Configuration interactive complète de Doppler (projet + configs + secrets + tokens).

**Usage** :

```bash
./scripts/doppler/setup-doppler.sh
```

**Actions** :

1. Vérifie la CLI Doppler
2. Crée le projet `tchalanet`
3. Crée les configs `dev`, `staging`, `production`
4. Génère et ajoute les secrets (utilise `generate-secrets.sh` en interne)
5. Crée les Service Tokens
6. Affiche les tokens à ajouter dans GitHub

**Cas d'usage** :

- ✅ Setup initial complet du projet
- ✅ Configuration rapide pour nouvel environnement
- ✅ Workflow guidé pour débutants

**Mode d'emploi** :

```bash
# Installation Doppler CLI si nécessaire
brew install dopplerhq/cli/doppler

# Connexion
doppler login

# Lancer le setup interactif
./scripts/doppler/setup-doppler.sh

# Suivre les instructions à l'écran
```

---

## 🔄 Workflows recommandés

### Workflow 1 : Setup initial (nouvel environnement)

**Option A - Automatique** (recommandé) :

```bash
./scripts/doppler/setup-doppler.sh
```

**Option B - Manuel** :

```bash
# 1. Générer les secrets
./scripts/doppler/generate-secrets.sh staging > /tmp/secrets.env

# 2. Créer projet et config dans Doppler UI
#    → https://dashboard.doppler.com

# 3. Upload les secrets
./scripts/doppler/create-secrets-from-env.sh \
  /tmp/secrets.env \
  --project tchalanet \
  --config staging
```

---

### Workflow 2 : Migration depuis fichiers `.env` existants

Si tu as déjà des secrets dans `envs/staging/.secrets` :

```bash
# Dry-run pour vérifier
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging \
  --dry-run

# Upload réel
./scripts/doppler/create-secrets-from-env.sh \
  envs/staging/.secrets \
  --project tchalanet \
  --config staging
```

---

### Workflow 3 : Rotation d'un secret spécifique

```bash
# Générer un nouveau secret
NEW_PASSWORD=$(openssl rand -base64 32)

# Mettre à jour dans Doppler
doppler secrets set POSTGRES_PASSWORD="$NEW_PASSWORD" \
  --project tchalanet \
  --config staging

# Ou via fichier temporaire
echo "POSTGRES_PASSWORD=$NEW_PASSWORD" > /tmp/new-pass.env
./scripts/doppler/create-secrets-from-env.sh \
  /tmp/new-pass.env \
  --project tchalanet \
  --config staging
rm /tmp/new-pass.env
```

---

### Workflow 4 : Rotation de tous les secrets

```bash
# Générer nouveaux secrets
./scripts/doppler/generate-secrets.sh staging > /tmp/new-secrets.env

# Upload dans Doppler
./scripts/doppler/create-secrets-from-env.sh \
  /tmp/new-secrets.env \
  --project tchalanet \
  --config staging

# Nettoyer
rm /tmp/new-secrets.env
```

---

## 🎯 Résumé des différences

### generate-secrets.sh vs create-secrets-from-env.sh

| Critère                        | generate-secrets.sh        | create-secrets-from-env.sh |
| ------------------------------ | -------------------------- | -------------------------- |
| **Génère des secrets**         | ✅ Oui (aléatoires)        | ❌ Non (lit un fichier)    |
| **Upload vers Doppler**        | ❌ Non (affiche seulement) | ✅ Oui (via CLI)           |
| **Besoin d'un fichier source** | ❌ Non                     | ✅ Oui (.env)              |
| **Besoin de Doppler CLI**      | ❌ Non                     | ✅ Oui                     |
| **Output**                     | stdout (format .env)       | Doppler (via API)          |

### Quand utiliser lequel ?

**`generate-secrets.sh`** :

- Tu veux **créer** de nouveaux secrets
- Tu n'as pas encore de fichier `.secrets`
- Tu veux copier/coller dans Doppler UI manuellement

**`create-secrets-from-env.sh`** :

- Tu as **déjà** un fichier `.env` ou `.secrets`
- Tu veux **automatiser** l'upload vers Doppler
- Tu veux synchroniser en masse

**`setup-doppler.sh`** :

- Tu veux un **setup complet** guidé
- Tu débutes avec Doppler
- Tu veux créer projet + configs + secrets d'un coup

---

## 📚 Exemples pratiques

### Exemple complet : Setup staging from scratch

```bash
# 1. Installer et se connecter
brew install dopplerhq/cli/doppler
doppler login

# 2. Setup automatique complet
./scripts/doppler/setup-doppler.sh
# → Crée projet, configs, génère et upload secrets, crée tokens

# 3. Récupérer les tokens affichés et les ajouter dans GitHub Secrets
```

### Exemple complet : Migrer secrets existants

```bash
# Tu as déjà envs/dev/.secrets avec des secrets
# Tu veux les mettre dans Doppler

# 1. Vérifier ce qui serait uploadé
./scripts/doppler/create-secrets-from-env.sh \
  envs/dev/.secrets \
  --project tchalanet \
  --config dev \
  --dry-run

# 2. Upload
./scripts/doppler/create-secrets-from-env.sh \
  envs/dev/.secrets \
  --project tchalanet \
  --config dev

# 3. Vérifier
doppler secrets --project tchalanet --config dev
```

---

## 🆘 Troubleshooting

### Erreur : "doppler CLI not found"

```bash
# Installer
brew install dopplerhq/cli/doppler  # macOS
curl -Ls https://cli.doppler.com/install.sh | sh  # Linux
```

### Erreur : "Not logged in"

```bash
doppler login
```

### Secret non uploadé par create-secrets-from-env.sh

Par défaut, seules les clés contenant des mots-clés secrets sont uploadées.

Solutions :

```bash
# Forcer l'upload d'une clé spécifique
--include MA_CLE

# Uploader toutes les clés
--all
```

---

✅ **Les 3 scripts sont complémentaires et ont chacun leur utilité !**
