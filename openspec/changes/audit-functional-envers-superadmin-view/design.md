# Design

## Audit Families

### Functional Audit

Functional audit is the operator/business timeline.

Source of truth:

- table: `audit_event`
- API: `platform.audit.api.AuditApi`
- annotation: `@AuditLog`
- web endpoint: `GET /platform/audit/logs`
- web page: `/app/platform/ops/audit`

It answers:

- Who performed a meaningful action?
- On which business/system entity?
- Was the outcome success or failure?
- Which reason, request id, tenant, IP, user agent, or force flag was involved?

Functional audit is curated. It should not contain every column diff.

### Entity Revision History

Entity revision history is technical revision history backed by Hibernate Envers.

Source of truth:

- `revinfo`
- entity-specific `*_aud` tables
- `@Audited` entity annotations
- capability slice: `platform.entityhistory`

It answers:

- Which persisted fields changed?
- What was the revision number and revision type?
- What did the row look like at a prior revision?

Envers is not an operator action feed. It should be exposed through `platform.entityhistory`
read-only projections that resolve entity type, entity id, revision metadata and field diffs.

`platform.entityhistory` owns the Envers revision entity, revision listener, entity allowlist,
revision projections and API. `platform.audit` must not own Envers listeners or revision
metadata.

Initial exposed Envers allowlist:

- `SELLER_TERMINAL`: audited control/financial fields only, not full personal/contact data.
- `DRAW_RESULT`: result lifecycle and payload revisions.
- `LIMIT_ASSIGNMENT`: rule assignment revisions.

Entities outside this allowlist should not keep class-level `@Audited` annotations for now.
Fresh-database migrations should create only the `_aud` tables required by this allowlist.

## Current Functional Flow

```text
Web PlatformAuditPage
  -> PlatformAuditApi.listAuditEvents
  -> TchBackendClient.get('/platform/audit/logs')
  -> AuditEventRestController.getAuditLogs
  -> AuditApi.listAuditEvents
  -> AuditService.listAuditEvents
  -> AuditEventRepositoryAdapter.findByCriteria
  -> audit_event
```

`TchBackendClient` unwraps `ApiResponse<T>`, so the page expects the backend `data` payload as a `TchPage`.

## RLS Expectations

For `GET /api/v1/platform/audit/logs`, the request should bind:

- `app.api_scope = 'platform'`
- `app.is_super_admin = 'true'`
- `app.current_tenant = ''` unless a tenant override is explicitly used

With those values, `public.allow_platform_cross_tenant_select()` is true and the `audit_event_rls_select` policy allows all tenant/global functional audit rows.

## Short-Term Debug Plan

1. Confirm the API response directly as superadmin:

   ```http
   GET /api/v1/platform/audit/logs?page=0&size=20
   ```

2. Confirm whether data exists:

   ```sql
   select count(*) from audit_event;
   select tenant_id, entity_type, action, count(*)
   from audit_event
   group by tenant_id, entity_type, action
   order by count(*) desc;
   ```

3. Confirm what RLS sees for the same request:

   ```sql
   select
     current_setting('app.api_scope', true),
     current_setting('app.is_super_admin', true),
     current_setting('app.current_tenant', true),
     public.allow_platform_cross_tenant_select();
   ```

4. Trigger a known annotated action, for example cache clear, draw lifecycle, identity user change, notification lifecycle or platform public content update.

5. Reload `/app/platform/ops/audit` and verify one new `audit_event` row appears.

## Short-Term Fix

If `audit_event` is empty, keep the current page connected to functional audit and improve the UI copy:

- title: `Audit fonctionnel`
- empty state: clarify that Envers/entity revisions are not shown yet.
- add a small summary panel: functional audit source is `audit_event`; Envers is future work.

If `audit_event` has rows but the API returns none:

- add a backend integration test for `SUPER_ADMIN + ApiScope.PLATFORM` listing tenant and global rows through RLS.
- inspect `ResolvedAccessContext.superAdmin()` and `TchRequestContext.isSuperAdmin()` for the live user.
- add one targeted log/test around RLS variables for `/platform/audit/logs`.

If writes are missing after annotated actions:

- add an integration test that calls an annotated endpoint and verifies `audit_event` after success.
- capture actor/tenant at aspect time or extend `LogAuditEventRequest` with an explicit actor snapshot so after-commit writes do not depend on ambient context.

## Entity History Usage

Add a separate read-only capability surface, not a write path:

```text
platform.audit
  functional timeline only -> audit_event

platform.entityhistory
  technical revisions only -> revinfo + allowlisted *_aud projections
```

Implemented API shape:

```http
GET /platform/entity-history/revisions?entityType=SELLER_TERMINAL&entityId={idOrBusinessKey}
GET /platform/entity-history/revisions?entityType=DRAW_RESULT&entityId={idOrBusinessKey}
GET /platform/entity-history/revisions?entityType=LIMIT_ASSIGNMENT&entityId={idOrBusinessKey}
```

Recommended web shape:

- Operations menu can expose two clearly separated sources:
  - one `Audit` entry with tabs `Audit fonctionnel` and `Révisions techniques`; or
  - two entries, `Audit fonctionnel` and `Historique entités`.
- Functional tab remains action/outcome oriented.
- Revisions tab is entity oriented and requires `entityType`/`entityId` or a narrow
  recent-revisions query.
- Detail expansion shows metadata, changed fields and safe before/after values.

Recommended list contract:

```text
EntityRevisionItem {
  revisionId: string;
  entityType: string;
  entityId: string;
  operation: CREATE | UPDATE | DELETE;
  changedAt: string;
  changedBy?: string;
  tenantId?: string;
  changedFields?: string[];
  changedValues?: EntityRevisionFieldChange[];
}
```

Permissions:

- `audit.functional.read` for functional audit reads.
- `audit.entity_revision.read` for entity history reads.

## Guardrails

- Do not join arbitrary `*_aud` tables dynamically from browser input.
- Maintain an allowlist of Envers-enabled entities and their display metadata.
- Read through Hibernate `AuditReader` or backend-controlled projections.
- Apply superadmin/platform RLS plus the dedicated `audit.entity_revision.read` permission.
- Never expose raw `*_aud` tables to the browser.
- Pagination is mandatory.
- Never use Envers as proof that a sensitive action was authorized or reasoned; functional audit remains the compliance-facing action record.
