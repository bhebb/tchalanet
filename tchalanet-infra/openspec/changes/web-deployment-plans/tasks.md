# Tasks — Web Deployment Plans

## Plan A — Vercel

### Setup projets Vercel
- [ ] Créer un projet Vercel pour `admin-portal` (rootDirectory: `tchalanet-web`, buildCommand: `pnpm nx build admin-portal --configuration=production`, outputDirectory: `dist/apps/admin-portal/browser`)
- [ ] Créer un projet Vercel pour `platform-portal` (idem, `platform-portal`)
- [ ] Renommer le projet Vercel existant en `tchalanet-public-portal` si nécessaire
- [ ] Configurer les variables d'environnement dans chaque projet Vercel (staging + prod) : `TCH_RUNTIME_PROFILE`, `VITE_API_BASE_URL`, `VITE_EDGE_BASE_URL`, Firebase vars
- [ ] Configurer les domaines custom dans Vercel : `app.*`, `admin.*`, `portal.*`

### DNS
- [ ] Ajouter CNAME `admin.stg.tchalanet.com` → Vercel
- [ ] Ajouter CNAME `portal.stg.tchalanet.com` → Vercel
- [ ] Ajouter CNAME `admin.tchalanet.com` → Vercel
- [ ] Ajouter CNAME `portal.tchalanet.com` → Vercel

### Runtime profiles
- [ ] Vérifier que `runtime:stg-vercel` et `runtime:prod-vercel` injectent les bonnes vars pour `admin-portal` et `platform-portal`
- [ ] Tester un build local avec `pnpm nx build admin-portal --configuration=production`
- [ ] Tester un build local avec `pnpm nx build platform-portal --configuration=production`

---

## Plan B — Docker

### Dockerfile web
- [ ] Créer `tchalanet-web/Dockerfile.web` (multi-stage : builder Node 22 → Nginx alpine)
- [ ] Valider le build local : `docker build -f tchalanet-web/Dockerfile.web -t tchalanet-web:local .`
- [ ] Créer `tchalanet-infra/nginx/web.conf` (3 blocs server par portail)

### docker-compose-web.yml
- [ ] Vérifier que `docker-compose-web.yml` monte les bons réseaux (`edge` uniquement — pas de `back`)
- [ ] Ajouter les labels Traefik manquants pour `admin` et `platform` (ou utiliser server_name Nginx)

### up-seq.sh
- [ ] Ajouter `compose/docker-compose-web.yml` dans le tableau `FILES`
- [ ] Ajouter l'étape `Up web` après `Up edge-service`

### Traefik routers
- [ ] Ajouter `web-admin` et `web-platform` dans `traefik/dynamic-src/staging/10-routers.yaml`
- [ ] Ajouter `web-admin` et `web-platform` dans `traefik/dynamic-src/prod/10-routers.yaml`

### CI/CD & publication image
- [ ] Mettre à jour `scripts/docker/publish-images.sh` pour inclure l'image `web`
- [ ] Vérifier que `IMAGE_TAG` est cohérent entre l'image API et l'image web

### DNS
- [ ] Ajouter `admin.stg.tchalanet.com` → IP Hetzner staging
- [ ] Ajouter `portal.stg.tchalanet.com` → IP Hetzner staging
- [ ] Ajouter `admin.tchalanet.com` → IP Hetzner prod
- [ ] Ajouter `portal.tchalanet.com` → IP Hetzner prod

### Smoke tests
- [ ] Ajouter `admin.*` et `portal.*` dans `scripts/utils/smoke-staging.sh`

---

## Documentation
- [x] `openspec/changes/web-deployment-plans/proposal.md` — plan rédigé
- [ ] `docs/operations/DEPLOYMENT.md` — mettre à jour avec les deux plans
- [ ] `docs/operations/IMAGES-DEPLOYMENT.md` — ajouter image web
- [ ] `compose/docker-compose.index.md` — documenter `docker-compose-web.yml`
