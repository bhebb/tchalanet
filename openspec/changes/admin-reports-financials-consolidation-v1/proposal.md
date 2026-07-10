# Admin Reports Financials Consolidation

## Why

Tenant-admin financial analytics are report surfaces. The web route already exposes them under
`/reports/financials`, but code was split between a standalone `financials` feature and backend
`tenantadmin.financials`.

## What Changes

- Move tenant-admin financial BFF code under `features.reporting`.
- Move admin-portal financials page and API service under `features/reports`.
- Remove the doc-only legacy `features.stats` feature marker now that reporting uses
  `core.analytics`.
- Preserve existing public API and web routes.

## Impact

- No financial calculation changes.
- No database changes.
- Existing `/admin/financials/*` backend paths remain available.
