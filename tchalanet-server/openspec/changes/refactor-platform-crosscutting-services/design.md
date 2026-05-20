# Design — Platform Cross-cutting Consolidation

## Architecture Position

`platform.accesscontrol`, `platform.audit` and `platform.idempotence` are separate platform
capabilities. They share conventions but do not collapse into one generic cross-cutting module.

```text
platform/<capability>/
  api/       public Java API and annotations/models
  internal/  service, persistence, web, adapters, aspects
```

Allowed dependency direction:

```text
common <- platform <- core <- features
catalog <- platform
```

`platform` may depend on `common` and catalog APIs. It must not depend on `core` or `features`.

## Capability Boundaries

| Concern | Owner | Must not do |
|---|---|---|
| Permission decision | `platform.accesscontrol` | Validate business state or resource lifecycle |
| Functional audit | `platform.audit` | Replace Envers or technical revision audit |
| Envers revision metadata | `platform.audit.internal.persistence` | Record user-facing business action by itself |
| HTTP idempotency | `platform.idempotence` | Replace domain invariants, locks or uniqueness |
| Processed event idempotence | `platform.idempotence` | Accept handler keys from clients |
| JPA base fields/listeners | `common.persistence` | Persist functional audit/access/idempotency records |

## Access Control

Access control answers: "May this actor attempt this action in this tenant/scope?"

It does not answer:

- Is this payout payable?
- Is this ticket cancellable?
- Is this terminal active?
- Is this session open?
- Does this seller assignment still match?

HTTP controllers express permission requirements with method security (`@PreAuthorize` or
capability-specific meta-annotations). Non-HTTP or reusable use cases call `AccessControlApi`.

Security-sensitive requirements:

- Deny by default when context, tenant or actor facts are missing.
- Super-admin override must be explicit and auditable.
- Effective permissions may be cached, but revocation must have a clear invalidation path.
- Tenant-scoped permission rows must be tenant-safe and RLS-compatible.

## Audit

Two audits coexist and must stay distinct.

### Functional Audit

Functional audit records business/user actions:

- actor;
- tenant/effective tenant;
- action;
- target type/id;
- outcome: success, denied, failure;
- request/correlation ids;
- details safe for audit.

Functional audit is triggered by `@AuditLog` or explicit `AuditApi` calls.

Rules:

- Success audit should be written after the business transaction commits.
- Denied/failure audit should be written in a separate transaction.
- Audit infrastructure failure must not turn a committed business operation into a client failure.
- Do not audit the same action in both controller and handler unless the two entries represent
  different actions.

### Technical Revision Audit

Envers captures row-level changes and revision metadata. It is useful for forensic diffing, not for
answering "who performed this business action and why?".

Rules:

- Envers revision metadata may include actor/tenant/request/correlation.
- Envers is not a substitute for required functional audit.
- Functional audit tables belong to `platform.audit`, not `common.persistence`.

## Idempotence

Idempotence has two independent surfaces.

### HTTP Idempotency

HTTP idempotency protects client retries for write endpoints where duplicate execution is dangerous.

Expected behavior:

- missing `Idempotency-Key` where required -> `400 idempotency.missing`;
- same key + same payload + completed -> replay same resource/result semantics;
- same key + different payload -> `409 idempotency.payload_mismatch`;
- same key + in progress -> `409 idempotency.in_progress`;
- expired record -> explicit expiry behavior, not silent unsafe re-execution.

The request hash must be canonical enough to avoid false mismatches from irrelevant formatting.

### Processed Event Idempotence

Processed-event idempotence protects event consumers/projectors/listeners.

Rules:

- handler keys are stable constants;
- event ids are source facts, not generated per delivery attempt;
- duplicate `(tenant, handler_key, event_id)` skips side effects;
- absence of event id must be treated deliberately, usually with a correlation/dedupe key fallback.

## Integration Map

| Integration point | Owner | Notes |
|---|---|---|
| `@PreAuthorize` | Web/controller | Declares permission requirement |
| `TchPermissionEvaluator` | accesscontrol adapter/config | Delegates to `AccessControlApi` or internal service |
| `@AuditLog` | `platform.audit` | Declares functional audit |
| Envers revision listener | `platform.audit.internal` | Adds technical revision metadata |
| `@RequireIdempotency` | `platform.idempotence` | Applies HTTP idempotency |
| `ProcessedEventPort` | `platform.idempotence.api` | Used by listeners such as notification |

## Verification Strategy

This change should prefer focused tests:

- access control allow/deny and tenant isolation;
- audit success after commit and failure/denied in separate transaction;
- audit failure isolation from successful business operation;
- HTTP idempotency replay, mismatch and in-progress cases;
- processed-event duplicate skip;
- ArchUnit package privacy and dependency boundaries.
