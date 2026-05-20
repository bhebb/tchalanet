# Platform Capability `platform.audit`

## Role

`platform.audit` owns functional audit: the history of business/user actions.

Functional audit is not the same thing as technical revision audit. Envers can tell us what row
changed; functional audit tells us which business action was attempted, by whom, in which tenant,
with which outcome.

## Functional Audit

Functional audit entries should capture:

- actor;
- tenant or effective tenant;
- action;
- target type and target id;
- outcome: success, denied, failure;
- request id and correlation id;
- safe details needed for investigation.

Functional audit is produced through `@AuditLog` or explicit `AuditApi` calls.

## Technical Revision Audit

Envers revision metadata is technical audit. It may include actor, tenant, request id and
correlation id, but it does not replace functional audit for sensitive business actions.

Examples:

- A denied action may have no row revision but still needs functional audit.
- A row update may have an Envers revision but still need a functional audit entry naming the
  business action.

## Transaction Rules

- Success audit should be emitted after the main business transaction commits.
- Failure or denied audit may use a separate transaction.
- Audit infrastructure failure must be logged/observed.
- Audit infrastructure failure must not turn an already committed business operation into a client
  failure.

## Public Surface

Consumers outside the capability use only:

```text
platform/audit/api/
```

Implementation stays private:

```text
platform/audit/internal/
```

Core and features inject `AuditApi`, never internal services/repositories.

## HTTP API

Audit endpoints are read-only search/read endpoints. Public HTTP writes to audit are forbidden.

Tenant-scoped reads must respect RLS/effective tenant. Platform override must be explicit and
auditable.

## Guardrails

- Functional audit persistence belongs to `platform.audit`, not `common.persistence`.
- Do not duplicate the same audit action in controller and handler unless they represent different
  actions.
- Envers is not a substitute for functional audit.
- Audit failures do not rollback successful business operations.
