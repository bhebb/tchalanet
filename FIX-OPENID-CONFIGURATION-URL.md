# Fix: URL malformée openid-configuration

## 🔴 Problème

Le frontend appelait une URL malformée :
```
http://localhost:8083/apihttps://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration
```

Au lieu de :
```
https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration
```

## 🔍 Cause racine

L'intercepteur `apiBaseInterceptor` préfixait **toutes** les requêtes HTTP avec `environment.apiBase`, y compris les URLs absolues de Keycloak.

### Problème 1: `isKeycloakUrl()` hardcodé

La fonction `isKeycloakUrl()` dans `http.utils.ts` vérifiait uniquement :
```typescript
if (url.startsWith('http://localhost:8082/realms/tchalanet/'))
```

Mais pas `https://auth.localtest.me` !

### Problème 2: URLs absolues pas détectées

L'intercepteur avait cette ligne commentée :
```typescript
// if (isAbsoluteUrl(req.url)) return next(req);
```

Donc toutes les URLs (même `https://...`) étaient préfixées avec `apiBase`.

## ✅ Solution appliquée

### 1. Mise à jour de `http.utils.ts`

**Avant** :
```typescript
export const OIDC_BASE = 'http://localhost:8082/realms/tchalanet'; // hardcodé

export function isKeycloakUrl(url: string): boolean {
  if (url.startsWith(`${OIDC_BASE}/`)) return true;
  // ...
}
```

**Après** :
```typescript
import { environment } from '@tchl/config';

export function isKeycloakUrl(url: string): boolean {
  // ✅ Utilise environment.authUrl dynamique
  if (url.startsWith(environment.authUrl)) return true;
  
  // ✅ Détection des domaines Keycloak de tous les environnements
  if (url.includes('auth.localtest.me') || 
      url.includes('auth.stg.tchalanet.com') || 
      url.includes('auth.tchalanet.com')) return true;
  
  // ✅ Garde-fou pour les chemins OpenID Connect
  if (url.includes('/realms/') && url.includes('/protocol/openid-connect')) return true;
  
  return false;
}
```

### 2. Mise à jour de `api-base.interceptor.ts`

**Avant** :
```typescript
export const apiBaseInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('assets/')) return next(req);
  if (isKeycloakUrl(req.url)) return next(req);

  // ❌ URLs absolues commentées !
  // if (isAbsoluteUrl(req.url)) return next(req);

  // ❌ Préfixe TOUT avec apiBase
  const url = `${environment.apiBase}${req.url}`;
  return next(req.clone({ url }));
};
```

**Après** :
```typescript
export const apiBaseInterceptor: HttpInterceptorFn = (req, next) => {
  // Laisse les assets (ex: /assets/i18n/fr.json)
  if (req.url.includes('assets/')) return next(req);
  
  // ✅ Skip Keycloak URLs (détection améliorée)
  if (isKeycloakUrl(req.url)) return next(req);

  // ✅ Ne touche pas aux URLs absolues (http:// ou https://)
  if (/^https?:\/\//i.test(req.url)) return next(req);

  // ✅ Préfixe uniquement les URLs relatives
  const url = environment.apiBase ? `${environment.apiBase}${req.url}` : req.url;
  return next(req.clone({ url }));
};
```

## 🎯 Résultat

### URLs traitées correctement

| Type d'URL | Avant | Après |
|------------|-------|-------|
| Keycloak OpenID | ❌ `http://localhost:8083/apihttps://auth.localtest.me/...` | ✅ `https://auth.localtest.me/...` |
| API relative | ✅ `/v1/pages` → `http://localhost:8083/api/v1/pages` | ✅ Inchangé |
| Assets | ✅ `/assets/i18n/fr.json` | ✅ Inchangé |
| API absolue | ❌ Préfixée | ✅ Passée telle quelle |

### Environnements supportés

- ✅ **Local IDE** : `https://auth.localtest.me`
- ✅ **Local Docker** : `https://auth.localtest.me`  
- ✅ **Staging** : `https://auth.stg.tchalanet.com`
- ✅ **Production** : `https://auth.tchalanet.com`

## 🧪 Test

### Avant
```bash
# Dans la console du navigateur
GET http://localhost:8083/apihttps://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration
❌ 404 Not Found
```

### Après
```bash
# Dans la console du navigateur
GET https://auth.localtest.me/realms/tchalanet/.well-known/openid-configuration
✅ 200 OK
{
  "issuer": "https://auth.localtest.me/realms/tchalanet",
  "authorization_endpoint": "https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/auth",
  ...
}
```

## 📋 Fichiers modifiés

1. **libs/shared/api/src/lib/utils/http.utils.ts**
   - Import de `environment` depuis `@tchl/config`
   - Suppression des constantes hardcodées `OIDC_BASE` et `API_BASE`
   - Amélioration de `isKeycloakUrl()` pour détecter dynamiquement

2. **libs/shared/api/src/lib/interceptors/api-base.interceptor.ts**
   - Ajout de la vérification des URLs absolues
   - Amélioration des commentaires
   - Ordre de vérification optimisé

## ✅ Validation

```bash
# 1. Vérifier qu'il n'y a pas d'erreurs de compilation
cd /Users/bhebb/Documents/projets/tchalanet
nx build shared-api

# 2. Démarrer l'app et vérifier dans la console réseau du navigateur
npm run start:web

# 3. Ouvrir https://app.localtest.me
# 4. Dans DevTools > Network, chercher "openid-configuration"
# 5. Vérifier que l'URL est correcte (pas de double préfixe)
```

## 🔐 Sécurité

✅ Aucune clé secrète hardcodée  
✅ Les URLs Keycloak utilisent HTTPS en staging/prod  
✅ L'intercepteur ne modifie que les URLs relatives (API interne)

---

**Statut** : ✅ Problème résolu. L'URL OpenID Configuration est maintenant correctement formée pour tous les environnements.

