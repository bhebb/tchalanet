# Audit functional + Envers visibility for superadmin

## Why

The superadmin web surface exposes a platform audit page, but functional audit rows currently do not appear for the operator. At the same time, the backend has two different audit mechanisms:

- `platform.audit` functional audit, stored in `audit_event`, intended for human/operator traceability of important business or administrative actions.
- Hibernate Envers technical revision audit, stored in `revinfo` and `*_aud` tables, intended for entity-level change history and reconstruction.

Today the web page reads only functional audit events from `GET /platform/audit/logs`. Envers revisions are not exposed through the web UI or a platform API.

## What

- Document the two audit families and their responsibilities.
- Debug why the superadmin functional audit list can render empty.
- Define a short-term fix for the functional audit page.
- Define a future `platform.entityhistory` design for using Envers without mixing it into
  `platform.audit` writes.

## Impact

- Backend: `platform.audit`, new `platform.entityhistory`, RLS context, audit API, optional future
  Envers read model.
- Web: superadmin audit page and eventual audit source tabs/filters.
- Ops/compliance: clearer distinction between action timeline and entity revision history.

## Non-goals

- Do not replace `platform.audit` with Envers.
- Do not write functional audit rows from Envers revision listeners.
- Do not expose raw `*_aud` tables directly to the browser.
- Do not add broad audit coverage to every endpoint in this change.
- Do not keep Envers revision listeners or revision metadata under `platform.audit`.

## Initial Findings

- `PLATFORM_AUDIT.md` defines `platform.audit` as functional audit only and explicitly excludes
  Envers.
- Envers revision metadata and listeners now belong to the `platform.entityhistory` slice.
- The current web page calls `PlatformAuditApi.listAuditEvents(...)`, which calls `GET /platform/audit/logs`.
- `TchBackendClient.get<T>()` unwraps `ApiResponse<T>`, so the page is not failing because of the response envelope.
- `AuditEventRestController` returns `ApiResponse<TchPage<AuditEventResponse>>` and is guarded by `hasRole('SUPER_ADMIN')`.
- `audit_event` RLS allows cross-tenant select when `app.is_super_admin = true` and `app.api_scope = 'platform'`.
- `/api/v1/platform/**` resolves to `ApiScope.PLATFORM`, so a correctly resolved superadmin context should see tenant and global functional audit events.
- Docker was not available locally during this investigation, so the live database could not be queried to confirm whether `audit_event` is empty or filtered.

## Current Hypotheses

1. No functional audit rows exist in the runtime database yet, because most observed changes are Envers-only or the operator has not executed an annotated functional action.
2. Functional rows are written, but RLS context is not set as expected for the superadmin request in this runtime.
3. Functional rows are written only for specific annotated endpoints; the page is working but expectations currently include Envers revisions, which are not surfaced.
4. Some success audits may be skipped when no transaction synchronization is active or when the context is not available at write time; this needs a focused integration test.
