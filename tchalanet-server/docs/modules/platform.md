# Platform Layer

## Purpose

`platform` hosts transversal application service modules that are stateful or lifecycle-bearing but do not own core business-critical invariants.

Examples:

```text
platform.audit
platform.accesscontrol
platform.identity
platform.tenantconfig
platform.tenanttheme
platform.document
platform.communication
platform.notification
platform.idempotence
```

## Naming distinction

```text
platform/ package layer
  = Java/backend layer for transversal service modules

/api/v1/platform/**
  = HTTP platform admin scope
```

## Standard shape

```text
platform/<capability>/
  api/
    XxxApi.java
    model/
  internal/
    service/
    persistence/
    web/
    event/
    adapter/
    cache/
    config/
```

## Rules

- Expose only `api/` to other modules.
- Hide all implementation under `internal/`.
- Do not depend on `core` or `features`.
- Do not import another platform capability directly by default.
- Use events or documented ADR exceptions for intra-platform collaboration.
- CQRS is optional.
- Hexagonal architecture is not required.
- Use explicit transactions and context rules from ADR-001.
