# ⚡ Quick Reference - Déploiement Staging

**Pour les pressés. Guide détaillé:** [SERVER-STAGING-SETUP.md](./SERVER-STAGING-SETUP.md)

---

## 🎯 Règle d'Or

**DEV** = Build local  
**STAGING/PROD** = Pull images pré-buildées depuis GHCR

❌ **Ne JAMAIS** builder sur le serveur  
✅ **Toujours** pusher les images vers le registry d'abord

---

## 📦 Variables d'Environnement

### Où mettre quoi ?

| Fichier | Contenu | Usage |
|---------|---------|-------|
| **compose.env** | `IMAGE_TAG`, `KC_REALM`, `SPRING_PROFILES_ACTIVE` | Interpolation Docker Compose `${VAR}` |
| **.env** | URLs publiques, config app | Runtime (via `env_file:`) |
| **.secrets** | Passwords, tokens | Runtime sensible (jamais versionné) |

### Checklist compose.env

```bash
# envs/staging/compose.env doit contenir:
ENV=staging
IMAGE_TAG=stg-YYYYMMDD-X
API_IMAGE_BASE=ghcr.io/USER/tchalanet-api
KEYCLOAK_IMAGE=ghcr.io/USER/tchalanet-keycloak:TAG
KC_REALM=tchalanet-staging
KC_BOOTSTRAP_ADMIN_USERNAME=admin
SPRING_PROFILES_ACTIVE=staging
DOCKER_NETWORK_EDGE=edge-staging
DOCKER_NETWORK_BACK=back-staging
```

---

## 🚀 Commandes Essentielles

### 1️⃣ Première Installation Serveur

```bash
# 1. Créer serveur Hetzner
cd scripts/hcloud && ./03-create-server.sh

# 2. Builder & pusher images (LOCAL)
docker build -t ghcr.io/USER/tchalanet-api:TAG tchalanet-server/
docker push ghcr.io/USER/tchalanet-api:TAG

docker build -t ghcr.io/USER/tchalanet-keycloak:TAG keycloak/
docker push ghcr.io/USER/tchalanet-keycloak:TAG

# 3. Télécharger secrets
DOPPLER_TOKEN=dp.st.xxx make doppler-download-staging

# 4. Déployer infra
make push-staging

# 5. Copier .secrets manuellement (1ère fois seulement)
scp -i ~/.ssh/tchalanet_stg envs/staging/.secrets \
    tch@<IP>:/opt/tchalanet-infra/envs/staging/.secrets

# 6. SSH et démarrer
make ssh-staging
cd /opt/tchalanet-infra
make env-merge ENV=staging
make up-staging
```

### 2️⃣ Redéploiement (après changement code)

```bash
# Local
docker build -t ghcr.io/USER/tchalanet-api:NEW_TAG .
docker push ghcr.io/USER/tchalanet-api:NEW_TAG

# Mettre à jour envs/staging/compose.env avec NEW_TAG
vim envs/staging/compose.env

# Déployer
make deploy-staging
```

### 3️⃣ Mise à jour secrets uniquement

```bash
# Local
DOPPLER_TOKEN=xxx make doppler-download-staging
scp -i ~/.ssh/tchalanet_stg envs/staging/.secrets tch@<IP>:/opt/...

# Serveur
ssh ... 'cd /opt/tchalanet-infra && docker compose restart api keycloak'
```

---

## 🐛 Erreurs Fréquentes

### ❌ `unable to prepare context: path not found`

**Cause:** Tentative de build local sur le serveur  
**Fix:** ✅ Corrigé dans `up-seq.sh` - ne build plus en staging/prod

### ❌ `The "KC_REALM" variable is not set`

**Cause:** Variable manquante dans `compose.env`  
**Fix:** Ajouter dans `envs/staging/compose.env`

### ❌ `POSTGRES_PASSWORD=""` (vide)

**Cause:** `.secrets` absent ou non chargé  
**Fix:** 
```bash
# Vérifier présence
ls -la envs/staging/.secrets

# Télécharger depuis Doppler
DOPPLER_TOKEN=xxx make doppler-download-staging

# Copier sur serveur
scp -i ~/.ssh/tchalanet_stg envs/staging/.secrets tch@<IP>:/opt/...
```

### ❌ Services unhealthy ou crashent

**Debug:**
```bash
# Logs
docker logs -f tchl-api-staging

# Variables runtime
docker exec tchl-api-staging env | grep POSTGRES

# Santé
docker inspect tchl-keycloak-staging | jq '.[0].State.Health'
```

---

## 📊 Vérifications Post-Déploiement

```bash
# Conteneurs up?
docker ps --format "table {{.Names}}\t{{.Status}}"

# Tous healthy?
docker ps --filter "health=healthy"

# Logs OK?
docker logs --tail 20 tchl-api-staging

# Health endpoints
curl http://localhost:8080/actuator/health      # API
curl http://localhost:8080/health               # Keycloak
```

---

## 📚 Documentation Complète

- **Setup complet:** [SERVER-STAGING-SETUP.md](./SERVER-STAGING-SETUP.md)
- **Déploiement:** [DEPLOYMENT.md](./DEPLOYMENT.md)
- **Fix détaillé:** [FIX-STAGING-BUILD-ISSUE.md](./FIX-STAGING-BUILD-ISSUE.md)
- **Historique:** [../CHANGELOG.md](../CHANGELOG.md)

---

**Dernière MAJ:** 14 novembre 2025

