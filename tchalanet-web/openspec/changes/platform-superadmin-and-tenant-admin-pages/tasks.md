# Tasks

> Inline template/styles obligatoires (pas de .html/.scss séparés).
> Signals + store service (pas NgRx).
> Un service API par domaine.
> Cocher [ ] → [x] en temps réel.

## Slice 1 — Infrastructure transverse

> **Blocked / superseded by `admin-superadmin-sidenav-v0`** (Slice 1)
> SupportAccessStore, SensitiveDataMaskPipe et AdminOverrideBanner sont implémentés là.

- ~~`SupportAccessStore` — signal store dans `core/tenant-admin-access/`~~
- ~~`SensitiveDataMaskPipe` — masque phone/email/amount en mode support~~
- ~~`AdminOverrideBanner` — bannière SUPPORT_OVERRIDE / SUPPORT_READONLY dans `features/private/shared/`~~
- ~~Intégrer `AdminOverrideBanner` dans le shell privé (`PrivateShellPage` ou `PrivateShellComponent`)~~

## Slice 2 — Platform API services

> **Blocked / superseded by `admin-superadmin-sidenav-v0`** (Slices 5, 7, 12)
> Services platform API répartis dans les slices domaine du nouveau change.

- ~~`platform-tenants-api.service.ts` — tenant CRUD + admin tenant creation~~
- ~~`platform-tenant-admin-access-api.service.ts` — session support (start/stop)~~
- ~~`platform-ops-api.service.ts` — batch jobs/gates/executions + draws ops + draw-results + cache~~
- ~~Migrer `PlatformAdminApi` vers le nouveau découpage (garder compatibilité)~~

## Slice 3 — Platform dialog

> **Blocked / superseded by `admin-superadmin-sidenav-v0`** (Slice 12)
> Dialog absorbé dans le Slice 12 Support tenant.

- ~~`StartTenantAdminAccessDialog` — dialog raison + checkbox + mode~~

## Slice 4 — Platform pages

> **Blocked / superseded by `admin-superadmin-sidenav-v0`** (Slices 2, 5, 7, 12)
> Les pages platform ops existent déjà. Les pages tenants et la sidenav sont dans le nouveau change.

- ~~`platform-tenants.page.ts` — liste paginée + actions par statut (rework)~~
- ~~`platform-tenant-create.page.ts` — formulaire création tenant + preview~~
- ~~`platform-tenant-admins.page.ts` — liste admins d'un tenant~~
- ~~`platform-tenant-admin-create.page.ts` — formulaire invitation admin~~
- ~~`platform-ops-batch.page.ts` — tabs Jobs/Gates/Executions~~
- ~~`platform-ops-draws.page.ts` — actions draw ops avec dryRun~~
- ~~`platform-ops-draw-results.page.ts` — liste + actions sensibles~~
- ~~`platform-ops-cache.page.ts` — liste caches + clear~~
- ~~`platform-audit.page.ts` — log d'audit~~
- ~~`platform.routes.ts` — mise à jour routes~~

## Slice 5 — Admin API services

> **Blocked / superseded by `admin-tenant-sidenav-v0`**
> Ces services sont absorbés dans le Slice 11 de `admin-tenant-sidenav-v0`.
> Ne pas implémenter ici.

- ~~`runtime-api.service.ts` — GET /tenant/runtime~~
- ~~`tenant-config-api.service.ts` — GET/PUT /admin/tenant-config + sub-endpoints~~
- ~~`business-days-api.service.ts` — CRUD business days~~
- ~~`games-admin-api.service.ts` — list/catalog/enable/disable/settings~~

## Slice 6 — Admin pages

> **Blocked / superseded by `admin-tenant-sidenav-v0`**
> Ces pages sont absorbées dans les Slices 7, 11 de `admin-tenant-sidenav-v0`.
> Ne pas implémenter ici.

- ~~`admin-runtime.page.ts` — runtime info + regional + locales + raw JSON~~
- ~~`admin-config.page.ts` — sections locale/communication/document/rules + health~~
- ~~`admin-business-days.page.ts` — calendrier + overrides + dialog~~
- ~~`admin-games.page.ts` — tabs activés/catalogue + dialog settings~~
- ~~`admin.routes.ts` — mise à jour routes (settings/runtime, settings/config, business-days, games)~~

## Validation

- [ ] `nx build` green après chaque slice
- [ ] Aucune donnée fake hardcodée dans les templates
- [ ] Loading/error/empty states présents sur toutes les pages
- [ ] AdminOverrideBanner visible dans /app/admin/** quand session support active
- [ ] Actions mutantes désactivées en SUPPORT_READONLY
