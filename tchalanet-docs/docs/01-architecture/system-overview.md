# System overview

## Objectif

Tchalanet est une plateforme multi-tenant (web + mobile + backend + infra)
pour : vente de tickets, tirages, ingestion résultats, paiements, configuration tenant.

## Composants

- **Web (Angular/Nx)** : public + privé (dashboards)
- **Mobile (Flutter)** : POS offline/online
- **Backend (Spring Boot)** : domaines critiques (core), orchestration (features), référentiels (catalog)
- **Infra (Docker)** : Postgres (RLS), Redis, Meilisearch, Keycloak, Traefik, Unleash, etc.
- **Edge service** : (si applicable) proxy/rules/templating léger

## Flux majeurs (vue “carte”)

- Sell ticket → persist → events → ledger/stats/print
- Draw scheduling → open/close → results fetch/apply → settlement
- Public verify ticket → endpoint public + noindex + rate limit
- Tenant theming/i18n → PageModel + tokens CSS runtime

## Source of truth (détails)

- Backend: `tchalanet-server/docs/ARCHITECTURE.md`
- Infra: `tchalanet-infra/docs/`
- Web: `apps/tchalanet-web/*.md` et `libs/**/README.md`
- OpenSpec packs: `openspec/context/*`
