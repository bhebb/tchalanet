# Feature: Stats — Fonctionnel

## Rôle

Exposer des statistiques agrégées (tenant/admin) sur ventes, payouts, draws.

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/app/stats/*`
- Widgets: StatsCharts, StatsTables

### Mobile (POS)

- Écrans: stats POS

### API (contrats)

- Endpoints:
  - `GET /api/v1/tenant/stats/sales`
  - `GET /api/v1/tenant/stats/payouts`
  - `GET /api/v1/tenant/stats/draws`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/stats/FEATURE_STATS.md`
