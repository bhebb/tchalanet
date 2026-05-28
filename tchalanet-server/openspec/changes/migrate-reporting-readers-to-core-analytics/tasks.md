# Tasks

## 1. Query API

- [ ] Create `GetTenantKpisQuery`.
- [ ] Create `GetSalesReportQuery`.
- [ ] Create `GetOutletReportQuery`.
- [ ] Create result models:
  - [ ] `TenantKpisView`
  - [ ] `SalesReportLine`
  - [ ] `OutletReportLine`
  - [ ] `ReportPeriod`

## 2. Move readers

- [ ] Move `TenantKpisReader` to `core.analytics.internal.infra.persistence`.
- [ ] Move `SalesReportReader` to `core.analytics.internal.infra.persistence`.
- [ ] Add or move `OutletReportReader` if needed.
- [ ] Rename readers with analytics ownership, e.g. `TenantKpisAnalyticsReader`.

## 3. Query handlers

- [ ] Implement `GetTenantKpisQueryHandler`.
- [ ] Implement `GetSalesReportQueryHandler`.
- [ ] Implement `GetOutletReportQueryHandler`.
- [ ] Ensure handlers return API models and do not expose persistence rows.

## 4. Feature reporting refactor

- [ ] Update `GetTenantKpisService` to call `QueryBus`.
- [ ] Update sales report flow to call `QueryBus`.
- [ ] Keep `OutletReportExportService` in features but source data from `core.analytics`.
- [ ] Remove `EntityManager` usage from `features.reporting`.

## 5. Export

- [ ] Keep simple CSV export in V1 if acceptable.
- [ ] Document future move to `platform.document` for report rendering/storage.
- [ ] Ensure export file endpoints are not wrapped as JSON if returning files.

## 6. Tests

- [ ] Unit test query handlers.
- [ ] Integration test report queries with fixture tickets/payout/settlement data.
- [ ] Test date range boundaries.
- [ ] Test feature reporting response mapping.
