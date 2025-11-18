# ✅ Configuration Keycloak dans tchalanet-web - Résumé

## 📋 Diagnostic Complet

### Problème
```
[ERROR] Issuer mismatch. 
Well known issuer: http://localhost:8080/realms/tchalanet 
≠ 
Configured URL: https://auth.localtest.me/realms/tchalanet
```

### Cause
Keycloak utilise uniquement `KC_HOSTNAME` (sans protocole), donc il déduit le protocole de la requête entrante. Quand on accède directement à `localhost:8080`, il retourne `http://localhost:8080`.

---

## ✅ Solution Appliquée

### 1. Infrastructure (Keycloak)

**Fichiers modifiés :**
- ✅ `compose/docker-compose-keycloak.yml` - Ajout de `KC_HOSTNAME_URL` avec variable
- ✅ `envs/common/keycloak.env` - Valeur par défaut pour prod
- ✅ `envs/dev/keycloak.env` - Force `https://auth.localtest.me`
- ✅ `envs/staging/keycloak.env` - Force `https://auth.stg.tchalanet.com`
- ✅ `envs/prod/keycloak.env` - Force `https://auth.tchalanet.com`

**Résultat :** Keycloak retournera toujours un issuer HTTPS correspondant à l'environnement.

### 2. Frontend (Angular)

**Configuration actuelle (✅ CORRECTE) :**

| Environnement | Fichier | authUrl | Utilisé quand |
|---------------|---------|---------|---------------|
| **Production** | `environment.ts` | `https://auth.tchalanet.com/realms/tchalanet` | Build prod (défaut) |
| **Staging** | `environment-staging.ts` | `https://auth.stg.tchalanet.com/realms/tchalanet` | `--configuration=staging` |
| **Local (Traefik)** | `environment-local.ts` | `https://auth.localtest.me/realms/tchalanet` | `--configuration=local` |
| **Dev (IDE)** | `environment-local-ide.ts` | `https://auth.localtest.me/realms/tchalanet` | `nx serve` (défaut dev) |

**Configuration OIDC (`app.config.ts`) :**
```typescript
provideAuth({
  config: {
    authority: environment.authUrl,  // ✅ Utilise le bon fichier d'environnement
    clientId: 'tchalanet-web',
    responseType: 'code',  // PKCE
    scope: 'openid profile email',
    silentRenew: true,
    useRefreshToken: true,
  }
})
```

---

## 🔧 Actions à Effectuer

### Étape 1 : Régénérer les fichiers merged
```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra
./scripts/utils/merge-env.sh dev
```

### Étape 2 : Redémarrer Keycloak
```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra

# Méthode 1 : Via le script automatique
./scripts/utils/refresh-keycloak.sh dev

# Méthode 2 : Manuellement
docker compose -f compose/docker-compose-postgres.yml \
               -f compose/docker-compose-keycloak.yml \
               --env-file=envs/dev/.env.merged \
               down keycloak

docker compose -f compose/docker-compose-postgres.yml \
               -f compose/docker-compose-keycloak.yml \
               --env-file=envs/dev/.env.merged \
               up -d keycloak
```

### Étape 3 : Vérifier l'issuer retourné
```bash
# Attendre que Keycloak soit prêt (30-60 secondes)
sleep 30

# Vérifier l'issuer
curl -sk https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration \
  | grep -o '"issuer":"[^"]*"'

# ✅ Résultat attendu :
# "issuer":"https://auth.localtest.me/realms/tchalanet"
```

### Étape 4 : Vider le cache navigateur
1. Ouvrir DevTools (F12)
2. Application → Storage → Clear site data
3. Ou en incognito

### Étape 5 : Redémarrer le frontend
```bash
# Si via npm serve
npm run start:web

# Si via Docker/Traefik
cd tchalanet-infra
make up-web ENV=dev
```

### Étape 6 : Tester l'authentification
1. Aller sur https://app.localtest.me (ou http://localhost:4200)
2. Cliquer sur "Se connecter"
3. ✅ Devrait rediriger vers Keycloak sans erreur "Issuer mismatch"
4. Se connecter avec : `admin` / `[mot de passe admin]`
5. ✅ Devrait revenir sur l'app avec le token

---

## 🔍 Vérifications Supplémentaires

### Realm Client Keycloak

Se connecter à https://auth.localtest.me → Admin Console → Clients → `tchalanet-web`

**Valid redirect URIs :**
```
https://app.localtest.me/*
http://localhost:4200/*
https://app.stg.tchalanet.com/*
https://app.tchalanet.com/*
```

**Web Origins :**
```
https://app.localtest.me
http://localhost:4200
https://app.stg.tchalanet.com
https://app.tchalanet.com
+
```

**Settings :**
- Client Protocol: `openid-connect`
- Access Type: `public`
- Standard Flow: `ON`
- Direct Access Grants: `ON` (optionnel, pour tests)
- Valid Post Logout Redirect URIs: `+` (ou les mêmes que redirect URIs)

---

## 📊 Matrice de Configuration

| Composant | Dev (local) | Staging | Production |
|-----------|-------------|---------|------------|
| **Keycloak URL** | `https://auth.localtest.me` | `https://auth.stg.tchalanet.com` | `https://auth.tchalanet.com` |
| **Frontend URL** | `https://app.localtest.me` | `https://app.stg.tchalanet.com` | `https://app.tchalanet.com` |
| **API URL** | `https://api.localtest.me` | `https://api.stg.tchalanet.com` | `https://api.tchalanet.com` |
| **KC_HOSTNAME_URL** | ✅ `https://auth.localtest.me` | ✅ `https://auth.stg.tchalanet.com` | ✅ `https://auth.tchalanet.com` |
| **environment file** | `environment-local-ide.ts` | `environment-staging.ts` | `environment.ts` |

---

## 🐛 Debugging

### Logs Keycloak
```bash
docker logs tchl-keycloak-dev -f | grep -i "hostname\|issuer"
```

### Logs Frontend (Console navigateur)
```javascript
// Vérifier la config OIDC
localStorage.getItem('authWellKnownEndPoints')
// Devrait contenir l'issuer avec HTTPS

// Vérifier le token
localStorage.getItem('authnResult')
```

### Test direct well-known
```bash
# Avec détails
curl -skv https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration

# Seulement l'issuer
curl -sk https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq -r .issuer
```

### Vérifier les variables Keycloak
```bash
docker exec tchl-keycloak-dev env | grep KC_HOSTNAME
# Devrait afficher :
# KC_HOSTNAME=auth.localtest.me
# KC_HOSTNAME_URL=https://auth.localtest.me
```

---

## 📚 Documentation Créée

1. **`KEYCLOAK-ISSUER-CONFIG.md`** - Comment Keycloak génère l'issuer (détails techniques)
2. **`WEB-KEYCLOAK-CONFIG.md`** - Configuration complète du frontend (ce document)
3. **`scripts/utils/refresh-keycloak.sh`** - Script automatique pour régénérer et redémarrer

---

## ✅ Checklist Finale

- [x] `KC_HOSTNAME_URL` ajouté dans docker-compose
- [x] `KC_HOSTNAME_URL` défini pour dev/staging/prod
- [x] Documentation complète créée
- [x] Script refresh-keycloak.sh créé
- [ ] **À FAIRE** : Régénérer `.env.merged`
- [ ] **À FAIRE** : Redémarrer Keycloak
- [ ] **À FAIRE** : Vérifier issuer retourné
- [ ] **À FAIRE** : Tester le login frontend

---

## 🎯 Commande Unique pour Tout Résoudre

```bash
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-infra

# Rendre le script exécutable (une seule fois)
chmod +x scripts/utils/refresh-keycloak.sh

# Tout faire d'un coup
./scripts/utils/refresh-keycloak.sh dev
```

Ce script va :
1. ✅ Régénérer `.env.merged`
2. ✅ Vérifier `KC_HOSTNAME_URL`
3. ✅ Redémarrer Keycloak
4. ✅ Attendre que Keycloak soit healthy
5. ✅ Vérifier l'issuer retourné

Ensuite, **redémarrer le frontend** et tester !

