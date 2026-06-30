# Deployment

## Runtime standard

Production et staging utilisent :

- Traefik
- PostgreSQL
- Redis
- API (Spring Boot)
- Edge service
- Firebase externe pour l'authentification

## Variables minimales

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
APP_DB_PASSWORD=<secret>
REDIS_PASSWORD=<secret>
EDGE_HMAC_SECRET=<secret>
```

## Staging

```bash
make env-merge ENV=staging
make up-staging
make smoke-staging
```

## Production

```bash
make env-merge ENV=prod
make up-prod
```

---

## Plans de déploiement web

Les 3 portails Angular (`public-portal`, `admin-portal`, `platform-portal`) peuvent être déployés selon deux stratégies. Voir le détail complet dans [`openspec/changes/web-deployment-plans/proposal.md`](../../openspec/changes/web-deployment-plans/proposal.md).

### Plan A — Cloudflare Pages (recommandé)

Chaque portail est un projet CF Pages indépendant. Le backend (API + edge) reste sur Hetzner. CF Pages gère le TLS, le CDN et les previews PR automatiquement — **gratuit (Free plan)**.

| Portail | Staging (preview branch) | Prod (main) |
|---|---|---|
| `public-portal` | `app.stg.tchalanet.com` | `app.tchalanet.com` |
| `admin-portal` | `admin.stg.tchalanet.com` | `admin.tchalanet.com` |
| `platform-portal` | `portal.stg.tchalanet.com` | `portal.tchalanet.com` |

**Build command (identique pour les 3 projets, adapter le nom du portail) :**
```bash
# Framework preset: None
# Root directory: tchalanet-web
# Build command:
pnpm install --frozen-lockfile && pnpm nx build public-portal --configuration=production
# Output directory:
dist/apps/public-portal/browser
```

**Variables CF Pages (par projet, dans dashboard → Settings → Environment variables) :**
```
# Production
VITE_API_BASE_URL=https://api.tchalanet.com
VITE_EDGE_BASE_URL=https://edge.tchalanet.com
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_PROJECT_ID=...

# Preview (staging branch)
VITE_API_BASE_URL=https://api.stg.tchalanet.com
VITE_EDGE_BASE_URL=https://edge.stg.tchalanet.com
```

**Déclencheur :** connexion directe GitHub dans le dashboard CF Pages — push `main` → prod auto, push sur toute autre branche → preview auto.

Les routeurs Traefik n'ont **pas** de route `web-svc` dans ce plan — Cloudflare Pages gère le TLS et le CDN.

---

### Plan B — Docker (stack complète sur Hetzner)

Un conteneur Nginx unique sert les 3 portails. Il est ajouté à la stack `up-seq.sh`.

**Image :** `ghcr.io/tchalanet/web:${IMAGE_TAG}` publiée sur GHCR.

**Structure interne du conteneur :**
```
/usr/share/nginx/html/public/    ← public-portal
/usr/share/nginx/html/admin/     ← admin-portal
/usr/share/nginx/html/platform/  ← platform-portal
```

**Nginx route par `server_name` :**
```
app.{env}.tchalanet.com    → /public
admin.{env}.tchalanet.com  → /admin
portal.{env}.tchalanet.com → /platform
```

**Ajout dans up-seq.sh :** ajouter `compose/docker-compose-web.yml` dans `FILES` + étape `Up web` après `Up edge-service`.

**Traefik :** ajouter les routers `web-admin` et `web-platform` dans `traefik/dynamic-src/{staging,prod}/10-routers.yaml`.

**Publication de l'image :**
```bash
./scripts/docker/publish-images.sh tchalanet <tag> ghcr.io
```

---

### Comparatif

| Critère | CF Pages (Plan A) | Docker (Plan B) |
|---|---|---|
| CDN / cache edge | Oui (natif) | Non |
| Preview PR | Oui (auto) | Non |
| Coût | Gratuit (CF Pages Free) | Hetzner (déjà payé) |
| Config runtime | Env CF Pages par projet | Rebuild ou env.js |
| Complexité opérationnelle | Faible | Modérée |
| Autonomie totale | Non | Oui |

**Recommandation :** Cloudflare Pages (Plan A) pour les 3 portails. Gratuit, CDN global, previews PR automatiques, zéro config Docker. Le domaine `tchalanet.com` est déjà sur Cloudflare — DNS et TLS sont natifs.
