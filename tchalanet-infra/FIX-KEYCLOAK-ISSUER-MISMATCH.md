# Fix: Keycloak Issuer Mismatch

## 🔴 Problème

```
[ERROR] Issuer mismatch. Well known issuer http://localhost:8080/realms/tchalanet 
does not match configured well known url https://auth.localtest.me/realms/tchalanet
```

## 🔍 Cause

Keycloak retournait `http://localhost:8080/realms/tchalanet` comme issuer dans sa configuration OpenID au lieu de `https://auth.localtest.me/realms/tchalanet`.

### Pourquoi ?

Keycloak utilise par défaut l'URL de la requête entrante pour construire l'issuer. Comme il reçoit les requêtes depuis Traefik sur `http://keycloak:8080` (réseau Docker interne), il retourne cette URL au lieu de l'URL publique.

## ✅ Solution appliquée

### 1. Configuration Keycloak - `docker-compose-keycloak.yml`

**Ajout de `--hostname-url`** :

```yaml
command: >
  start
  --http-enabled=true
  --hostname-strict=false
  --http-port=8080
  --http-management-enabled=true
  --http-management-port=9000
  --hostname=${KC_HOSTNAME:-auth.localtest.me}
  --hostname-url=https://${KC_HOSTNAME:-auth.localtest.me}  # ✅ AJOUTÉ
  --proxy-headers=xforwarded
  --import-realm
  --cache=local
  ${KC_EXTRA_ARGS}
```

### 2. Configuration Realm - `tchalanet-realm.json`

**Client `tchalanet-web`** - Ajout des URLs Traefik :

```json
{
  "clientId": "tchalanet-web",
  "redirectUris": [
    "http://localhost:4200/*",
    "https://app.localtest.me/*",           // ✅ AJOUTÉ
    "https://app.stg.tchalanet.com/*",      // ✅ AJOUTÉ
    "https://app.tchalanet.com/*"           // ✅ AJOUTÉ
  ],
  "webOrigins": [
    "http://localhost:4200",
    "https://app.localtest.me",             // ✅ AJOUTÉ
    "https://app.stg.tchalanet.com",        // ✅ AJOUTÉ
    "https://app.tchalanet.com"             // ✅ AJOUTÉ
  ]
}
```

**Client `tchalanet-swagger`** - Ajout des URLs Traefik :

```json
{
  "clientId": "tchalanet-swagger",
  "redirectUris": [
    "http://localhost:8081/swagger-ui/oauth2-redirect.html",
    "https://api.localtest.me/swagger-ui/oauth2-redirect.html",      // ✅ AJOUTÉ
    "https://api.stg.tchalanet.com/swagger-ui/oauth2-redirect.html", // ✅ AJOUTÉ
    "https://api.tchalanet.com/swagger-ui/oauth2-redirect.html"      // ✅ AJOUTÉ
  ],
  "webOrigins": [
    "http://localhost:8081",
    "https://api.localtest.me",              // ✅ AJOUTÉ
    "https://api.stg.tchalanet.com",         // ✅ AJOUTÉ
    "https://api.tchalanet.com"              // ✅ AJOUTÉ
  ]
}
```

## 🚀 Application des changements

```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra

# Redémarrer Keycloak avec la nouvelle configuration
docker compose -f compose/docker-compose-project.yml up -d keycloak

# Attendre que Keycloak soit healthy
docker logs -f tchl-keycloak-dev

# Vérifier l'issuer
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
# Attendu: "https://auth.localtest.me/realms/tchalanet"
```

## ✅ Validation

### 1. Vérifier l'issuer Keycloak

```bash
curl -s -k https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq '.issuer'
```

**Attendu** :
```json
"https://auth.localtest.me/realms/tchalanet"
```

### 2. Vérifier le frontend Angular

Ouvrir https://app.localtest.me dans le navigateur et vérifier la console :
- ✅ Plus d'erreur "Issuer mismatch"
- ✅ La découverte OpenID fonctionne
- ✅ Le bouton de login redirige vers Keycloak

### 3. Tester le flow OAuth complet

1. Ouvrir https://app.localtest.me
2. Cliquer sur "Se connecter"
3. Redirection vers https://auth.localtest.me
4. Login avec super_admin / changeme
5. Redirection vers https://app.localtest.me/auth/callback
6. ✅ Authentification réussie

## 📋 Architecture

```
Browser
    ↓
https://app.localtest.me (Angular App)
    ↓ (OAuth discovery)
https://auth.localtest.me/.well-known/openid-configuration
    ↓
Traefik (tchl-traefik-dev)
    ↓
Keycloak (tchl-keycloak-dev:8080)
    ↓ (retourne)
{
  "issuer": "https://auth.localtest.me/realms/tchalanet",  ✅
  "authorization_endpoint": "https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/auth",
  "token_endpoint": "https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token"
}
```

## 🔐 Environnements configurés

| Env | Frontend | Keycloak | Issuer |
|-----|----------|----------|--------|
| **Local** | https://app.localtest.me | https://auth.localtest.me | ✅ Configuré |
| **Staging** | https://app.stg.tchalanet.com | https://auth.stg.tchalanet.com | ✅ Configuré |
| **Production** | https://app.tchalanet.com | https://auth.tchalanet.com | ✅ Configuré |

## 📝 Notes importantes

### Pourquoi `--hostname-url` ?

Sans cette option, Keycloak utilise l'URL de la requête HTTP entrante (depuis le réseau Docker interne) pour construire l'issuer. Avec `--hostname-url=https://auth.localtest.me`, on force Keycloak à utiliser l'URL publique.

### Options Keycloak utilisées

- `--hostname` : Le nom d'hôte (sans protocole)
- `--hostname-url` : L'URL complète (avec https://)
- `--hostname-strict=false` : Permet les requêtes depuis différents domaines
- `--proxy-headers=xforwarded` : Keycloak lit les headers `X-Forwarded-*` de Traefik

### Redirects URIs

Les patterns `/*` dans redirectUris permettent :
- `https://app.localtest.me/auth/callback` ✅
- `https://app.localtest.me/` ✅
- `https://app.localtest.me/any/path` ✅

### CORS / Web Origins

Les `webOrigins` autorisent les requêtes AJAX depuis :
- Le frontend Angular vers Keycloak (token refresh)
- Swagger UI vers Keycloak (authentication)

## 🐛 Troubleshooting

### Issuer toujours `http://localhost:8080`

```bash
# Vérifier que KC_HOSTNAME est défini
docker exec tchl-keycloak-dev env | grep KC_HOSTNAME

# Vérifier les logs Keycloak au démarrage
docker logs tchl-keycloak-dev | grep -i hostname

# Recréer le conteneur (pas juste restart)
docker compose -f compose/docker-compose-project.yml up -d --force-recreate keycloak
```

### Redirect URI mismatch après login

Vérifier que le realm est bien importé avec les nouvelles URLs :
```bash
# Se connecter à l'admin Keycloak
open https://auth.localtest.me/admin

# Vérifier Clients > tchalanet-web > Valid redirect URIs
# Doit contenir : https://app.localtest.me/*
```

### CORS errors

Si le frontend affiche "CORS policy blocked", vérifier :
1. Les `webOrigins` dans le client Keycloak
2. Que Traefik forward bien les headers CORS
3. Que `--proxy-headers=xforwarded` est actif

## 📖 Références

- [Keycloak Hostname Configuration](https://www.keycloak.org/server/hostname)
- [Keycloak Reverse Proxy](https://www.keycloak.org/server/reverseproxy)
- [OAuth 2.0 Redirect URIs](https://datatracker.ietf.org/doc/html/rfc6749#section-3.1.2)

---

**Statut** : ✅ Issuer mismatch résolu. Keycloak retourne maintenant le bon issuer pour tous les environnements.

