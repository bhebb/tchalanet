# Feature: Reporting — Fonctionnel

## Rôle

Exposer des dashboards et rapports agrégés (multi-domaines: sales, payout, ledger), avec filtres/pagination/exports.

## Invariants

- Orchestration only (pas de logique métier)
- Respect des permissions fines (tenant/admin)

---

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - `/app/admin/reporting/sales`
  - `/app/admin/reporting/payouts`
  - `/app/admin/reporting/ledger`
- Widgets :
  - `ReportingTables`, `ReportingCharts`
- i18n namespaces :
  - `reporting.*`

### Mobile (POS)

- Écrans :
  - (optionnel) vues synthèse POS

### API (contrats)

- Endpoints :
  - `GET /api/v1/admin/reporting/sales`
  - `GET /api/v1/admin/reporting/payouts`
  - `GET /api/v1/admin/reporting/ledger`
- Notes :
  - pagination `@TchPaging`; `ApiResponse<TchPage<...>>`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/reporting/FEATURE_REPORTING.md`
