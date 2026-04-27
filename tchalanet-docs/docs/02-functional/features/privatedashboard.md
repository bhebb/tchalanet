# Feature: PrivateDashboard — Fonctionnel

## Rôle

Exposer un tableau de bord privé (tenant/admin) avec widgets agrégés.

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/app/dashboard`
- Widgets: DashboardWidgets

### Mobile (POS)

- Écrans: dashboard POS

### API (contrats)

- Endpoints:
  - `GET /api/v1/tenant/dashboard`
  - `GET /api/v1/admin/dashboard`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/privatedashboard/FEATURE_PRIVATEDASHBOARD.md`
