# Feature: PublicDraw — Fonctionnel

## Rôle

Exposer un read‑model public pour les résultats de tirage (Home / Historique / Détail) via façades globales.

## Invariants

- Pas d’accès direct aux repos internes des domaines

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/public/results`, `/public/results/latest`
- Widgets: LatestResultsPanel, PublicDrawList
- i18n: `publicdraw.*`

### Mobile (POS)

- Écrans: consultation dernier résultat
- Offline: cache court

### API (contrats)

- Endpoints:
  - `GET /api/v1/public/results/latest`
  - `GET /api/v1/public/results`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/publicdraw/FEATURE_PUBLICDRAW.md`
