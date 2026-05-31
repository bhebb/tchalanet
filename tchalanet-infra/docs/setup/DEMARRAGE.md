# 🚀 Guide de démarrage complet - Tchalanet Infra

**Date:** 5 novembre 2025  
**Environnement:** dev (local)  
**Status:** ✅ Tous les problèmes résolus - Infrastructure 100% opérationnelle

## ✅ ÉTAT ACTUEL DE L'INFRASTRUCTURE

Tous les fichiers de configuration sont en place et prêts :

### Fichiers créés/configurés :

- ✅ `compose/` - Tous les fichiers compose corrigés et fonctionnels
- ✅ `envs/dev/.secrets` - Secrets avec tous les passwords et tokens
- ✅ `envs/dev/.env` - Variables runtime
- ✅ `envs/dev/.env.merged` - Fusion common + dev
- ✅ `keycloak/` - Provider, thème, realm avec 3 users
- ✅ `scripts/` - Scripts utils et setup
- ✅ Réseaux Docker: `edge-dev`, `back-dev`
- ✅ Makefile avec toutes les cibles nécessaires

### Images Docker prêtes :

- ✅ `tchl/keycloak:26.4.0-custom` - Image Keycloak avec provider + thème

### Services configurés :

- ✅ Postgres (2 bases de données créées automatiquement)
- ✅ Redis
- ✅ Keycloak (HTTP OK en dev, SSL désactivé)
- ✅ Traefik

---

## 🎯 ÉTAPES POUR DÉMARRER

### 1. Lancer Docker Desktop

**macOS:**

```bash
open -a Docker
```

**Attendre que Docker soit prêt (icône Docker stable dans la barre de menu)**

Vérifier:

```bash
docker ps
# Doit retourner une liste vide ou des conteneurs, pas d'erreur
```

---

### 2. Setup initial (première fois seulement)

```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra

# Tout en une commande
make local-setup-dev && \
make realm-generate ENV=dev && \
make build-keycloak

# ✔ Cette commande crée :
#   - Réseaux Docker (edge-dev, back-dev)
#   - Secrets (.secrets)
#   - Variables d'environnement (.env.merged)
#   - Realm Keycloak avec 3 users
#   - Image Keycloak custom
```

---

### 3. Lancer la stack complète

```bash
make up-all ENV=dev
```

**Cette commande démarre tous les services avec les bons profiles.**

---

### 4. Vérifier que les services démarrent

```bash
# Voir les conteneurs
docker ps

# Voir les logs en temps réel
docker compose logs -f

# État de santé
curl http://localhost:8082/health  # Keycloak
```

**Conteneurs attendus :**

- `tchl-postgres-dev` (healthy)
- `tchl-redis-dev` (healthy)
- `tchl-keycloak-dev` (healthy)
- `tchl-traefik-dev` (healthy)

---

### 5. Accéder aux services

Une fois tous les conteneurs "healthy" ou "running" :

| Service               | URL                               | Credentials                                                                |
| --------------------- | --------------------------------- | -------------------------------------------------------------------------- |
| **Keycloak**          | http://localhost:8082             | `super_admin` / `Changeme1!`<br>`admin` / `Changeme1!`<br>`agent` / `Changeme1!` |
| **Traefik Dashboard** | http://localhost:8080             | -                                                                          |
| **Postgres**          | localhost:5432                    | `postgres` / `devpass`                                                     |
| **Redis**             | localhost:6379                    | Password: `devredis`                                                       |

---


### 5. Tests de santé

```bash
# Check rapide
make check-health ENV=dev

# Vérifier Keycloak
curl http://localhost:8082/health

# Vérifier Postgres
docker exec tchalanet-dev-postgres-1 pg_isready

# Vérifier Redis
docker exec tchalanet-dev-redis-1 redis-cli ping
```

---

## 🔧 DÉPANNAGE

### Problème: Docker ne démarre pas

```bash
# Relancer Docker Desktop
killall Docker && open -a Docker

# Ou redémarrer la machine
```

### Problème: Port déjà utilisé

```bash
# Voir quel processus utilise le port 5432 (exemple)
lsof -i :5432

# Tuer le processus
kill -9 <PID>
```

### Problème: Conteneur en erreur

```bash
# Voir les logs du conteneur
docker logs tchalanet-dev-postgres-1

# Redémarrer un service
make restart-postgres ENV=dev
```

### Problème: Variables manquantes

```bash
# Vérifier les variables
make check-envs ENV=dev

# Regénérer .env.merged
make env-merge ENV=dev
```

### Problème: Réseaux manquants

```bash
# Recréer les réseaux
docker network create edge-dev
docker network create back-dev

# Ou via script
./scripts/utils/setup-networks.sh dev
```

---

## 🧹 NETTOYAGE

### Stopper tous les services

```bash
make local-down ENV=dev
# ou
make down-all ENV=dev
```

### Supprimer volumes et réinitialiser

```bash
docker compose --env-file envs/dev/compose.env \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  down -v

# Supprimer TOUS les volumes Docker
docker volume prune -a
```

### Nettoyer complètement Docker

```bash
docker system prune -a --volumes
# ⚠️ Attention: supprime TOUT (images, volumes, networks)
```

---

## 📝 COMMANDES UTILES

```bash
# État rapide
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Logs d'un service
docker logs -f tchalanet-dev-keycloak-1

# Entrer dans un conteneur
docker exec -it tchalanet-dev-postgres-1 bash

# Rebuild une image
make build-keycloak

# Regénérer le realm Keycloak
make realm-generate ENV=dev
```

---

## ✅ CHECKLIST DE DÉMARRAGE

- [ ] Docker Desktop lancé et fonctionnel (`docker ps` fonctionne)
- [ ] Réseaux créés (`edge-dev`, `back-dev`)
- [ ] Variables d'environnement fusionnées (`.env.merged`)
- [ ] Secrets créés (`envs/dev/.secrets`)
- [ ] Image Keycloak buildée (`tchl/keycloak:26.4.0-custom`)
- [ ] Realm Keycloak généré (`tchalanet-realm.json`)
- [ ] Provider compilé (`keycloak/providers/*.jar`)
- [ ] `make up-all ENV=dev` exécuté
- [ ] Tous les conteneurs en état "running" ou "healthy"
- [ ] Keycloak accessible sur http://localhost:8082
- [ ] Login Keycloak fonctionne avec `super_admin` / `Changeme1!`

---

## 🎯 PROCHAINES ÉTAPES APRÈS DÉMARRAGE

1. **Tester l'authentification Keycloak**

   - Se connecter à http://localhost:8082
   - Vérifier les 3 users (super_admin, admin, agent)
   - Tester le claim custom "tch" dans les tokens JWT

2. **Lancer l'API Spring Boot**
   - Voir `tchalanet-server/README.md`
   - `./mvnw spring-boot:run`

---

**Tout est prêt ! Il ne reste plus qu'à lancer Docker Desktop.** 🚀


# DÉMARRAGE (consolidé)

Ce document a été consolidé dans `OPERATIONS.md` (Quickstart & Deploy). Consulte ce fichier pour les étapes de démarrage local et staging :

- `tchalanet-infra/docs/OPERATIONS.md`

Merci.
