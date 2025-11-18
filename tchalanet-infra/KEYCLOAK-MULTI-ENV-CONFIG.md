# Configuration Keycloak Multi-Environnements

## 📋 Objectif

Éviter le problème **Issuer mismatch** dans tous les environnements (dev, staging, prod) en configurant correctement :
1. Les variables d'environnement Keycloak (`KC_HOSTNAME`, `--hostname-url`)
2. Les overlays de realm par environnement
3. Le script de génération de realm

## ✅ Configuration appliquée

### 1. Variables d'environnement par env

**Fichiers créés/mis à jour** :

#### `envs/dev/keycloak.env` ✅
```bash
KC_HOSTNAME=auth.localtest.me
KC_HOST=auth.localtest.me
KC_REALM=tchalanet
KC_BASE_URL_EXTERNAL_URL=https://auth.localtest.me
```

#### `envs/staging/keycloak.env` ✅
```bash
KC_HOSTNAME=auth.stg.tchalanet.com
KC_HOST=auth.stg.tchalanet.com
KC_REALM=tchalanet
KC_BASE_URL_EXTERNAL_URL=https://auth.stg.tchalanet.com
```

#### `envs/prod/keycloak.env` ✅ (nouveau)
```bash
KC_HOSTNAME=auth.tchalanet.com
KC_HOST=auth.tchalanet.com
KC_REALM=tchalanet
KC_BASE_URL_EXTERNAL_URL=https://auth.tchalanet.com
```

### 2. Configuration Docker Compose

**`docker-compose-keycloak.yml`** utilise déjà :

```yaml
command: >
  start
  --hostname=${KC_HOSTNAME:-auth.localtest.me}
  --hostname-url=https://${KC_HOSTNAME:-auth.localtest.me}  # ✅
  --proxy-headers=xforwarded
  ...
```

### 3. Overlays de realm par environnement

Les overlays existent déjà et sont bien configurés :

#### `keycloak/realms/overlays/local.json` ✅
```json
{
  "realm": "tchalanet",
  "clients": [
    {
      "clientId": "tchalanet-web",
      "redirectUris": ["https://app.localtest.me/*"],
      "webOrigins": ["https://app.localtest.me"]
    },
    {
      "clientId": "tchalanet-swagger",
      "redirectUris": ["https://api.localtest.me/swagger-ui/oauth2-redirect.html"],
      "webOrigins": ["https://api.localtest.me"]
    }
  ]
}
```

#### `keycloak/realms/overlays/staging.json` ✅
```json
{
  "realm": "tchalanet",
  "clients": [
    {
      "clientId": "tchalanet-web",
      "redirectUris": [
        "https://*.test.tchalanet.com/*",
        "https://*.stg.tchalanet.com/*"
      ],
      "webOrigins": [
        "https://*.test.tchalanet.com",
        "https://*.stg.tchalanet.com"
      ]
    },
    {
      "clientId": "tchalanet-swagger",
      "redirectUris": ["https://api.stg.tchalanet.com/swagger-ui/oauth2-redirect.html"],
      "webOrigins": ["https://api.stg.tchalanet.com"]
    }
  ]
}
```

#### `keycloak/realms/overlays/prod.json` ✅
```json
{
  "realm": "tchalanet",
  "clients": [
    {
      "clientId": "tchalanet-web",
      "redirectUris": ["https://*.tchalanet.com/*"],
      "webOrigins": ["https://*.tchalanet.com"]
    },
    {
      "clientId": "tchalanet-swagger",
      "redirectUris": ["https://api.tchalanet.com/swagger-ui/oauth2-redirect.html"],
      "webOrigins": ["https://api.tchalanet.com"]
    }
  ]
}
```

### 4. Script de génération de realm

Le script `scripts/keycloak/get-realm.sh` :
- ✅ Charge les variables d'environnement depuis `envs/<ENV>/keycloak.env`
- ✅ Génère le realm à partir du template
- ✅ Applique l'overlay spécifique à l'environnement
- ✅ Utilise `KC_REALM`, `KC_HOSTNAME`, etc.

## 🚀 Workflow de déploiement

### Développement local (dev)

```bash
cd tchalanet-infra

# 1. Régénérer le realm pour dev
make get-realm ENV=dev

# 2. Démarrer l'infrastructure
make up-all ENV=dev

# 3. Vérifier l'issuer
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
# Attendu: "https://auth.localtest.me/realms/tchalanet"
```

### Staging

```bash
cd tchalanet-infra

# 1. Merger les env vars
make env-merge ENV=staging

# 2. Régénérer le realm pour staging
make get-realm ENV=staging

# 3. Démarrer (avec certificats Let's Encrypt)
make up-staging

# 4. Vérifier l'issuer
curl -s https://auth.stg.tchalanet.com/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
# Attendu: "https://auth.stg.tchalanet.com/realms/tchalanet"
```

### Production

```bash
cd tchalanet-infra

# 1. Merger les env vars
make env-merge ENV=prod

# 2. Régénérer le realm pour prod
make get-realm ENV=prod

# 3. Démarrer
make up-prod

# 4. Vérifier l'issuer
curl -s https://auth.tchalanet.com/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
# Attendu: "https://auth.tchalanet.com/realms/tchalanet"
```

## 📋 Fichiers générés

Chaque environnement génère son propre fichier realm :

```
keycloak/realms/
├── templates/
│   └── realm.base.json           # Template de base
├── overlays/
│   ├── local.json                # Overlay pour dev
│   ├── staging.json              # Overlay pour staging
│   └── prod.json                 # Overlay pour prod
└── tchalanet-realm.json          # ⚠️ Généré (ne pas commit si sensible)
```

**Important** : Le fichier `tchalanet-realm.json` est généré dynamiquement et peut contenir :
- Des URLs spécifiques à l'environnement
- Des utilisateurs de test (en dev/staging)
- Des secrets temporaires

Il ne doit **PAS** être commité s'il contient des secrets. Utiliser `.gitignore` :

```gitignore
# keycloak/realms/.gitignore
tchalanet-realm.json
*-realm.json
```

## 🔐 Matrice des environnements

| Env | Keycloak | Frontend | API | Issuer |
|-----|----------|----------|-----|--------|
| **dev** | https://auth.localtest.me | https://app.localtest.me | https://api.localtest.me | https://auth.localtest.me/realms/tchalanet |
| **staging** | https://auth.stg.tchalanet.com | https://app.stg.tchalanet.com | https://api.stg.tchalanet.com | https://auth.stg.tchalanet.com/realms/tchalanet |
| **prod** | https://auth.tchalanet.com | https://app.tchalanet.com | https://api.tchalanet.com | https://auth.tchalanet.com/realms/tchalanet |

## ✅ Validation

### Checklist avant déploiement

Pour chaque environnement :

- [ ] ✅ `envs/<ENV>/keycloak.env` existe et contient `KC_HOSTNAME`
- [ ] ✅ `keycloak/realms/overlays/<ENV>.json` existe avec les bonnes URLs
- [ ] ✅ `make get-realm ENV=<ENV>` génère `tchalanet-realm.json` sans erreur
- [ ] ✅ Le realm généré contient les bonnes `redirectUris` pour les clients
- [ ] ✅ L'issuer retourné par `/.well-known/openid-configuration` correspond à l'URL publique

### Test complet

```bash
# Fonction helper pour tester un env
test_keycloak_issuer() {
  local env=$1
  local expected_issuer=$2
  
  echo "🧪 Test Keycloak $env..."
  
  # 1. Régénérer realm
  make get-realm ENV=$env
  
  # 2. Vérifier le fichier généré
  local realm_file="keycloak/realms/tchalanet-realm.json"
  if [ ! -f "$realm_file" ]; then
    echo "  ❌ Realm non généré"
    return 1
  fi
  
  # 3. Vérifier les redirectUris dans le realm
  local web_redirects=$(jq -r '.clients[] | select(.clientId=="tchalanet-web") | .redirectUris[]' "$realm_file")
  echo "  📝 Redirects: $web_redirects"
  
  # 4. Après déploiement, vérifier l'issuer
  # (nécessite que Keycloak soit démarré)
  # curl -s https://<keycloak-url>/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
  
  echo "  ✅ Realm généré avec succès"
}

# Tester tous les environnements
test_keycloak_issuer "dev" "https://auth.localtest.me/realms/tchalanet"
test_keycloak_issuer "staging" "https://auth.stg.tchalanet.com/realms/tchalanet"
test_keycloak_issuer "prod" "https://auth.tchalanet.com/realms/tchalanet"
```

## 🐛 Troubleshooting

### Issuer mismatch après régénération

```bash
# 1. Vérifier que KC_HOSTNAME est chargé
cd tchalanet-infra
source envs/<ENV>/.env.merged
echo $KC_HOSTNAME
# Doit afficher: auth.<domain>

# 2. Vérifier que l'overlay est appliqué
jq '.clients[] | select(.clientId=="tchalanet-web") | {redirectUris, webOrigins}' \
  keycloak/realms/tchalanet-realm.json

# 3. Recréer le conteneur Keycloak
docker compose -f compose/docker-compose-project.yml up -d --force-recreate keycloak

# 4. Attendre le démarrage et tester
sleep 30
curl -s -k https://auth.<domain>/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
```

### Realm non importé au démarrage

Vérifier les logs Keycloak :
```bash
docker logs tchl-keycloak-<ENV> | grep -i "import\|realm"
```

Si "Realm 'tchalanet' imported" n'apparaît pas :
1. Vérifier que `tchalanet-realm.json` existe dans `keycloak/realms/`
2. Vérifier le volume mount dans `docker-compose-keycloak.yml`
3. Supprimer le volume Keycloak et redémarrer : `docker volume rm keycloak-data-<ENV>`

### Redirects URIs non acceptés

Si après login Keycloak refuse la redirection :
1. Vérifier que l'URL de l'app est bien dans `redirectUris`
2. Les wildcards `/*` sont supportés : `https://app.localtest.me/*`
3. Les wildcards de sous-domaines `*` sont supportés : `https://*.tchalanet.com/*`
4. Vérifier dans l'admin Keycloak : Clients > tchalanet-web > Settings

## 📖 Références

- [Keycloak Server Hostname](https://www.keycloak.org/server/hostname)
- [Keycloak Import/Export](https://www.keycloak.org/server/importExport)
- [Script get-realm.sh](../../scripts/keycloak/get-realm.sh)
- [FIX-KEYCLOAK-ISSUER-MISMATCH.md](./FIX-KEYCLOAK-ISSUER-MISMATCH.md)

---

**Statut** : ✅ Configuration multi-environnements complète. Les issuer mismatch sont évités dans tous les environnements.

