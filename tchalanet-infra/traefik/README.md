# Configuration Traefik Multi-Environnement

## 📋 Vue d'ensemble

La configuration Traefik est maintenant **entièrement adaptable** aux environnements **local (dev)**, **staging** et **prod**.

### Structure des fichiers

```
traefik/
├── traefik.yml                    # Configuration statique globale
├── certs/
│   ├── local-cert.pem            # Certificat mkcert pour dev
│   └── local-key.pem
└── dynamic/
    ├── common/                    # Config partagée (middlewares, TLS options)
    │   ├── 01-tls.yaml
    │   └── 02-middlewares.yaml
    └── env/                       # Configurations par environnement
        ├── dev.yaml               # Local (*.localtest.me + certificat mkcert)
        ├── staging.yaml           # Staging (*.stg.tchalanet.com + Let's Encrypt)
        ├── prod.yaml              # Production (*.tchalanet.com + Let's Encrypt)
        └── active.yaml            # → Symlink créé automatiquement vers {ENV}.yaml
```

## 🚀 Utilisation

### Option 1: Script automatique (recommandé)

```bash
# Lancer Traefik en mode dev (par défaut)
./scripts/local/start-traefik.sh

# Lancer Traefik en staging
./scripts/local/start-traefik.sh staging

# Lancer Traefik en production
./scripts/local/start-traefik.sh prod
```

Le script :
- ✅ Charge automatiquement les variables d'environnement depuis `envs/{common,ENV}/compose.env`
- ✅ Valide que la configuration existe
- ✅ Démarre Traefik avec docker compose
- ✅ Affiche les informations de connexion

### Option 2: Manuellement

```bash
# 1. Définir l'environnement
export ENV=dev  # ou staging, prod

# 2. Charger les variables
set -a
source envs/common/compose.env
source envs/$ENV/compose.env
set +a

# 3. Démarrer Traefik
cd compose
docker compose -f docker-compose-project.yml -f docker-compose-traefik.yml up -d traefik
```

## 🔧 Comment ça marche

### 1. Sélection automatique de l'environnement

Le script `traefik-entrypoint.sh` s'exécute au démarrage du conteneur et :
- Lit la variable d'environnement `ENV` (défaut: `dev`)
- Crée un symlink `active.yaml` → `env/${ENV}.yaml`
- Configure `acme.json` avec les bonnes permissions

```bash
# Vérifier le symlink actif dans le conteneur
docker exec tchl-traefik-dev ls -la /etc/traefik/dynamic/
```

### 2. Configurations par environnement

| Environnement | Domaines | Certificats | Resolver |
|--------------|----------|-------------|----------|
| **dev** | `*.localtest.me` | mkcert (local-cert.pem) | - |
| **staging** | `*.stg.tchalanet.com` | Let's Encrypt | `le` |
| **prod** | `*.tchalanet.com` | Let's Encrypt | `le` |

### 3. Services exposés

Tous les environnements exposent :
- **Traefik Dashboard** : `traefik.{env}.domain`
- **Keycloak** : `auth.{env}.domain`
- **API** : `api.{env}.domain`
- **Unleash** (staging/prod) : `flags.{env}.domain`
- **Meilisearch** (staging/prod) : `search.{env}.domain`

## 🔐 Certificats SSL

### Développement local (dev)

1. **Installer mkcert** :
   ```bash
   # macOS
   brew install mkcert
   
   # Linux
   # Voir https://github.com/FiloSottile/mkcert#installation
   ```

2. **Générer et installer le certificat** :
   ```bash
   # Installer le CA local
   mkcert -install
   
   # Générer le certificat pour les domaines locaux
   cd traefik/certs
   mkcert -cert-file local-cert.pem -key-file local-key.pem \
     "*.localtest.me" \
     "auth.localtest.me" \
     "api.localtest.me" \
     "traefik.localtest.me"
   ```

3. **Vérifier** :
   ```bash
   ls -la traefik/certs/
   # Doit contenir: local-cert.pem, local-key.pem
   ```

### Staging / Production

Les certificats sont générés automatiquement par Let's Encrypt via le resolver `le` configuré dans Traefik.

Le fichier `acme.json` stocke les certificats (créé automatiquement avec permissions 600).

## 🧪 Tests et vérification

### Vérifier que Traefik charge la bonne config

```bash
# Voir les logs au démarrage
docker logs tchl-traefik-dev

# Doit afficher:
# → Setting up environment: dev
# ✓ Active configuration: env/dev.yaml
# → Starting Traefik...
```

### Vérifier les routers actifs

```bash
# Dashboard Traefik
open http://localhost:8080

# Ou via API
curl -s http://localhost:8080/api/http/routers | jq
```

### Tester les domaines locaux

```bash
# Keycloak
curl -k https://auth.localtest.me/health

# API
curl -k https://api.localtest.me/actuator/health

# Dashboard Traefik
open https://traefik.localtest.me
```

## 🐛 Troubleshooting

### Erreur: `net::ERR_CERT_AUTHORITY_INVALID`

**Cause** : Le certificat mkcert n'est pas installé dans le trousseau système.

**Solution** :
```bash
mkcert -install
# Redémarrer le navigateur
```

### Erreur: `active.yaml not found`

**Cause** : La variable `ENV` n'est pas passée au conteneur.

**Solution** :
```bash
# Vérifier que ENV est bien défini
docker inspect tchl-traefik-dev | jq '.[0].Config.Env'

# Ou utiliser le script start-traefik.sh
./scripts/local/start-traefik.sh dev
```

### Traefik ne charge pas les routers

**Cause** : Fichier de configuration invalide ou non monté.

**Solution** :
```bash
# Vérifier que les fichiers sont bien montés
docker exec tchl-traefik-dev ls -la /etc/traefik/dynamic/

# Vérifier la syntaxe YAML
cd traefik/dynamic/env
yamllint dev.yaml staging.yaml prod.yaml

# Voir les logs Traefik
docker logs -f tchl-traefik-dev
```

## 📝 Ajouter un nouveau domaine

### 1. Modifier le fichier d'environnement

```yaml
# Dans traefik/dynamic/env/dev.yaml (ou staging.yaml, prod.yaml)
http:
  routers:
    mon-nouveau-service:
      rule: Host(`monservice.localtest.me`)
      entryPoints: [websecure]
      service: mon-service-svc
      tls: {}
      middlewares: [secure-headers@file, gzip@file]

  services:
    mon-service-svc:
      loadBalancer:
        servers:
          - url: "http://mon-service:8080"
```

### 2. Redémarrer Traefik

```bash
docker compose -f docker-compose-project.yml -f docker-compose-traefik.yml restart traefik
```

Ou attendre le rechargement automatique (watch activé).

## 📚 Références

- [Documentation Traefik](https://doc.traefik.io/traefik/)
- [mkcert](https://github.com/FiloSottile/mkcert)
- [Let's Encrypt](https://letsencrypt.org/)

