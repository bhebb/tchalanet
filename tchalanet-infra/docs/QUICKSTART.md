# 🚀 DÉMARRAGE RAPIDE - Infrastructure Tchalanet

**Dernière mise à jour:** 5 novembre 2025

---

## ⚡ QUICK START (5 minutes)

```bash
cd tchalanet-infra

# 1. Setup initial (première fois seulement)
make local-setup-dev
make generate-meili-master-key ENV=dev
make realm-generate ENV=dev
make build-keycloak

# 2. Démarrer tous les services
make up-all ENV=dev

# 3. Vérifier que tout tourne
docker ps
```

**Accès aux services:**

- Keycloak: http://localhost:8082 (`super_admin` / `changeme`)
- Unleash: http://localhost:4242
- Postgres: localhost:5432 (`postgres` / `devpass`)
- Traefik: http://localhost:8080

---

## 📋 SERVICES DISPONIBLES

### Démarrent automatiquement ✅

- **Postgres** - Base de données (3 DB créées automatiquement)
- **Redis** - Cache
- **Keycloak** - Authentification (avec provider custom + 3 users)
- **Unleash** - Feature flags
- **Meilisearch** - Recherche
- **Traefik** - Reverse proxy

### Optionnels ⚙️

- **Unleash Edge** - Proxy feature flags (désactivé par défaut en dev)

---

## 🔧 COMMANDES UTILES

### Gestion de la stack

```bash
# Démarrer
make up-all ENV=dev

# Arrêter
make down-all ENV=dev

# Redémarrer un service
make restart-keycloak ENV=dev

# Voir les logs
make logs ENV=dev
docker logs tchl-keycloak-dev

# État des conteneurs
docker ps
```

### Configuration

```bash
# Vérifier les variables d'environnement
make check-envs ENV=dev

# Regénérer .env.merged
make env-merge ENV=dev

# Setup .env pour l'API Spring Boot
make setup-api-env ENV=dev
```

### Debugging

```bash
# Bases de données
docker exec -it tchl-postgres-dev psql -U postgres -c "\l"

# Tester Keycloak
curl http://localhost:8082/health

# Tester Unleash
curl http://localhost:4242/health
```

---

## 🎯 SERVICES OPTIONNELS

### Activer Unleash Edge

Si tu veux utiliser Unleash Edge (pour prod ou tests de performance) :

```bash
# 1. Modifier le Makefile (ligne COMPOSE_PROFILES pour dev)
COMPOSE_PROFILES := core,cache,flags,flags-edge,search

# 2. Redémarrer
make up-all ENV=dev
```

⚠️ **Note:** Unleash Edge nécessite des tokens configurés. Voir `envs/dev/.secrets` pour `UNLEASH_FRONTEND_TOKEN` et `UNLEASH_SERVER_TOKEN`.

---

## 🔐 Configurer Keycloak comme SSO pour Unleash (manuel - dev)

Si tu veux que Unleash utilise Keycloak pour l'authentification (SSO), voici une méthode simple et reproductible pour l'environnement local.

### 1) Créer un client Keycloak pour Unleash

- Ouvre Keycloak Admin UI (ex: http://localhost:8082/admin)
- Sélectionne le realm `tchalanet` (ou ton realm de staging/production)
- Clients → Create
  - Client ID: (if using OIDC for other services, create an appropriate Keycloak client) — Unleash OSS uses token-based access (see below)
  - Client Protocol: `openid-connect`
  - Root URL: `https://flags.localtest.me` (ou l'URL de ton env)
  - Save
- Dans l'onglet Settings:
  - Access Type: `confidential` (si tu utilises oauth2-proxy)
  - Standard Flow Enabled: ON
  - Valid Redirect URIs: `https://flags.localtest.me/oauth2/callback` (adapter au host)
  - Web Origins: `https://flags.localtest.me`
- Credentials → copy le `Secret` (on l'utilisera dans les envs)

### 2) Accéder à Unleash (protection par tokens)

Unleash OSS ne supporte pas SAML et, dans notre configuration sans proxy, nous protégeons l'accès via des tokens API. Deux cas :

- UI publique (dev): par défaut l'UI est accessible en local. Pour staging/prod, restreins l'accès via votre réseau / firewall.
- Accès machine/Edge (production): utilise des API tokens.

Pour générer et utiliser les tokens Edge / Upstream :

1. Ouvre Unleash UI (ex: http://localhost:4242)
2. Admin → API tokens → Create token (choisis `Admin` ou `Client` selon besoin)
3. Ajoute le token dans `envs/<env>/.secrets` :

```dotenv
UNLEASH_SERVER_TOKEN=<token-for-admin-bootstrap-or-edge>
UNLEASH_FRONTEND_TOKEN=<token-for-front-or-edge>
```

4. Relance l'Edge :

```bash
make up-all ENV=dev
```

L'Edge utilisera `UNLEASH_SERVER_TOKEN` pour communiquer avec le serveur Unleash (ou `UNLEASH_FRONTEND_TOKEN` pour la lecture côté front). L'UI conserve un accès en lecture/gestion selon les tokens et le firewall.

---

## 🐛 TROUBLESHOOTING

### Conteneur en erreur

```bash
# Voir les logs
docker logs <nom-conteneur>

# Redémarrer proprement
docker stop <nom-conteneur>
docker rm <nom-conteneur>
make up-all ENV=dev
```

### Variables manquantes

```bash
# Vérifier
cat envs/dev/.env
cat envs/dev/.secrets

# Regénérer
make env-prepare ENV=dev
```

### Bases de données manquantes

```bash
# Nettoyer complètement et relancer (le script d'init sera réexécuté)
docker volume rm tchl-postgres-data-dev pgdata-dev
make up-all ENV=dev
```

### Ports déjà utilisés

```bash
# Voir quel processus utilise le port
lsof -i :5432

# Tuer le processus
kill -9 <PID>
```

---

## 📚 DOCUMENTATION COMPLÈTE

Pour plus de détails, consulte :

- **DEPLOYMENT.md** - 🚀 Guide complet de déploiement staging/production
- **SUMMARY.md** - Résumé de toutes les corrections apportées
- **CONFIGURATION_FINALE.md** - Vue d'ensemble complète de l'infra
- **API_CONNEXION.md** - Connecter l'API Spring Boot
- **FIX_KEYCLOAK_DB.md** - Résolution erreurs DB
- **POSTGRES_SSL.md** - Configuration SSL pour prod
- **DEMARRAGE.md** - Guide complet avec troubleshooting

---

## ✅ CHECKLIST

Avant de démarrer, assure-toi que :

- [ ] Docker Desktop est lancé (`docker ps` fonctionne)
- [ ] Les réseaux existent (`docker network ls | grep edge-dev`)
- [ ] Les secrets existent (`cat envs/dev/.secrets`)
- [ ] L'image Keycloak est buildée (`docker images | grep tchl/keycloak`)

---

**Prochaine étape:** Connecter l'API Spring Boot  
→ Voir `docs/API_CONNEXION.md`

🚀 **L'infrastructure est prête !**
