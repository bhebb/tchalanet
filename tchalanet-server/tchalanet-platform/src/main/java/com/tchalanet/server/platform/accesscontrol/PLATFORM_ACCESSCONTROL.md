# Platform Capability `platform.accesscontrol`

## Role

`platform.accesscontrol` owns authorization policy:

- roles;
- permissions;
- tenant role assignments;
- effective permission resolution;
- permission decisions.

It answers: **may this actor attempt this action in this scope?**

It does not answer whether a target resource is in a valid business state.

## Public Surface

Consumers outside the capability use only:

```text
platform/accesscontrol/api/
```

Implementation stays private:

```text
platform/accesscontrol/internal/
```

Core/features/platform peers must never import `platform.accesscontrol.internal.*`.

## Deny-Safe Evaluation

Permission evaluation is deny-by-default when security facts are missing or ambiguous.

Required facts for tenant-scoped checks:

- authenticated actor;
- effective tenant from canonical context;
- requested permission;
- actor role/membership facts from platform-owned state.

Never trust tenant ids from request bodies as authorization source-of-truth.

## What AccessControl Does Not Do

Access control must not validate:

- payout status or payout eligibility;
- ticket cancellability or settlement state;
- terminal/outlet/session state;
- seller terminal assignment;
- offline sync business eligibility;
- limits, draw state, cutoff, or game availability.

Those checks belong to the owning `core` domain validators/handlers.

## Integration

HTTP controllers declare requirements using method security:

```java
@PreAuthorize("hasPermission('payout:approve')")
```

Non-HTTP or reusable application flows call `AccessControlApi`.

Role and permission write endpoints must be functionally audited through `platform.audit`.

## Persistence

Role, permission and assignment tables are owned by `platform.accesscontrol`.

Tenant-scoped rows must be RLS-compatible. Application queries must not use client-provided tenant
ids as the isolation source.

## Guardrails

- Platform must not depend on `core` or `features`.
- Accesscontrol must not import core/features domain packages.
- Permission checks deny when actor/tenant/permission facts are missing.
- Permission write endpoints are audited.
- Business invariants remain in core domains.
