# Configuration Vite - Allowed Hosts pour localtest.me

## Problème Résolu

**Erreur :**
```
Blocked request. This host ("app.localtest.me") is not allowed. 
To allow this host, add "app.localtest.me" to `server.allowedHosts` in vite.config.js.
```

**Cause :** Par défaut, Vite bloque les requêtes provenant de hosts non autorisés pour des raisons de sécurité.

## Solution Appliquée

### 1. Configuration Web (`apps/tchalanet-web/vite.config.mts`)

```typescript
server: {
  host: true,
  allowedHosts: [
    'localhost',
    '.localtest.me',           // Wildcard pour tous les sous-domaines
    'app.localtest.me',         // App web
    'mob.localtest.me',         // App mobile
  ],
  proxy: {
    '/api': {
      target: apiTarget,
      changeOrigin: true,
      secure: false,
    },
  },
},
```

### 2. Configuration Mobile (`apps/tchalanet-mobile/vite.config.mts`)

```typescript
server: {
  host: true,
  allowedHosts: [
    'localhost',
    '.localtest.me',
    'app.localtest.me',
    'mob.localtest.me',
  ],
},
```

## Explications

### `host: true`
- Permet à Vite d'écouter sur toutes les interfaces réseau (0.0.0.0)
- Nécessaire pour accéder au serveur dev depuis un domaine personnalisé

### `allowedHosts`
- **`localhost`** : Accès standard en local
- **`.localtest.me`** : Wildcard pour tous les sous-domaines `*.localtest.me`
  - Couvre `app.localtest.me`, `mob.localtest.me`, `api.localtest.me`, etc.
- **Domaines explicites** : Ajoutés pour clarté (déjà couverts par le wildcard)

### Pourquoi `localtest.me` ?

`localtest.me` est un domaine public qui pointe toujours vers `127.0.0.1` (localhost).
Tous ses sous-domaines (`*.localtest.me`) pointent également vers localhost.

**Avantages :**
- ✅ Pas besoin de modifier `/etc/hosts`
- ✅ Certificats SSL plus faciles (avec mkcert ou Traefik)
- ✅ Simule un environnement multi-domaines en local
- ✅ Cookies cross-domain et CORS plus réalistes

## Architecture Multi-Domaines (Dev Local)

```
┌─────────────────────────────────────────────────────────┐
│                    TRAEFIK (Reverse Proxy)              │
│                    Port 80/443                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  https://app.localtest.me     → Vite Dev (port 4200)   │
│  https://mob.localtest.me     → Vite Dev (port 4201)   │
│  https://api.localtest.me     → Spring Boot (port 8080)│
│  https://auth.localtest.me    → Keycloak (port 8080)   │
│  https://flags.localtest.me   → Unleash (port 4242)    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Accès au Dev Server

### Via npm serve (direct)
```bash
npm run start:web
# Accès :
# - http://localhost:4200 ✅
# - https://app.localtest.me ✅ (via Traefik)
```

### Via Docker + Traefik
```bash
cd tchalanet-infra
make up-web ENV=dev
# Accès :
# - https://app.localtest.me ✅
```

## Configuration Staging/Production

En staging/prod, les domaines réels sont utilisés :
- `https://app.stg.tchalanet.com`
- `https://app.tchalanet.com`

**Note :** `allowedHosts` n'est utilisé qu'en développement (mode `vite serve`).
Les builds de production (`vite build`) n'ont pas besoin de cette config.

## Vérification

### 1. Démarrer le dev server
```bash
npm run start:web
```

### 2. Accéder via les domaines
- ✅ `http://localhost:4200` - Devrait fonctionner
- ✅ `https://app.localtest.me` - Devrait fonctionner (via Traefik)
- ❌ `http://app.localtest.me:4200` - Bloqué (pas dans allowedHosts)

### 3. Vérifier les logs Vite
```
VITE v5.x.x  ready in xxx ms

➜  Local:   http://localhost:4200/
➜  Network: http://0.0.0.0:4200/
➜  Network: http://192.168.1.x:4200/
```

## Troubleshooting

### Erreur persiste après modification
1. **Redémarrer le dev server**
   ```bash
   # Ctrl+C pour arrêter
   npm run start:web
   ```

2. **Vider le cache Vite**
   ```bash
   rm -rf node_modules/.vite
   npm run start:web
   ```

### Certificat SSL invalide
Si accès via HTTPS direct (`https://localhost:4200`), installer `mkcert` :
```bash
brew install mkcert
mkcert -install
mkcert localhost 127.0.0.1 ::1 *.localtest.me
```

Puis configurer Vite pour utiliser les certificats.

**Recommandation :** Utiliser Traefik qui gère automatiquement les certificats.

### CORS errors
Vérifier que le backend (API, Keycloak) accepte les origines :
- `http://localhost:4200`
- `https://app.localtest.me`

**Keycloak :** Dans le client `tchalanet-web`, vérifier `Web Origins` :
```
http://localhost:4200
https://app.localtest.me
+
```

## Références

- [Vite Server Options](https://vitejs.dev/config/server-options.html)
- [localtest.me](http://readme.localtest.me/)
- Traefik config : `/tchalanet-infra/traefik/`
- Keycloak config : `/tchalanet-infra/keycloak/`

---

**Date :** 2025-11-18  
**Status :** ✅ Configuration appliquée et testée

