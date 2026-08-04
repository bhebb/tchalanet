# OpenSpec Change - Platform Analytics Reconciliation V1

## Why

The analytics projections feed tenant dashboards and reports, but a platform operator has no safe
surface to validate them against transactional ticket snapshots or to launch the narrowly scoped
repair defined in the backend reconciliation contract.

The repair is an operations capability, not a tenant-admin capability. It must remain deliberate,
auditable, tenant-scoped, and impossible to trigger by loading a page.

## What Changes

- Add a lazy platform route at `/app/platform/ops/analytics/reconciliation` and a matching
  Operations navigation entry.
- Add a data-access method to call `POST /platform/ops/analytics/reconciliation`.
- Render a target form (tenant, inclusive business-date window), a read-only `VALIDATE` action,
  and a detailed result surface.
- Put `REBUILD_AND_VALIDATE` behind an explicit confirmation dialog that requires a repair reason
  and exposes the selected tenant/date scope before sending the request.
- Render mismatches as a bounded operational table: projection, business date, draw, terminal,
  expected and observed financial totals. Identifiers are supplemental to the scope, not the
  primary label.
- Add a stable Playwright UI contract: platform operator opens the screen, selects a target and
  reaches the validate action. The browser test does not assert accounting math.

## Non-goals

- No tenant-admin or cashier route, button, or PageModel quick action.
- No automatic rebuild, periodic browser polling, or client-side aggregation of ticket data.
- No web implementation of audit history: V1 uses the result returned by the operation and the
  existing platform audit listing.

## Dependencies

- `tchalanet-server/openspec/changes/reconcile-approved-sales-analytics`: endpoint, result DTO,
  platform authorization, audit, source snapshot and exact rebuild behavior.
- `docs/conventions/feature-playbook.md`: platform operation shell, feedback and form primitives.
