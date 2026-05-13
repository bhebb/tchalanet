# Design — Operational Context and Identity

## Target packages

```text
common.context
common.web.context
platform.identity
platform.accesscontrol
platform.operationalcontext   # only if reusable resolver is promoted
```

## Boundary rules

### common.context

Owns technical/runtime context:

- `TchRequestContext`
- `TchContext`
- `TchContextResolver`
- request/thread binding primitives
- batch/startup context binding primitives

It must not depend on `platform`, `core`, `catalog`, or `features`.

### platform.identity

Owns persisted user facts:

- app user;
- profile;
- preferences;
- tenant membership;
- identity-provider mapping;
- user bootstrap.

It does not own request context, operational context, or permissions.

### platform.accesscontrol

Owns permission/role checks and assignments.

### operational context

Composes runtime facts to validate an operation frame.

For seller/POS flows, resolution may depend on:

- identity membership;
- access-control permissions;
- terminal state;
- outlet state;
- session state.

## Rule of placement

If a context object is persisted user data, place it in `platform.identity`.
If it exists only for the current request/job/use case, keep it in `common.context` or a resolver/use-case-specific package.
If it validates a terminal/outlet/session frame, it is operational context, not identity.
