# Fix: Appels API bloqués - Problème de proxy Vite

## 🐛 Problème

Les appels API depuis l'application web vers le backend étaient bloqués.

**Symptômes :**
- ❌ Requêtes API ne passent pas
- ❌ Possibles erreurs CORS
- ❌ Timeout ou connexion refusée

## 🔍 Cause racine

Le fichier `.env.development` utilisait une **URL absolue** pour `VITE_API_BASE` :

```bash
# ❌ INCORRECT
VITE_API_BASE=http://localhost:8083/api
```

Avec cette configuration, l'interceptor HTTP Angular créait des requêtes vers `http://localhost:8083/api/...` directement, **sans passer par le proxy Vite**.

Cela causait :
1. **Problèmes CORS** - Le backend doit autoriser `Origin: http://localhost:4200`
2. **Port incorrect** - Le backend écoute sur `:8080` pas `:8083`
3. **Bypass du proxy** - Le proxy Vite configuré dans `vite.config.mts` n'était pas utilisé

## ✅ Solution

Utiliser un **chemin relatif** `/api` pour que les requêtes passent par le proxy Vite :

```bash
# ✅ CORRECT
VITE_API_BASE=/api
```

### Changements appliqués

#### 1. Fichier `.env.development`

```diff
- VITE_API_BASE=http://localhost:8083/api
- VITE_API_BASE_URL=http://localhost:8083
- VITE_APP_URL=https://app.localtest.me
+ VITE_API_BASE=/api
+ VITE_API_BASE_URL=http://localhost:4200
+ VITE_APP_URL=http://localhost:4200
```

#### 2. Configuration du proxy Vite améliorée

**Fichier :** `apps/tchalanet-portal/vite.config.mts`

Ajout de logs pour déboguer :

```typescript
proxy: {
  '/api': {
    target: apiTarget, // Par défaut: http://localhost:8080
    changeOrigin: true,
    secure: false,
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq, req) => {
        console.log(`[Vite Proxy] → ${req.method} ${req.url}`);
      });
      proxy.on('proxyRes', (proxyRes, req) => {
        console.log(`[Vite Proxy] ← ${req.method} ${req.url} [${proxyRes.statusCode}]`);
      });
    },
  },
}
```

## 🔄 Comment ça fonctionne maintenant

### Flux des requêtes

1. **Frontend** (Angular) fait une requête relative :
   ```typescript
   http.get('/v1/configs/i18n?lang=fr')
   ```

2. **Interceptor** ajoute `environment.apiBase` (`/api`) :
   ```typescript
   → http.get('/api/v1/configs/i18n?lang=fr')
   ```

3. **Proxy Vite** intercepte `/api` et redirige vers le backend :
   ```
   http://localhost:4200/api/v1/configs/i18n
   →  http://localhost:8083/api/v1/configs/i18n
   ```

4. **Backend Spring Boot** reçoit la requête (base-path: `/api`) :
   ```
   → Controller endpoint: /v1/configs/i18n
   ```

### Avantages

✅ **Pas de problème CORS** - Toutes les requêtes viennent de `localhost:4200`  
✅ **Port correct** - Le proxy redirige vers `:8083` (port backend local-ide)  
✅ **Débug facile** - Logs du proxy dans la console du serveur Vite  
✅ **Configuration centralisée** - Le port backend est configuré dans une seule variable `TCH_API_TARGET`  

## 🎯 Variables d'environnement pour le proxy

Le proxy Vite utilise la variable d'environnement `TCH_API_TARGET` :

```bash
# Par défaut (si non défini)
TCH_API_TARGET=http://localhost:8083

# Pour un backend sur un autre port
TCH_API_TARGET=http://localhost:9000 npx nx serve tchalanet-portal

# Pour un backend distant
TCH_API_TARGET=https://api.dev.example.com npx nx serve tchalanet-portal
```

## 🔍 Déboguer les problèmes de proxy

### Vérifier que le proxy fonctionne

Dans le terminal du serveur Vite, tu devrais voir :

```
[Vite Proxy] → GET /api/v1/configs/i18n?lang=fr => http://localhost:8080/api/v1/configs/i18n?lang=fr
[Vite Proxy] ← GET /api/v1/configs/i18n?lang=fr [200]
```

### Vérifier que le backend est accessible

```bash
# Test direct
curl http://localhost:8083/api/v1/configs/i18n?lang=fr

# Via le proxy Vite (avec le dev server qui tourne)
curl http://localhost:4200/api/v1/configs/i18n?lang=fr
```

### Erreurs communes

| Erreur | Cause | Solution |
|--------|-------|----------|
| `ECONNREFUSED` | Backend pas démarré | Démarrer le backend Spring Boot |
| `404` | Mauvais endpoint | Vérifier le chemin dans le controller |
| `CORS error` | URL absolue utilisée | Utiliser `/api` (relatif) |
| `502 Bad Gateway` | Mauvais port dans proxy | Vérifier `TCH_API_TARGET` |

## 📚 Configuration backend

Le backend Spring Boot en mode `local-ide` est configuré avec :

```yaml
# tchalanet-server/src/main/resources/application-local-ide.yaml
server:
  port: 8083  # Port local-ide (8080 est réservé à Traefik)

springdoc:
  base-path: ${APP_BASE_PATH:/api}  # Tous les endpoints ont le préfixe /api
```

Donc les controllers Spring avec `@RequestMapping("/me")` sont accessibles à `/api/me`.

## ✅ Checklist de vérification

- [x] `.env.development` utilise `VITE_API_BASE=/api` (relatif)
- [x] Proxy Vite configuré dans `vite.config.mts` (port 8083)
- [x] Backend Spring Boot démarré sur port 8083 (profil local-ide)
- [x] Logs du proxy visibles dans le terminal du serveur Vite
- [x] Pas d'erreur CORS dans la console du navigateur

## 🔄 Pour les autres environnements

### Staging / Production

En staging et production, utiliser des **URLs absolues** car il n'y a pas de proxy Vite :

```bash
# .env.staging
VITE_API_BASE=https://api.stg.tchalanet.com/api

# .env.production
VITE_API_BASE=https://api.tchalanet.com/api
```

Le backend doit être configuré pour accepter les requêtes CORS depuis l'origine de l'application web.

---

**Date de fix :** 19 novembre 2025  
**Problème :** Appels API bloqués (URL absolue bypass le proxy Vite)  
**Solution :** Utiliser chemin relatif `/api` en développement  
**Impact :** Plus de problèmes CORS, proxy Vite utilisé correctement

