# Configuration Keycloak - Résumé Technique

Date: 2025-11-18

## Problème Résolu

### Symptôme

```
[ERROR] Issuer mismatch.
Well known issuer http://localhost:8080/realms/tchalanet
does not match configured well known url https://auth.localtest.me/realms/tchalanet
```

### Cause

Keycloak utilisait `KC_HOSTNAME` (nom d'hôte uniquement) et déduisait le protocole (http/https) de la requête entrante. Accès direct via `localhost:8080` → retournait `http://localhost:8080`.

### Solution

Utiliser `KC_HOSTNAME_URL` pour forcer l'URL complète avec protocole HTTPS.

---

## Modifications Appliquées

### 1. Docker Compose

**Fichier:** `compose/docker-compose-keycloak.yml`

```yaml
environment:
  KC_HOSTNAME: ${KC_HOSTNAME:-auth.localtest.me}
  KC_HOSTNAME_URL: ${KC_HOSTNAME_URL:-https://auth.localtest.me} # ← Nouveau
  KC_HOSTNAME_STRICT: 'false'
  KC_HOSTNAME_STRICT_HTTPS: 'false' # ← Changé de true à false pour dev
```

### 2. Variables d'Environnement

**Fichier:** `envs/common/keycloak.env` (défaut prod)

```bash
KC_HOSTNAME=auth.tchalanet.com
KC_HOSTNAME_URL=https://auth.tchalanet.com  # ← Nouveau
```

**Fichier:** `envs/dev/keycloak.env`

```bash
KC_HOSTNAME=auth.localtest.me
KC_HOSTNAME_URL=https://auth.localtest.me  # ← Nouveau
```

**Fichier:** `envs/staging/keycloak.env`

```bash
KC_HOSTNAME=auth.stg.tchalanet.com
KC_HOSTNAME_URL=https://auth.stg.tchalanet.com  # ← Nouveau
```

**Fichier:** `envs/prod/keycloak.env`

```bash
KC_HOSTNAME=auth.tchalanet.com
KC_HOSTNAME_URL=https://auth.tchalanet.com  # ← Nouveau
```

### 3. Script d'Automatisation

**Fichier créé:** `scripts/utils/refresh-keycloak.sh`

Automatise :

1. Régénération de `.env.merged`
2. Vérification de `KC_HOSTNAME_URL`
3. Redémarrage de Keycloak
4. Attente du health check
5. Vérification de l'issuer

---

## Configuration Frontend (Angular)

### Structure des Environnements

```
libs/shared/config/src/lib/environments/
├── environment.ts              → Production (défaut)
├── environment-staging.ts      → Staging
├── environment-local.ts        → Dev via Traefik
└── environment-local-ide.ts    → Dev via npm serve
```

### Sélection de l'Environnement

**Fichier:** `apps/tchalanet-web/project.json`

```json
{
  "configurations": {
    "production": {
      // Utilise environment.ts (défaut, pas de fileReplacements)
    },
    "development": {
      "fileReplacements": [
        {
          "replace": "environment.ts",
          "with": "environment-local-ide.ts"
        }
      ]
    },
    "staging": {
      "fileReplacements": [
        {
          "replace": "environment.ts",
          "with": "environment-staging.ts"
        }
      ]
    }
  }
}
```

### Configuration OIDC

**Fichier:** `apps/tchalanet-web/src/app/app.config.ts`

```typescript
provideAuth({
  config: {
    authority: environment.authUrl, // Ex: https://auth.localtest.me/realms/tchalanet
    clientId: 'tchalanet-web',
    responseType: 'code', // Authorization Code Flow + PKCE
    scope: 'openid profile email',
    redirectUrl: environment.appUrl + '/auth/callback',
    postLogoutRedirectUri: environment.appUrl,
    silentRenew: true,
    useRefreshToken: true,
  },
});
```

---

## Matrice de Configuration

| Env              | KC_HOSTNAME_URL                  | Frontend authUrl                                  | Frontend appUrl                 | Build Command                         |
| ---------------- | -------------------------------- | ------------------------------------------------- | ------------------------------- | ------------------------------------- |
| **Dev (IDE)**    | `https://auth.localtest.me`      | `https://auth.localtest.me/realms/tchalanet`      | `https://app.localtest.me`      | `nx serve`                            |
| **Dev (Docker)** | `https://auth.localtest.me`      | `https://auth.localtest.me/realms/tchalanet`      | `https://app.localtest.me`      | `nx build --configuration=local`      |
| **Staging**      | `https://auth.stg.tchalanet.com` | `https://auth.stg.tchalanet.com/realms/tchalanet` | `https://app.stg.tchalanet.com` | `nx build --configuration=staging`    |
| **Production**   | `https://auth.tchalanet.com`     | `https://auth.tchalanet.com/realms/tchalanet`     | `https://app.tchalanet.com`     | `nx build --configuration=production` |

---

## Commandes de Vérification

### 1. Vérifier la configuration Keycloak

```bash
docker exec tchl-keycloak-dev env | grep KC_HOSTNAME
```

**Attendu :**

```
KC_HOSTNAME=auth.localtest.me
KC_HOSTNAME_URL=https://auth.localtest.me
```

### 2. Vérifier l'issuer retourné

```bash
curl -sk https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration | jq -r .issuer
```

**Attendu :** `https://auth.localtest.me/realms/tchalanet`

### 3. Vérifier le build Angular

```bash
nx build tchalanet-web --configuration=development --verbose
```

Chercher : `file replacements` dans les logs

### 4. Vérifier le token en runtime

Console navigateur :

```javascript
localStorage.getItem('authWellKnownEndPoints');
// Devrait contenir : "issuer":"https://auth.localtest.me/realms/tchalanet"
```

---

## Procédure de Déploiement

### Développement Local

```bash
# 1. Régénérer les configs et redémarrer Keycloak
cd tchalanet-infra
./scripts/utils/refresh-keycloak.sh dev

# 2. Démarrer le frontend
cd ..
npm run start:web

# 3. Tester
# → http://localhost:4200
# → Cliquer "Se connecter"
# → Vérifier qu'il n'y a plus d'erreur "Issuer mismatch"
```

### Staging

```bash
# 1. Build frontend
nx build tchalanet-web --configuration=staging

# 2. Déployer l'infra
cd tchalanet-infra
make deploy ENV=staging

# 3. Vérifier
curl -sk https://auth.stg.tchalanet.com/realms/tchalanet/.well-known/openid-configuration | jq -r .issuer
```

### Production

```bash
# 1. Build frontend
nx build tchalanet-web --configuration=production

# 2. Déployer l'infra
cd tchalanet-infra
make deploy ENV=prod

# 3. Vérifier
curl -s https://auth.tchalanet.com/realms/tchalanet/.well-known/openid-configuration | jq -r .issuer
```

---

## Configuration Realm Keycloak

### Client: `tchalanet-web`

**Settings à vérifier dans Admin Console:**

```
Client ID: tchalanet-web
Client Protocol: openid-connect
Access Type: public
Standard Flow Enabled: ON
Implicit Flow Enabled: OFF (déconseillé pour SPA)
Direct Access Grants Enabled: ON (pour tests uniquement)
Valid Redirect URIs:
  - http://localhost:4200/*
  - https://app.localtest.me/*
  - https://app.stg.tchalanet.com/*
  - https://app.tchalanet.com/*
Web Origins:
  - http://localhost:4200
  - https://app.localtest.me
  - https://app.stg.tchalanet.com
  - https://app.tchalanet.com
  - +  (auto from redirect URIs)
Valid Post Logout Redirect URIs: +
```

---

## Documentation Créée

1. **`KEYCLOAK-ISSUER-CONFIG.md`**

   - Détails techniques sur la génération de l'issuer
   - Paramètres KC_HOSTNAME, KC_HOSTNAME_URL, etc.

2. **`WEB-KEYCLOAK-CONFIG.md`**

   - Configuration complète du frontend
   - Flux d'authentification
   - Debugging

3. **`FIX-ISSUER-MISMATCH-SUMMARY.md`**

   - Résumé exécutif
   - Checklist de validation
   - Commandes rapides

4. **`scripts/utils/refresh-keycloak.sh`**
   - Script automatique pour appliquer les changements

---

## Notes Importantes

### 1. Ordre de Priorité Keycloak

Keycloak détermine l'issuer dans cet ordre :

1. `KC_HOSTNAME_URL` (si défini) ← **Nous utilisons ceci**
2. `KC_HOSTNAME` + protocole déduit
3. Headers `X-Forwarded-*` (si `--proxy-headers`)
4. URL de la requête entrante

### 2. HTTPS vs HTTP

En dev, on utilise **HTTPS** partout (via Traefik avec certificat auto-signé pour `*.localtest.me`) pour simuler la prod et éviter les problèmes de CORS / Secure Cookies.

### 3. PKCE (Proof Key for Code Exchange)

Angular OIDC Client active PKCE automatiquement avec `responseType: 'code'`. C'est le standard recommandé pour les SPA (pas besoin de client secret).

### 4. Refresh Token

Activé via :

- Frontend : `useRefreshToken: true`
- Keycloak : Client setting "Use Refresh Token" = ON

Le token est rafraîchi automatiquement 30 secondes avant expiration.

---

## Troubleshooting

### Problème : Certificat SSL invalide en local

**Solution :** Accepter le certificat auto-signé dans le navigateur ou ajouter `*.localtest.me` à vos certificats de confiance.

### Problème : CORS errors

**Solution :** Vérifier les "Web Origins" dans le client Keycloak.

### Problème : Redirect loop

**Solution :** Vider le localStorage et les cookies, vérifier les "Valid Redirect URIs".

### Problème : Token non envoyé à l'API

**Solution :** Vérifier `authInterceptor` dans `app.config.ts`.

---

## Références

- [Keycloak Server Hostname](https://www.keycloak.org/server/hostname)
- [Angular Auth OIDC Client](https://angular-auth-oidc-client.com/)
- [OAuth 2.0 for Browser-Based Apps](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps)
- [PKCE RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)

---

**Dernière mise à jour :** 2025-11-18  
**Auteur :** GitHub Copilot  
**Status :** ✅ Configuration validée et documentée
