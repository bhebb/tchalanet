# Tasks

## 1. Query API

- [x] Create `GetTenantKpisQuery`.
- [x] Create `GetSalesReportQuery`.
- [x] Create `GetOutletReportQuery`.
- [x] Create result models:
  - [x] `TenantKpisView`
  - [x] `SalesReportLine`
  - [x] `OutletReportLine`
  - [ ] `ReportPeriod` *(not created — period expressed as from/to dates directly in each query)*

## 2. Move readers

- [x] Move `TenantKpisReader` to `core.analytics.internal.infra.persistence` (as `TenantKpisAnalyticsReader`).
- [x] Move `SalesReportReader` to `core.analytics.internal.infra.persistence` (as `SalesReportAnalyticsReader`).
- [x] Add or move `OutletReportReader` if needed (as `OutletReportAnalyticsReader`).
- [x] Rename readers with analytics ownership.
- [ ] Delete legacy readers from `features.reporting` (`TenantKpisReader`, `SalesReportReader`, `OutletPerformanceReader`) — duplicates still present.

## 3. Query handlers

- [x] Implement `GetTenantKpisQueryHandler`.
- [x] Implement `GetSalesReportQueryHandler`.
- [x] Implement `GetOutletReportQueryHandler`.
- [x] Ensure handlers return API models and do not expose persistence rows.

## 4. Feature reporting refactor

- [x] Update `GetTenantKpisService` to call `QueryBus`.
- [x] Update sales report flow to call `QueryBus`.
- [x] Keep `OutletReportExportService` in features but source data from `core.analytics`.
- [ ] Remove `EntityManager` usage from `features.reporting`. *(`SalesReportReader` and `OutletPerformanceReader` still use `EntityManager` — legacy readers not yet deleted)*

## 5. Export

- [x] Keep simple CSV export in V1 if acceptable.
- [ ] Document future move to `platform.document` for report rendering/storage.
- [x] Ensure export file endpoints are not wrapped as JSON if returning files.

## 6. Tests

- [ ] Unit test query handlers.
- [ ] Integration test report queries with fixture tickets/payout/settlement data.
- [ ] Test date range boundaries.
- [ ] Test feature reporting response mapping.
