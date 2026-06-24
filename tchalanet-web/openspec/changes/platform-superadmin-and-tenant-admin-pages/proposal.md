# Change: Platform Superadmin Pages + Tenant Admin Configuration Pages

## Status

**Superseded — tous les slices absorbés ailleurs.**

- Slices 5–6 (admin tenant) → superseded par `admin-tenant-sidenav-v0`
- Slices 1–4 (platform/superadmin) → superseded par `admin-superadmin-sidenav-v0`

Ce change peut être archivé.

## Why

Les pages superadmin platform (`/app/platform`) sont quasi-entièrement des placeholders avec des empty states. Les pages tenant admin (`/app/admin`) manquent les surfaces de configuration essentielles (runtime, config, business days, games). Les routes existent, les guards sont en place, mais le contenu fonctionnel est absent.

Ce change implémente les deux surfaces en priorité P0 : les pages platform superadmin (gestion tenants, accès support audité, ops batch/draws/cache) et les pages admin tenant (runtime, config, business days, games) ainsi que le mode support superadmin (bannière + masquage données sensibles).

## What changes

### Platform Superadmin (`/app/platform`)

**API services** (dans `features/platform/`) :
- `platform-tenants-api.service.ts` — CRUD tenant + admin tenant (`GET/POST/PATCH /platform/tenants`, `POST /platform/tenants/:id/admins`)
- `platform-tenant-admin-access-api.service.ts` — session support (`POST/DELETE /platform/tenants/:id/admin-access`)
- `platform-ops-api.service.ts` — batch jobs, gates, executions, draws ops, draw-results, cache

**State** :
- `core/tenant-admin-access/support-access.store.ts` — état override support (sessionId, tenantId, mode, actorRole)

**Composants partagés** (dans `features/private/shared/`) :
- `admin-override-banner.ts` — bannière support affichée dans tout `/app/admin/**`
- `sensitive-data-mask.pipe.ts` — masque téléphone/email/montants en mode support

**Pages platform** (inline template/styles, pas de `.html`/`.scss` séparés) :
- `platform-tenants.page.ts` — liste tenants paginée depuis API, actions par statut
- `platform-tenant-create.page.ts` — formulaire création tenant (code, name, type, timezone, currency, address, theme, activate)
- `platform-tenant-admins.page.ts` — liste admins d'un tenant
- `platform-tenant-admin-create.page.ts` — formulaire création/invitation admin tenant
- `platform-ops-batch.page.ts` — tabs Jobs/Gates/Executions + dialog start job
- `platform-ops-draws.page.ts` — actions Generate/Open/Close/Apply avec dryRun + dialog
- `platform-ops-draw-results.page.ts` — liste draw results + actions sensibles (manual/override/confirm)
- `platform-ops-cache.page.ts` — liste caches + clear one/all
- `platform-audit.page.ts` — log d'audit superadmin

**Dialogs** (dans `features/platform/shared/`) :
- `start-tenant-admin-access-dialog.ts` — raison + checkbox + mode détecté par statut

**Mise à jour** :
- `platform.routes.ts` — ajouter les nouvelles routes
- `platform-admin-api.service.ts` — renommer/migrer vers le nouveau découpage

### Tenant Admin (`/app/admin`)

**API services** (dans `features/admin/`) :
- `runtime-api.service.ts` — `GET /tenant/runtime`, `GET /public/tenant/runtime`
- `tenant-config-api.service.ts` — `GET/PUT /admin/tenant-config`, `GET /admin/tenant-config/communication`, `GET /admin/tenant-config/document`
- `business-days-api.service.ts` — `GET/PUT/DELETE /admin/business-days`
- `games-admin-api.service.ts` — `GET /admin/games`, `GET /admin/games/catalog`, `POST /admin/games/:code/enable|disable`, `PATCH /admin/games/:code/settings`

**Pages admin** (inline template/styles) :
- `admin-runtime.page.ts` — runtime info + regional config + locales dynamiques + raw JSON
- `admin-config.page.ts` — sections locale/communication/document/rules + health card
- `admin-business-days.page.ts` — calendrier mensuel + liste overrides + dialog ajout/édition
- `admin-games.page.ts` — tabs jeux activés/catalogue + dialog settings

**Mise à jour** :
- `admin.routes.ts` — ajouter les nouvelles routes settings/runtime, settings/config, business-days, games
- `private-shell` — intégrer `AdminOverrideBanner` en haut du `<router-outlet>`

## Impact

- `apps/tch-portal/src/app/features/platform/` — nouveau contenu majeur
- `apps/tch-portal/src/app/features/admin/` — nouvelles pages + services
- `apps/tch-portal/src/app/features/private/shared/` — bannière + pipe
- `apps/tch-portal/src/app/core/` — nouveau store `SupportAccessStore`
- Aucune lib Nx créée (tout reste dans l'app)

## Non-goals

- Pas de widget PageModel pour ces pages (elles sont data-driven direct, pas page-model)
- Pas de gestion locale de mot de passe
- Pas de reveal données sensibles (V1+)
- Pas de validation tenant admin pour reveal (V2)
- Pas de NgRx (signals + store services)
- Pas d'audit viewer complet (V1+)
