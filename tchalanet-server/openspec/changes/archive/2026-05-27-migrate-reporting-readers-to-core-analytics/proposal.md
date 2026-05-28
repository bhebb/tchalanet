# Change: migrate-reporting-readers-to-core-analytics

## Status

Proposed.

## Summary

Keep `features.reporting` as the UI/export/reporting feature, but move critical KPI/report SQL readers into `core.analytics`.

Current legacy reporting code includes direct SQL readers under `features.reporting`:

- `features.reporting.tenantkpis.TenantKpisReader`
- `features.reporting.salesreport.SalesReportReader`

These readers query sales/ticket tables directly and compute KPIs. They should be owned by `core.analytics`, then exposed through stable queries.

## Why

`features.reporting` should not own critical analytics SQL, source-of-truth reads, or metric definitions. Its responsibility is:

- web endpoints;
- request/response DTOs;
- report/export orchestration;
- CSV/PDF generation;
- calling `core.analytics` queries through `QueryBus`.

Metric definitions and critical SQL belong to `core.analytics`.

## Scope

### In scope

- Move KPI and report SQL readers from `features.reporting` to `core.analytics.internal.infra.persistence`.
- Expose report/KPI queries through `core.analytics.api.query`.
- Update `features.reporting` services/controllers to use `QueryBus`.
- Keep export generation in `features.reporting`.
- Prepare future integration with `platform.document` for PDF/CSV generation if needed.

### Out of scope V1

- Full report designer.
- Async large report jobs.
- Persistent report archive.
- Advanced BI filters.

## Target split

```text
core.analytics
  api/query
    GetTenantKpisQuery
    GetSalesReportQuery
    GetOutletReportQuery
  api/model
    TenantKpisView
    SalesReportLine
    OutletReportLine
  internal/infra/persistence
    TenantKpisAnalyticsReader
    SalesReportAnalyticsReader
    OutletReportAnalyticsReader

features.reporting
  tenantkpis/
    web/
    model/
    mapper/
  salesreport/
    web/
    export/
    model/
  outletreport/
    web/
    export/
    model/
```

## Rules

- `features.reporting` must not use `EntityManager` for critical business metrics.
- `features.reporting` must not query sales tables directly for KPI definitions.
- `features.reporting` may generate CSV/PDF/export output from data returned by `core.analytics`.
- `core.analytics` query models and result models must use typed IDs where applicable.
- Tenant-scoped queries must respect RLS/context rules.

## Current code issues to fix

### Raw UUID criteria

Current `TenantKpisReader.computeTenantKpis(UUID tenantId, ...)` uses raw UUID. Move to core analytics and expose typed query:

```java
public record GetTenantKpisQuery(
    TenantId tenantId,
    LocalDate fromDate,
    LocalDate toDate
) implements Query<TenantKpisView> {}
```

### Direct tenant filtering

Current SQL uses `where t.tenant_id = :tenantId`. For tenant-scoped reads, prefer RLS and context-driven tenant where possible. If the query is platform/batch/cross-tenant or deliberately scoped by tenant, document why explicit tenant is used.

### Date/time boundaries

LocalDate filters must be converted into correct tenant-zone instants for timestamp columns where source data is stored as `Instant/timestamptz`. Avoid ambiguous LocalDate vs timestamp comparisons.

## Rollout

1. Add core analytics query models and handlers.
2. Move SQL readers under `core.analytics.internal.infra.persistence`.
3. Update reporting services to call QueryBus.
4. Keep response DTOs in features.reporting.
5. Add tests comparing old vs new responses.
