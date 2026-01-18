# Audit (Envers + domain audit)

## Envers (DB-level history)

- JPA entities extending BaseEntity/BaseTenantEntity are `@Audited`.
- Revision listener: `TchRevisionListener` is the source of truth for actor/tenant metadata.

## Domain audit events

- Important actions SHOULD publish an audit event:
  - `AuditEvent` includes tenant, actor, operationType, entityRef, outcome, metadata.
- Use `DomainEventPublisher` and publish audit events `AfterCommit` when the transaction succeeded.

## AuditLog API

- Audit log is read-only and paginated.
- Admin scope by default (tenant admin sees own tenant; super admin sees all or via override).
- Filtering allowed: date range, entity type/id, actor, outcome.
