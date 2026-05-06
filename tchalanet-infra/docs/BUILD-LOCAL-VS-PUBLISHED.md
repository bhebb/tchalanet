# Build Local vs Image Publiée - Guide

## 🎯 Contexte

En développement local, on a deux options pour démarrer les services :

1. **Images publiées** (ghcr.io) - rapide mais ne contient pas les changements locaux
2. **Build local** - rebuild avec Dockerfile local incluant tous les changements

## 📦 Services concernés

### API (Spring Boot)

- **Image publiée** : `ghcr.io/tchalanet/api:${IMAGE_TAG}`
- **Dockerfile local** : `tchalanet-server/Dockerfile`
- **Quand rebuilder** : Changements dans le code Java ou `logback-spring.xml`

### Keycloak

- **Image publiée** : `quay.io/keycloak/keycloak:26.4.0` (base) + customisation
- **Dockerfile local** : `tchalanet-infra/keycloak/Dockerfile`
- **Quand rebuilder** :
  - Changements dans les thèmes
  - Changements dans le provider custom
  - **Nouveau realm généré** (important !)

## 🚀 Commandes

### Mode 1 : Images publiées (par défaut)

```bash
cd tchalanet-infra

# Démarrer avec les images publiées
docker compose -f compose/docker-compose-project.yml up -d

# ⚠️ Problème : Le realm généré localement n'est PAS dans l'image publiée !
```

### Mode 2 : Build local (recommandé pour dev)

```bash
cd tchalanet-infra

# Démarrer avec build local
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build

# ✅ Avantages :
# - Inclut le realm généré dans keycloak/realms/tchalanat-realm.json
# - Inclut les changements de code Java (API)
# - Inclut les thèmes/providers Keycloak modifiés
```

### Mode 3 : Build sélectif (plus rapide)

```bash
cd tchalanet-infra

# Rebuilder uniquement Keycloak
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak

# Rebuilder uniquement l'API
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build api

# Rebuilder les deux
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build api keycloak
```

## 📋 Workflow complet pour Keycloak

### Changement de configuration Keycloak

```bash
cd tchalanet-infra

# 1. Régénérer le realm
make env-merge ENV=dev
make get-realm ENV=dev

# 2. Vérifier que le realm est généré
ls -lh keycloak/realms/tchalanet-realm.json

# 3. Rebuilder Keycloak avec le nouveau realm
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak

# 4. Vérifier les logs
docker logs -f tchl-keycloak-dev

# 5. Tester
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
```

## 🔍 Comprendre le Dockerfile Keycloak

```dockerfile
# Étape 1 : Builder le provider custom (JAR Maven)
FROM maven:3.9-eclipse-temurin-21-alpine AS provider-builder
WORKDIR /build
COPY tchalanet-keycloak-provider/ .
RUN mvn -q -DskipTests clean package

# Étape 2 : Image Keycloak finale
FROM quay.io/keycloak/keycloak:26.4.0

# Copier les thèmes
COPY --chown=keycloak:keycloak themes/ /opt/keycloak/themes/

# Copier le provider custom
COPY --from=provider-builder --chown=keycloak:keycloak /build/target/*.jar /opt/keycloak/providers/

# ⚠️ IMPORTANT : Copier le realm généré
COPY --chown=keycloak:keycloak realms/*-realm.json /opt/keycloak/data/import/

# Build Keycloak avec les extensions
RUN /opt/keycloak/bin/kc.sh build
```

**Points clés** :

- Le realm `tchalanet-realm.json` est copié dans l'image au build
- Si tu changes le realm, tu DOIS rebuilder l'image
- L'import se fait au démarrage via `--import-realm`

## ⚠️ Erreurs courantes

### Erreur 1 : Realm pas importé

**Symptôme** : Keycloak démarre mais le realm est vide ou ancien

**Cause** : Le realm a été régénéré APRÈS le build de l'image

**Solution** :

```bash
# 1. Régénérer le realm
make get-realm ENV=dev

# 2. REBUILDER l'image (important !)
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

### Erreur 2 : Issuer mismatch malgré les changements

**Symptôme** : L'erreur persiste après avoir régénéré le realm

**Cause** : L'image Keycloak n'a pas été rebuildée

**Solution** :

```bash
# Forcer le rebuild (sans cache)
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml build --no-cache keycloak

docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d keycloak
```

### Erreur 3 : Volume Keycloak corrompu

**Symptôme** : Keycloak démarre mais utilise des données anciennes

**Cause** : Le volume Docker contient l'ancien état

**Solution** :

```bash
# 1. Arrêter Keycloak
docker compose -f compose/docker-compose-project.yml stop keycloak

# 2. Supprimer le volume
docker volume rm keycloak-data

# 3. Rebuilder et redémarrer
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

## 🎯 Cas d'usage

### Je développe le provider Keycloak

```bash
# À chaque changement dans tchalanet-keycloak-provider/
cd tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

### Je change les thèmes Keycloak

```bash
# À chaque changement dans themes/
cd tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

### Je change la configuration du realm

```bash
# 1. Modifier overlays ou template
# 2. Régénérer
make get-realm ENV=dev
# 3. Rebuilder
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

### Je développe l'API Spring Boot

```bash
# 1. Modifier le code Java
cd tchalanet-server
./mvnw -DskipTests package

# 2. Rebuilder l'image
cd ../tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build api
```

## 📖 Commande Make simplifiée (à créer)

Tu pourrais ajouter au Makefile :

```makefile
# Makefile
rebuild-keycloak:
	@echo "→ Régénération du realm..."
	@$(MAKE) get-realm ENV=$(ENV)
	@echo "→ Rebuild de l'image Keycloak..."
	docker compose -f compose/docker-compose-project.yml \
	               -f compose/docker-compose.local-build.yml up -d --build keycloak
	@echo "✅ Keycloak rebuilé et redémarré"

rebuild-api:
	@echo "→ Build du JAR Spring Boot..."
	cd ../tchalanet-server && ./mvnw -DskipTests package
	@echo "→ Rebuild de l'image API..."
	docker compose -f compose/docker-compose-project.yml \
	               -f compose/docker-compose.local-build.yml up -d --build api
	@echo "✅ API rebuildée et redémarrée"

rebuild-all:
	@$(MAKE) rebuild-api
	@$(MAKE) rebuild-keycloak
```

Usage :

```bash
make rebuild-keycloak ENV=dev
make rebuild-api ENV=dev
make rebuild-all ENV=dev
```

## ✅ Checklist dev quotidien

### Démarrage journalier

```bash
cd tchalanet-infra

# 1. S'assurer que les services tournent
docker compose -f compose/docker-compose-project.yml ps

# 2. Démarrer l'app Angular
cd ..
npm run start:web

# 3. Ouvrir l'app
open https://app.localtest.me
```

### Après changement de code

#### Backend (API)

```bash
cd tchalanet-server
./mvnw -DskipTests package
cd ../tchalanet-infra
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build api
```

#### Keycloak (realm/thèmes/provider)

```bash
cd tchalanet-infra
make get-realm ENV=dev
docker compose -f compose/docker-compose-project.yml \
               -f compose/docker-compose.local-build.yml up -d --build keycloak
```

#### Frontend (Angular)

```bash
# Le hot reload fonctionne automatiquement
# Pas besoin de rebuild ni redémarrer
```

---

**Résumé** : Toujours utiliser `docker-compose.local-build.yml` en dev pour avoir les derniers changements !
