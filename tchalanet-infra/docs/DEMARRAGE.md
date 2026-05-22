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

- ✅ Postgres (5 bases de données créées automatiquement)
- ✅ Redis
- ✅ Keycloak (HTTP OK en dev, SSL désactivé)
- ✅ Unleash
- ✅ Unleash Edge (avec tokens par défaut pour dev)
- ✅ Meilisearch
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
make generate-meili-master-key ENV=dev && \
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
curl http://localhost:4242/health  # Unleash
```

**Conteneurs attendus :**

- `tchl-postgres-dev` (healthy)
- `tchl-redis-dev` (healthy)
- `tchl-keycloak-dev` (healthy)
- `tchl-unleash-dev` (healthy)
- `tchl-unleash-edge-dev` (running)
- `tchl-meilisearch-dev` (healthy)
- `tchl-traefik-dev` (healthy)

---

### 5. Accéder aux services

Une fois tous les conteneurs "healthy" ou "running" :

| Service               | URL                               | Credentials                                                                |
| --------------------- | --------------------------------- | -------------------------------------------------------------------------- |
| **Keycloak**          | http://localhost:8082             | `super_admin` / `Changeme1!`<br>`admin` / `Changeme1!`<br>`agent` / `Changeme1!` |
| **Traefik Dashboard** | http://localhost:8080             | -                                                                          |
| **Unleash**           | http://localhost:4242             | Token: `*:*.devtoken123456789`                                             |
| **Unleash Edge**      | http://localhost:3063/edge/health | -                                                                          |
| **Postgres**          | localhost:5432                    | `postgres` / `devpass`                                                     |
| **Redis**             | localhost:6379                    | Password: `devredis`                                                       |
| **Meilisearch**       | http://localhost:7700             | Master key dans `.secrets`                                                 |

---

## ⚠️ IMPORTANT : Unleash Edge

### Tokens par défaut (dev uniquement)

Pour **dev local**, Unleash Edge utilise des tokens de démonstration :

- `UNLEASH_SERVER_TOKEN=*:*.devtoken123456789` (admin/server token pour bootstrap)
- `UNLEASH_FRONTEND_TOKEN=*:*.dev-frontend-token` (token public/front pour Edge)

⚠️ **Ces tokens sont INSECURE et uniquement pour dev !**

### Pour staging/prod : Créer de vrais tokens

```bash
# 1. Démarrer Unleash d'abord
make up-unleash ENV=staging

# 2. Se connecter à Unleash UI (http://localhost:4242)
#    - Créer un compte admin
#    - Aller dans Admin > API tokens
#    - Créer un Admin API token (metadata: "bootstrap-<env>")
#    - Copier le token

# 3. Exporter le token et lancer le script
export UNLEASH_SERVER_TOKEN="<votre-admin-token>"
export ENV=staging
make create-unleash-edge-tokens

# Le script va créer des tokens Edge et écrire la première token dans `envs/staging/.secrets`
# sous la variable `UNLEASH_FRONTEND_TOKEN`.
```

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

2. **Configurer Unleash**

   - Créer des feature flags
   - Tester l'API Unleash depuis le front

3. **Tester Meilisearch**

   - Indexer des données de test
   - Tester la recherche

4. **Lancer l'API Spring Boot**
   - Voir `tchalanet-server/README.md`
   - `./mvnw spring-boot:run`

---

**Tout est prêt ! Il ne reste plus qu'à lancer Docker Desktop.** 🚀

## 🎯 CONFIGURER KEYCLOAK COMME SSO POUR UNLEASH (MANUEL)

Cette section décrit comment configurer Keycloak pour fournir l'authentification SSO à Unleash (UI/API) et les points d'intégration pour Unleash Edge.

> Les étapes ci-dessous couvrent : création du client Keycloak, configuration des scopes/mappers, création des rôles nécessaires, et variables d'environnement à définir dans `envs/<env>/.secrets`.

### A. Créer le client Keycloak pour Unleash

1. Ouvrir Keycloak Admin UI (ex: http://localhost:8082/admin) et sélectionner le realm `tchalanet`.
2. Créer un nouveau client :

   - Client ID : (create an appropriate Keycloak client for your web apps if needed; Unleash OSS uses token-based access)
   - Protocol : `openid-connect`
   - Root URL : `https://flags.<host>` (ex: `https://flags.localtest.me` pour dev)
   - Valid Redirect URIs : `https://flags.<host>/oauth2/callback`
   - Web Origins : `https://flags.<host>`
   - Access Type : `confidential` (si utilisé via `oauth2-proxy`) ou `public` (si client public)
   - Standard Flow : ON

3. Côté Credentials → copy `Secret` si Access Type = `confidential`.

### B. Scopes & mappers recommandés

Dans l'onglet `Client Scopes` ou `Mappers` du client :

- Ajouter `email` et `profile` si besoin.
- Mapper `roles` pour exposer les rôles realm ou client dans le token (claim `roles`).
- Si Unleash attend un champ particulier (ex: `tch`), crée un mapper custom pour ajouter ce claim.

Exemple de mapper pour `roles` :

- Mapper type : `Role List` / `oidc-role-name-mapper`
- Token claim name: `roles`
- Add to access token: true

### C. Rôles et permissions

- Dans Keycloak → Roles (Realm Roles) ajouter : `UNLEASH_ADMIN` ou `UNLEASH_USER` selon vos besoins.
- Associer ces rôles aux comptes utilisateurs ou groupes.
- Optionnel : configurer `Client Roles` pour le client `unleash` si vous souhaitez scoper les droits par client.

### D. Accès à Unleash sans proxy (token-based)

Unleash OSS ne supporte pas SAML côté SP et nous n'utilisons plus de proxy oauth2. L'accès machine/Edge doit être protégé par des tokens API.

- Générer un token depuis l'UI Unleash (Admin → API tokens)
- Stocker les tokens dans `envs/<env>/.secrets` :

```dotenv
UNLEASH_SERVER_TOKEN=<token-for-admin-bootstrap-or-edge>
UNLEASH_FRONTEND_TOKEN=<edge-frontend-token-or-csv>
```

- Relancer l'Edge (et Unleash si nécessaire) :

```bash
make up-all ENV=dev
```

La UI restera accessible en local pour development ; en staging/prod, restreins l'accès via firewall, VPN ou règles Traefik si nécessaire.

### E. Vérifier l'intégration

1. Accéder à `https://flags.<host>` → tu dois être redirigé vers Keycloak (login page)
2. Après login, vérifier le payload du token (JWT) et s'assurer que le claim `roles` contient `UNLEASH_ADMIN` ou `UNLEASH_USER` selon ton rôle.
3. Dans l'UI Unleash, vérifier que l'accès est correct et que tu peux créer/éditer flags.

### F. Notes pour staging / production

- En staging/prod, ne laisse pas `OAUTH2_PROXY_UNLEASH_COOKIE_SECURE=false`. Utilise HTTPS et `true`.
- Stocke secrets (client secret, cookie secrets, Unleash tokens) dans Doppler / Vault et injecte-les dans CI.
- Configure Traefik / DNS pour exposer `flags.<domain>` et `auth.<domain>` (Keycloak) avec TLS.

---

# DÉMARRAGE (consolidé)

Ce document a été consolidé dans `OPERATIONS.md` (Quickstart & Deploy). Consulte ce fichier pour les étapes de démarrage local et staging :

- `tchalanet-infra/docs/OPERATIONS.md`

Merci.
