# Web Deployment Plans — Vercel & Docker

## Why

Les 3 portails Angular (`public-portal`, `admin-portal`, `platform-portal`) n'ont pas de plan de déploiement unifié. Le `docker-compose-web.yml` existe mais le service `web` est absent de `up-seq.sh` (donc jamais démarré en staging/prod). Le `vercel.json` ne couvre que `public-portal`. Il faut deux stratégies documentées et exécutables.

## What

Deux plans de déploiement pour le web, activables indépendamment par environnement :

### Plan A — Vercel (recommandé pour staging rapide & prod public)

Chaque portail est un projet Vercel distinct. Le backend (API + edge) reste sur Hetzner.

#### Projets Vercel

| Portail | Domaine staging | Domaine prod | Config |
|---|---|---|---|
| `public-portal` | `app.stg.tchalanet.com` | `app.tchalanet.com` | `vercel.json` existant |
| `admin-portal` | `admin.stg.tchalanet.com` | `admin.tchalanet.com` | `apps/admin-portal/vercel.json` à créer |
| `platform-portal` | `portal.stg.tchalanet.com` | `portal.tchalanet.com` | `apps/platform-portal/vercel.json` à créer |

#### Build commands par portail

```bash
# public-portal (déjà configuré à la racine)
pnpm nx build public-portal --configuration=production

# admin-portal
pnpm nx build admin-portal --configuration=production
# outputDirectory: dist/apps/admin-portal/browser

# platform-portal
pnpm nx build platform-portal --configuration=production
# outputDirectory: dist/apps/platform-portal/browser
```

#### Variables d'environnement Vercel (par projet)

```
# Runtime profile — injecté au build
TCH_RUNTIME_PROFILE=stg-vercel   # ou prod-vercel
# Firebase
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_PROJECT_ID=...
# API base URL
VITE_API_BASE_URL=https://api.stg.tchalanet.com   # ou api.tchalanet.com
VITE_EDGE_BASE_URL=https://edge.stg.tchalanet.com
```

#### Déclencheur de déploiement

Vercel détecte les push GitHub. Chaque projet est configuré sur :
- `main` → prod
- `staging` (ou PR preview) → staging

Le fichier `vercel.json` à la racine doit être remplacé par des fichiers par portail, ou Vercel est configuré en mode "monorepo" avec `rootDirectory` par projet.

#### DNS

Staging : CNAME `*.stg.tchalanet.com` → Vercel (ou entrée par projet)
Prod : CNAME `app / admin / portal` → Vercel

Les routeurs Traefik staging/prod n'ont **pas besoin** de routes `web-svc` dans ce plan.

---

### Plan B — Docker (stack auto-hébergée complète sur Hetzner)

Tous les portails sont servis par un unique conteneur Nginx. L'image est publiée sur GHCR.

#### Architecture du conteneur `web`

```
ghcr.io/tchalanet/web:${IMAGE_TAG}
├── /usr/share/nginx/html/public/    ← public-portal
├── /usr/share/nginx/html/admin/     ← admin-portal
└── /usr/share/nginx/html/platform/  ← platform-portal
```

Nginx route par `server_name` :

| `server_name` | `root` |
|---|---|
| `app.{env}.tchalanet.com` | `/usr/share/nginx/html/public` |
| `admin.{env}.tchalanet.com` | `/usr/share/nginx/html/admin` |
| `portal.{env}.tchalanet.com` | `/usr/share/nginx/html/platform` |

#### Dockerfile web (à créer dans `tchalanet-web/`)

```dockerfile
FROM node:22-alpine AS builder
WORKDIR /app
COPY . .
RUN corepack enable pnpm && pnpm install --frozen-lockfile
RUN pnpm nx build public-portal --configuration=production
RUN pnpm nx build admin-portal --configuration=production
RUN pnpm nx build platform-portal --configuration=production

FROM nginx:alpine
COPY --from=builder /app/dist/apps/public-portal/browser   /usr/share/nginx/html/public
COPY --from=builder /app/dist/apps/admin-portal/browser    /usr/share/nginx/html/admin
COPY --from=builder /app/dist/apps/platform-portal/browser /usr/share/nginx/html/platform
COPY tchalanet-infra/nginx/web.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

#### Configuration Nginx (`tchalanet-infra/nginx/web.conf`)

```nginx
server {
    listen 80;
    server_name ~^app\.*;
    root /usr/share/nginx/html/public;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
    location ~* \.(js|css|woff2|png|svg)$ { expires 1y; add_header Cache-Control "public, immutable"; }
}

server {
    listen 80;
    server_name ~^admin\.*;
    root /usr/share/nginx/html/admin;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}

server {
    listen 80;
    server_name ~^portal\.*;
    root /usr/share/nginx/html/platform;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}
```

#### Intégration dans up-seq.sh

`web` est absent de `up-seq.sh`. Il faut l'ajouter après `edge-service` :

```bash
# 8) Up web
echo "→ Up web"
"$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" up -d web || echo "⚠️  web up non-zero" >&2
```

Et ajouter `compose/docker-compose-web.yml` dans `FILES` de `up-seq.sh`.

#### Traefik — routes supplémentaires (staging / prod)

Ajouter dans `traefik/dynamic-src/staging/10-routers.yaml` et `.../prod/10-routers.yaml` :

```yaml
    web-admin:
      rule: Host(`admin.stg.tchalanet.com`)
      entryPoints: [websecure]
      service: web-svc
      tls: { certResolver: letsencrypt }
      middlewares: [secure-headers@file, gzip@file]

    web-platform:
      rule: Host(`portal.stg.tchalanet.com`)
      entryPoints: [websecure]
      service: web-svc
      tls: { certResolver: letsencrypt }
      middlewares: [secure-headers@file, gzip@file]
```

#### Publication de l'image

```bash
./scripts/docker/publish-images.sh tchalanet <tag> ghcr.io
# Le script doit inclure web dans les images à publier
```

#### Variables d'environnement (build-time, baked dans l'image)

Les apps Angular utilisent `environment.prod.ts`. Les URLs API/Edge sont fixées au build. Pour les changer sans rebuild :
- Option 1 : script d'injection `env.js` au démarrage du conteneur (pattern runtime config).
- Option 2 : rebuild par environnement (plus simple, recommandé en V0).

---

## Comparatif

| Critère | Vercel | Docker |
|---|---|---|
| Coût infra | Vercel hobby/pro | Hetzner (déjà payé) |
| CDN / Edge cache | Oui (natif) | Non (Traefik sans cache) |
| Déploiement | Push → auto | `make deploy-staging/prod` |
| Preview PR | Oui | Non |
| Config runtime | Env Vercel par projet | Rebuild ou env.js |
| Autonomie totale | Non | Oui |
| Complexité opérationnelle | Faible | Modérée |
| Portails multiples | 3 projets Vercel | 1 conteneur Nginx |

**Recommandation** : Vercel pour `public-portal` (déjà configuré). Docker pour `admin-portal` et `platform-portal` (accès restreint, pas besoin de CDN, déjà dans la stack Hetzner).

## Impact

- `tchalanet-web/` : `vercel.json` par portail, Dockerfile à créer
- `tchalanet-infra/` : `nginx/web.conf`, `up-seq.sh`, `publish-images.sh`, routeurs Traefik
- DNS : entrées admin.* et portal.* à créer
- CI/CD : pipeline de build image web à ajouter

## Non-goals

- SSR / Angular Universal — hors scope V0
- Multi-région Vercel
- Cache Nginx sophistiqué
- Migration des environnements existants (dev local inchangé)
