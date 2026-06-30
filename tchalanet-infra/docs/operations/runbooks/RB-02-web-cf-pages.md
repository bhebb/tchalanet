# RB-02 — Déploiement web via Cloudflare Pages

**Quand utiliser ce runbook :** mise en place initiale des 3 portails Angular sur CF Pages, ou réintégration après abandon de Vercel.

**Durée estimée :** 45-60 min (création des 3 projets + DNS + first deploy).

**Résultat attendu :** 3 projets CF Pages actifs, déploiements automatiques sur push `main`, previews PR auto, domaines personnalisés configurés.

---

## Architecture

```
GitHub push main
  └─ CF Pages (connexion GitHub native)
       ├─ public-portal  → app.tchalanet.com        (prod)
       │                 → app.stg.tchalanet.com     (preview branch)
       ├─ admin-portal   → admin.tchalanet.com       (prod)
       │                 → admin.stg.tchalanet.com   (preview branch)
       └─ platform-portal→ portal.tchalanet.com      (prod)
                         → portal.stg.tchalanet.com  (preview branch)
```

### Runtime config (pattern important)

Les portails ne lisent pas les env vars au build — ils chargent `/assets/config/runtime.{appId}.json` **au démarrage** via HTTP. Ce fichier JSON doit contenir les URLs absolues vers l'API.

Pour CF Pages (domaine différent de l'API), ce fichier doit être **généré au build** à partir des variables d'environnement CF Pages.

---

## Prérequis

- Compte Cloudflare avec accès au domaine `tchalanet.com`
- Accès GitHub repo en tant que collaborateur (pour connecter CF Pages)
- Droits de créer des projets CF Pages (rôle Editor ou Admin)

---

## Note sur les thèmes et assets générés

Les fichiers `theme-presets.registry.ts` et `token-manifest.generated.ts` sont **déjà commitués dans le repo** — ce sont des artifacts "reviewable" régénérés localement par les devs via `pnpm theme:generate`. CF Pages n'a pas à les regénérer.

**Aucune étape de génération de thème n'est nécessaire dans la commande de build CF Pages.**

---

## Étape 1 — Script de génération runtime config

Les portails chargent `/assets/config/runtime.{appId}.json` au démarrage — ce fichier doit contenir les URLs absolues de l'API (différentes entre staging et prod).

La config Firebase (apiKey, projectId, etc.) est déjà dans le fallback codé dans `environment.prod.ts` — c'est un identifiant public, pas un secret. **Ne pas le dupliquer dans CF Pages env vars.**

Seules 3 variables diffèrent vraiment entre staging et prod :
- `TCH_API_BASE_URL` — URL de l'API Spring Boot
- `TCH_EDGE_BASE_URL` — URL de l'edge service
- `TCH_ADMIN_PORTAL_URL` + `TCH_PLATFORM_PORTAL_URL` — liens cross-portail (public-portal seulement)

Créer `tchalanet-web/scripts/generate-runtime-config.mjs` :

```js
#!/usr/bin/env node
// Génère les overrides runtime config pour CF Pages.
// Seules les URLs absolues diffèrent par env — la config Firebase est dans le fallback.

import { writeFileSync, mkdirSync, readFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const appsDir = resolve(__dirname, '..');

const apiBase = process.env.TCH_API_BASE_URL;
const edgeBase = process.env.TCH_EDGE_BASE_URL;

if (!apiBase || !edgeBase) {
  console.error('❌ TCH_API_BASE_URL and TCH_EDGE_BASE_URL are required');
  process.exit(1);
}

const portals = [
  {
    appId: 'public-portal',
    dir: 'public-portal',
    override: {
      apiBaseUrl: `${apiBase}/api/v1`,
      authBaseUrl: `${edgeBase}/auth`,
      portalBaseUrls: {
        'admin-portal': process.env.TCH_ADMIN_PORTAL_URL ?? `${apiBase.replace('api.', 'admin.')}`,
        'platform-portal': process.env.TCH_PLATFORM_PORTAL_URL ?? `${apiBase.replace('api.', 'portal.')}`,
      },
    },
  },
  {
    appId: 'admin-portal',
    dir: 'admin-portal',
    override: {
      apiBaseUrl: `${apiBase}/api/v1`,
      authBaseUrl: `${edgeBase}/auth`,
    },
  },
  {
    appId: 'platform-portal',
    dir: 'platform-portal',
    override: {
      apiBaseUrl: `${apiBase}/api/v1`,
      authBaseUrl: `${edgeBase}/auth`,
    },
  },
];

// Chercher le répertoire de sortie des assets selon la config Angular du portail
// (peut être src/assets/config/ ou public/assets/config/ selon le portail)
for (const { appId, dir, override } of portals) {
  const candidates = [
    resolve(appsDir, 'apps', dir, 'src', 'assets', 'config'),
    resolve(appsDir, 'apps', dir, 'public', 'assets', 'config'),
  ];
  const outDir = candidates[0]; // ajuster si Angular est configuré avec public/
  mkdirSync(outDir, { recursive: true });
  const outFile = resolve(outDir, `runtime.${appId}.json`);
  writeFileSync(outFile, JSON.stringify(override, null, 2));
  console.log(`✅ ${outFile}`);
}
```

---

## Étape 2 — Fichiers CF Pages (_redirects, _headers)

CF Pages ne supporte pas `vercel.json`. Créer des fichiers équivalents dans `tchalanet-web/`.

Ces fichiers doivent être copiés dans le répertoire de sortie au moment du build (ou mis dans le `assets` d'Angular).

**`tchalanet-web/cf-pages/_redirects`** (SPA fallback — un fichier par portail, dans son output dir) :
```
/realms/*  /404.html  404
/*         /index.html  200
```

**`tchalanet-web/cf-pages/_headers`** :
```
/assets/i18n/*
  Cache-Control: public, max-age=86400

/assets/fallback/*
  Cache-Control: public, max-age=86400, stale-while-revalidate=604800

/*.js
  Cache-Control: public, max-age=31536000, immutable

/*.css
  Cache-Control: public, max-age=31536000, immutable
```

Les copier dans le build via la commande CF Pages (voir étape 4).

---

## Étape 3 — Créer les projets CF Pages

Dans le dashboard Cloudflare → **Pages** → **Create a project** → **Connect to Git**.

Répéter pour chacun des 3 portails :

### Projet 1 : `tchalanet-public-portal`

| Champ | Valeur |
|---|---|
| Repository | `{owner}/tchalanet` |
| Branch de production | `main` |
| Root directory | `tchalanet-web` |
| Framework preset | `None` |
| Build command | voir ci-dessous |
| Output directory | `dist/apps/public-portal/browser` |

**Build command :**
```bash
node scripts/generate-runtime-config.mjs && \
cp -r cf-pages/. dist/apps/public-portal/browser/ && \
pnpm install --frozen-lockfile && \
pnpm nx build public-portal --configuration=production && \
cp cf-pages/_redirects dist/apps/public-portal/browser/_redirects && \
cp cf-pages/_headers dist/apps/public-portal/browser/_headers
```

> Note : le build Angular doit tourner après `generate-runtime-config.mjs` pour que les fichiers JSON générés soient inclus dans le build (si angular.json pointe vers `public/` ou `src/assets/`). Ajuster l'ordre si nécessaire après le premier test.

### Projet 2 : `tchalanet-admin-portal`

Identique, remplacer `public-portal` par `admin-portal`.

### Projet 3 : `tchalanet-platform-portal`

Identique, remplacer `public-portal` par `platform-portal`.

---

## Étape 4 — Variables d'environnement CF Pages

La config Firebase est déjà dans le code (`environment.prod.ts`) — ne pas la répliquer ici. Seules les URLs d'infrastructure changent par environnement.

Pour chaque projet, dans CF Pages → Settings → Environment variables :

**Production (branch: `main`) :**
| Variable | Valeur |
|---|---|
| `TCH_API_BASE_URL` | `https://api.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.tchalanet.com` |
| `TCH_ADMIN_PORTAL_URL` | `https://admin.tchalanet.com` *(public-portal seulement)* |
| `TCH_PLATFORM_PORTAL_URL` | `https://portal.tchalanet.com` *(public-portal seulement)* |

**Preview / Staging (toutes branches sauf `main`) :**
| Variable | Valeur |
|---|---|
| `TCH_API_BASE_URL` | `https://api.stg.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.stg.tchalanet.com` |
| `TCH_ADMIN_PORTAL_URL` | `https://admin.stg.tchalanet.com` *(public-portal seulement)* |
| `TCH_PLATFORM_PORTAL_URL` | `https://portal.stg.tchalanet.com` *(public-portal seulement)* |

> Dans CF Pages, les variables "Preview" s'appliquent à toutes les branches sauf `main`.

---

## Étape 5 — Domaines personnalisés

Dans chaque projet CF Pages → Custom domains → Add a custom domain :

| Projet | Domaine prod | Domaine staging |
|---|---|---|
| public-portal | `app.tchalanet.com` | `app.stg.tchalanet.com` |
| admin-portal | `admin.tchalanet.com` | `admin.stg.tchalanet.com` |
| platform-portal | `portal.tchalanet.com` | `portal.stg.tchalanet.com` |

CF Pages crée automatiquement les enregistrements DNS dans Cloudflare et active le proxy orange (CDN + TLS).

> Pour le staging, CF Pages génère automatiquement un sous-domaine preview par branche (`{branch}.{project}.pages.dev`). Ajouter `app.stg.tchalanet.com` pointe vers ce déploiement preview de la branche `main` (= staging).

**Alternative pour staging :** utiliser une branche dédiée `staging` et pointer `app.stg.tchalanet.com` vers cette branche dans CF Pages → Branches & deployments.

---

## Étape 6 — Supprimer les anciens workflows Vercel

Une fois les 3 projets CF Pages validés :

```bash
git rm .github/workflows/vercel-stg.yml
git rm .github/workflows/vercel-preview.yml
git commit -m "chore(ci): remove obsolete Vercel workflows — replaced by CF Pages"
```

> Les secrets Vercel (`VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`) peuvent être supprimés de GitHub Settings → Secrets.

---

## Vérifications

```bash
# Test SPA routing — doit retourner index.html et non 404
curl -I https://app.stg.tchalanet.com/some/deep/route

# Test runtime config chargé correctement — ouvrir les DevTools réseau
# Vérifier que /assets/config/runtime.public-portal.json retourne l'URL staging
curl https://app.stg.tchalanet.com/assets/config/runtime.public-portal.json | jq .apiBaseUrl
# Attendu: "https://api.stg.tchalanet.com/api/v1"

# Test cache headers sur un fichier JS
curl -I https://app.tchalanet.com/main.abc123.js | grep cache-control
# Attendu: max-age=31536000, immutable
```

---

## Troubleshooting

| Symptôme | Cause probable | Action |
|---|---|---|
| Build échoue sur `generate-runtime-config.mjs` | pnpm/node pas dispo avant install | Déplacer le script après `pnpm install` |
| `runtime.*.json` retourne les URLs `/api/v1` relatives | Script non exécuté ou variables non définies | Vérifier les env vars CF Pages + logs de build |
| SPA : rechargement page → 404 | `_redirects` pas copié dans output | Vérifier la commande de copie dans le build command |
| Domaine custom non résolu | CNAME CF Pages pas encore propagé | Attendre 1-5 min, vérifier dans Cloudflare DNS |
| Build lent (5-10 min) | `pnpm install` sans cache | CF Pages cache le `node_modules` automatiquement au 2ème build |
