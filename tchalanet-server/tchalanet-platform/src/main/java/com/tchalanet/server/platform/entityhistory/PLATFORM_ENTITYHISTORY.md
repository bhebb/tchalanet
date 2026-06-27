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
- future read-only revision APIs and projections;
- future entity allowlists and field metadata for UI-safe revision history.

This module does not own:

- functional audit writes;
- `@AuditLog`;
- `audit_event`;
- raw `*_aud` exposure to browsers.

## Future HTTP Surface

```http
GET /api/v1/platform/entity-history/revisions?entityType=tenant&entityId=...
GET /api/v1/platform/entity-history/revisions/{revisionId}
```

The UI contract should use controlled projections, not raw Envers rows:

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
}
```

## Guardrails

- Maintain an allowlist of exposable entity types.
- Never expose raw `*_aud` tables to the browser.
- Use `AuditReader` or backend-controlled projections only.
- Pagination is mandatory.
- Gate reads with a dedicated permission such as `audit.entity_revision.read`.
