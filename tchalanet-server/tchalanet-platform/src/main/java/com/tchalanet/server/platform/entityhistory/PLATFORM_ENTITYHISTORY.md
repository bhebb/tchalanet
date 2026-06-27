# Platform Capability `platform.entityhistory`

## Role

`platform.entityhistory` owns technical entity revision history.

This capability is separate from `platform.audit`:

- `platform.audit` records functional/operator actions in `audit_event`.
- `platform.entityhistory` reads and annotates Hibernate Envers revisions in `revinfo` and
  allowlisted `*_aud` tables.

## Scope

This module owns:

- the Envers revision entity mapped to `revinfo`;
- the Envers revision listener that copies request context into revision metadata;
- read-only revision APIs and backend-controlled projections;
- entity allowlists and field metadata for UI-safe revision history.

This module does not own:

- functional audit writes;
- `@AuditLog`;
- `audit_event`;
- raw `*_aud` exposure to browsers.

## HTTP Surface

```http
GET /api/v1/platform/entity-history/revisions?entityType=SELLER_TERMINAL&entityId=...
GET /api/v1/platform/entity-history/revisions?entityType=DRAW_RESULT&entityId=...
GET /api/v1/platform/entity-history/revisions?entityType=LIMIT_ASSIGNMENT&entityId=...
```

`entityId` can be the technical UUID or a supported business lookup key for the selected entity
type. `DRAW_RESULT` supports business-oriented lookup by slot/date where the projection supports it.

The UI contract uses controlled projections, not raw Envers rows:

```text
EntityRevisionItem {
  revisionId
  entityType
  entityId
  operation
  changedAt
  changedBy
  tenantId
  changedFields
  changedValues
}
```

## Current Allowlist

Only these entity families are exposed:

- `SELLER_TERMINAL`
- `DRAW_RESULT`
- `LIMIT_ASSIGNMENT`

Do not add an entity to the UI/API dropdown just because a `_aud` table exists. Add it only after
reviewing the entity fields, data sensitivity, expected volume, and useful business search keys.

## Guardrails

- Maintain an allowlist of exposable entity types.
- Never expose raw `*_aud` tables to the browser.
- Use `AuditReader` or backend-controlled SQL/JPA projections only.
- Pagination is mandatory.
- Gate reads with a dedicated permission such as `audit.entity_revision.read`.
- Envers revisions complement functional audit; they are not proof of operator intent,
  authorization, or reason.
