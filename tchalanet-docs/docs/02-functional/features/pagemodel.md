# Feature: PageModel — Fonctionnel

## Rôle

Orchestrer la résolution d’un `PageModelResponse` par (scope, tenant, role, logical_page_id) et agréger des blocs dynamiques.

## Invariants

- Orchestration uniquement (pas de logique métier critique dans la BFF)
- Providers dédiés par widget; mapping stable DTO `*Response`

---

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - `/public/pagemodel/:pageId`
  - `/app/pagemodel/:pageId`
- Widgets :
  - `NewsWidget`, `DrawsWidget`, `PlansWidget`, `StatsWidget`
- i18n namespaces :
  - `pagemodel.*`

### Mobile (POS)

- Écrans :
  - home/dashboard POS
- Offline/Sync :
  - cache court des résolutions; dégradations contrôlées

### API (contrats)

- Endpoints :
  - `GET /api/v1/public/pagemodel/{logicalPageId}`
  - `GET /api/v1/tenant/pagemodel/{logicalPageId}`
- Notes :
  - envelope `ApiResponse<PageModelResponse>`; typed IDs; pas de UUID brut

> Source of truth :
>
> - Backend FEATURE : `tchalanet-server/src/main/java/com/tchalanet/server/features/pagemodel/FEATURE_PAGEMODEL.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)
