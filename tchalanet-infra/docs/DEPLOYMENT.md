# 🚀 Déploiement Tchalanet – Staging & Production

**Dernière mise à jour:** 14 novembre 2025

---

## 📋 Vue d'ensemble

Ce document décrit le flux **officiel** pour déployer l'infrastructure Tchalanet en **staging** et **production**, en utilisant le Makefile et les scripts existants.

### Objectifs

- ✅ Un seul chemin standard par environnement
- ✅ Basé sur les fichiers `envs/*`, `run-compose.sh` et les cibles `make`
- ✅ Gestion automatique des secrets via Doppler
- ✅ Déploiement reproductible et sécurisé

---

## 1. Organisation des fichiers d'environnement

Pour chaque environnement (`dev`, `staging`, `prod`) :

### 📁 Structure des fichiers

```
envs/<env>/
├── .env              # Variables runtime non sensibles
├── compose.env       # Variables pour Docker Compose (parsing YAML)
├── .secrets          # Secrets Doppler (NON versionné)
└── .env.merged       # Fichier généré (fusion sans secrets)
```

### 📄 Détails des fichiers

#### `envs/<env>/.env`

Variables runtime **non sensibles** :

- Configuration application (URLs, flags, etc.)
- Paramètres publics
- Configuration des features

**Exemple :**

```bash
APP_NAME=Tchalanet
APP_ENV=staging
API_URL=https://api.staging.tchalanet.com
FEATURE_ANALYTICS=true
```

#### `envs/<env>/compose.env`

Variables utilisées par **Docker Compose lors du parsing** du YAML :

- `ENV` (dev/staging/prod)
- `IMAGE_TAG` (version des images Docker)
- `API_IMAGE_BASE`, `KEYCLOAK_IMAGE`
- `DOCKER_NETWORK_EDGE`, `DOCKER_NETWORK_BACK`
- Variables d'interpolation (`${POSTGRES_PASSWORD}`, etc.)

**Exemple :**

```bash
ENV=staging
IMAGE_TAG=v1.2.3
API_IMAGE_BASE=registry.tchalanet.com/api
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_USER=postgres
POSTGRES_DB=tchalanet_staging
```

#### `envs/<env>/.secrets`

Secrets injectés via **Doppler** :

- Mots de passe base de données
- Tokens API
- Clés de chiffrement
- Credentials OAuth

⚠️ **Ce fichier n'est PAS versionné** (`.gitignore`)

**Généré par :**

```bash
make doppler-download-staging  # ou -prod
```

#### `envs/<env>/.env.merged` (généré)

Fichier runtime **fusionné** (sans secrets) créé par :

```bash
make env-merge ENV=staging
```

⚠️ **Les secrets ne sont PAS inclus** dans `.env.merged`.  
Ils sont chargés au runtime via :

1. `envs/<env>/.secrets` (dans les services avec `env_file:`)
2. Le fichier temporaire généré par `run-compose.sh` pour `--env-file`

---

## 2. Scripts principaux

### 2.1 Makefile (racine `tchalanet-infra/`)

#### Cibles importantes

| Commande                        | Description                              |
| ------------------------------- | ---------------------------------------- |
| `make env-merge ENV=staging`    | Génère `envs/staging/.env.merged`        |
| `make doppler-download-staging` | Télécharge les secrets avec Doppler      |
| `make doppler-download-prod`    | Télécharge les secrets (production)      |
| `make up-staging`               | Lance la stack complète (staging)        |
| `make up-prod`                  | Lance la stack complète (production)     |
| `make deploy-staging`           | Push + déploie sur le serveur staging    |
| `make deploy-prod`              | Push + déploie sur le serveur production |

#### Détail des cibles de déploiement

```makefile
# Staging
make deploy-staging
  → push-staging      # Push l'infra vers le serveur
    → ssh + rsync
  → env-merge         # Génère .env.merged
  → up-staging        # Lance tous les services
```

### 2.2 `scripts/utils/run-compose.sh`

**Wrapper standard** pour toutes les commandes `docker compose` :

1. ✅ Résout le binaire Docker (macOS/Linux)
2. ✅ Construit un fichier d'env **temporaire** (incluant `.secrets` et `compose.env`)
3. ✅ Ajoute `compose/docker-compose-project.yml` + fichiers spécifiques
4. ✅ Exécute `docker compose ... --env-file <temp>` avec `--project-name "tch-${ENV}"`

**Appelé par :**

- `scripts/utils/service-up.sh`
- `scripts/utils/run-compose-wrapper.sh`
- Indirectement par le `Makefile`

#### 🔧 Correction appliquée (14 nov 2025)

Le script a été corrigé pour **inclure `compose.env` dans le fichier merged** :

```bash
# Fusion (ordre de priorité croissant)
[[ -f "envs/common/.env" ]]       && cat "envs/common/.env"       >>"$MERGED"
[[ -f "envs/$ENV/.env" ]]         && cat "envs/$ENV/.env"         >>"$MERGED"
[[ -f "envs/$ENV/.secrets" ]]     && cat "envs/$ENV/.secrets"     >>"$MERGED"
[[ -f "envs/$ENV/compose.env" ]]  && cat "envs/$ENV/compose.env"  >>"$MERGED"  # ← AJOUTÉ

# Appel docker compose avec --env-file
cmd=( "$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$MERGED" )
```

**Résultat :** Les variables d'interpolation (`${POSTGRES_PASSWORD}`, `${IMAGE_TAG}`, etc.) sont maintenant **disponibles lors du parsing** du YAML par Docker Compose.

---

## 3. Flux de déploiement complet

### 3.1 Staging

```bash
# 1. Télécharger les secrets depuis Doppler
DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging

# 2. Vérifier les fichiers d'environnement
ls -la envs/staging/
# .env ✅
# compose.env ✅
# .secrets ✅ (fraîchement téléchargé)

# 3. Déployer (push + env-merge + up)
make deploy-staging
```

**Détail des étapes automatisées :**

1. `push-staging` → Rsync de l'infra vers `/opt/tchalanet-infra` sur le serveur
2. SSH → `make env-merge ENV=staging` (génère `.env.merged`)
3. SSH → `make up-staging` :
   - `render-traefik` (génère la config Traefik)
   - `certs-staging` (certificats auto-signés)
   - `acme-perms` (permissions Let's Encrypt)
   - `get-realm` (génère le realm Keycloak)
   - `networks` (crée les réseaux Docker)
   - `up-seq.sh staging` (lance les services dans l'ordre)

### 3.2 Production

```bash
# 1. Télécharger les secrets depuis Doppler
DOPPLER_TOKEN=dp.st.yyy make doppler-download-prod

# 2. Déployer
make deploy-prod
```

**Différences avec staging :**

- Certificats Let's Encrypt (prod) vs auto-signés (staging)
- Pas de `certs-staging` (Let's Encrypt géré par Traefik)
- Réseau isolé (`edge-prod`, `back-prod`)

---

## 4. Services déployés

### Stack complète (ordre de démarrage)

1. **Traefik** - Reverse proxy & TLS
2. **Postgres** - Base de données (3 DB : app, keycloak, unleash)
3. **Redis** - Cache
4. **Keycloak** - Authentification & SSO
5. **Unleash** - Feature flags
6. **Meilisearch** - Moteur de recherche
7. **API Spring Boot** - Backend applicatif
8. **Unleash Edge** (optionnel) - Proxy feature flags

### Vérification

```bash
# Connecté sur le serveur
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Logs
docker logs tchl-api-staging
docker logs tchl-keycloak-staging
```

---

## 5. Commandes utiles

### Gestion des secrets

```bash
# Télécharger les secrets Doppler
DOPPLER_TOKEN=dp.st.xxx make doppler-download ENV=staging

# Vérifier les secrets (ATTENTION : sensible)
cat envs/staging/.secrets | head -5
```

### SSH vers les serveurs

```bash
# Staging
make ssh-staging

# Production
make ssh-prod
```

### Push manuel (sans déploiement)

```bash
# Push l'infra (sans lancer les services)
make push-staging
make push-prod
```

### Recréer .env.merged

```bash
# Local
make env-merge ENV=staging

# Sur le serveur (via SSH)
make ssh-staging
cd /opt/tchalanet-infra
make env-merge ENV=staging
```

### Redémarrer un service

```bash
# Exemple : Keycloak
docker restart tchl-keycloak-staging

# Ou via compose
cd /opt/tchalanet-infra
ENV=staging ./scripts/utils/service-up.sh up keycloak staging
```

---

## 6. Troubleshooting

### ❌ Variables vides (`POSTGRES_PASSWORD=""`)

**Symptôme :**

```
WARNING: The "POSTGRES_PASSWORD" variable is not set. Defaulting to a blank string.
```

**Cause :** `compose.env` n'était pas inclus dans le fichier d'interpolation.

**Solution :** ✅ Corrigé dans `run-compose.sh` (voir section 2.2)

### ❌ Secrets non chargés

**Symptôme :** Services échouent avec erreurs d'auth.

**Solution :**

```bash
# Vérifier que .secrets existe
ls -la envs/staging/.secrets

# Re-télécharger
DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging
```

### ❌ Réseaux Docker manquants

**Symptôme :**

```
ERROR: Network edge-staging not found
```

**Solution :**

```bash
make networks ENV=staging
```

### ❌ Certificats expirés/manquants (staging)

**Solution :**

```bash
make certs-staging
make up-staging
```

---

## 7. Architecture de déploiement

### Localement (dev)

```
Makefile → run-compose.sh → docker compose --env-file $MERGED
            ↓
          MERGED (temp, interpolation only)
            ├─ envs/dev/compose.env   (IMAGE_TAG, POSTGRES_PASSWORD, etc.)
            └─ envs/dev/.secrets      (Doppler secrets)

          Runtime (via env_file: in YAML)
            └─ envs/dev/.env          (app config)
```

### Serveur distant (staging/prod)

```
[Local]
  make deploy-staging
    ↓
  push-infra.sh (rsync)
    ↓
[Serveur distant]
  /opt/tchalanet-infra/
    ↓
  make env-merge ENV=staging
    ↓
  make up-staging
    ↓
  run-compose.sh (avec --env-file)
    ↓
  docker compose --env-file <merged>
```

---

## 8. Checklist de déploiement

### Avant déploiement

- [ ] Secrets Doppler téléchargés (`make doppler-download-staging`)
- [ ] `.secrets` présent dans `envs/staging/`
- [ ] `compose.env` à jour (versions d'images, etc.)
- [ ] Tests locaux passés (`make up-all ENV=dev`)
- [ ] Images Docker buildées et pushées (si custom)

### Après déploiement

- [ ] Tous les conteneurs démarrés (`docker ps`)
- [ ] Logs sans erreurs critiques
- [ ] Health checks OK :
  - [ ] Traefik : `https://staging.tchalanet.com`
  - [ ] Keycloak : `https://auth.staging.tchalanet.com/health`
  - [ ] API : `https://api.staging.tchalanet.com/actuator/health`
- [ ] Base de données accessible
- [ ] Feature flags Unleash actifs

---

## 9. Rollback

En cas de problème critique :

```bash
# 1. SSH vers le serveur
make ssh-staging

# 2. Arrêter les services
cd /opt/tchalanet-infra
make down-all ENV=staging

# 3. Revenir à la version précédente (git)
git checkout <commit-hash-précédent>

# 4. Redéployer
make env-merge ENV=staging
make up-staging
```

---

## 10. Sécurité

### ⚠️ Bonnes pratiques

1. **Ne jamais commiter `.secrets`**

   - Vérifié dans `.gitignore`
   - Toujours télécharger via Doppler

2. **Rotation des secrets**

   - Changer les mots de passe tous les 90 jours
   - Mettre à jour dans Doppler
   - Re-télécharger et redéployer

3. **Accès SSH**

   - Utiliser des clés SSH dédiées (`~/.ssh/tchalanet_stg`, `~/.ssh/tchalanet_prod`)
   - Désactiver l'auth par mot de passe
   - Limiter les IPs autorisées (firewall)

4. **Logs sensibles**
   - Ne jamais logger les secrets
   - Masquer les mots de passe dans les logs applicatifs

---

## 📚 Références

- **Quickstart local :** `docs/QUICKSTART.md`
- **Configuration complète :** `docs/CONFIGURATION_FINALE.md`
- **API Spring Boot :** `docs/API_CONNEXION.md`
- **Doppler setup :** `docs/DOPPLER-SETUP-GUIDE.md`
- **Scripts infra :** `scripts/README.md`

---

## ✅ Résumé

**Flux standard pour déployer :**

```bash
# 1. Secrets
DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging

# 2. Déployer
make deploy-staging

# 3. Vérifier
make ssh-staging
docker ps
```

**Flux pour production :**

```bash
DOPPLER_TOKEN=dp.st.yyy make doppler-download-prod
make deploy-prod
```

🚀 **L'infrastructure est déployée et prête à l'emploi !**
