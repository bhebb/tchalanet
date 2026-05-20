# OpenSpec — Platform Rules (78)

## Status

NORMATIVE once ADR-001 is Accepted.

## Definition

`platform` contains transversal application service modules that:

1. may have state, persistence, lifecycle, cache, events, or external adapters;
2. are consumed by multiple core/features modules;
3. do not own core business-critical invariants.

A wrong silent result belongs in `core` if it can cause direct financial loss, regulatory dispute, wrong winner, wrong payout, wrong draw/result, wrong settlement, or wrong limit decision.

## Naming

`platform/` as Java layer is distinct from `/api/v1/platform/**` as platform admin HTTP scope.

## Module shape

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

## Public API

- Other modules MAY depend on `platform.<capability>.api`.
- Other modules MUST NOT depend on `platform.<capability>.internal`.
- API models MUST be immutable records where possible.
- API must not expose JPA entities, repositories, Spring MVC request models, or provider clients.

## Transactions and context

- Platform services join caller transaction by default.
- Platform services may start a transaction when called from web/batch/scheduler/async.
- `platform.audit` may use `REQUIRES_NEW` for failure audit.
- Persistent idempotence may define reservation/commit transaction semantics.
- Tenant/actor-sensitive platform services MUST use `TchRequestContext` or an explicit system context.

## Dependencies

Allowed:

```text
platform -> common
platform -> catalog
```

Forbidden:

```text
platform -> core
platform -> features
platform.<a>.internal -> platform.<b>.internal
```

Direct `platform -> platform` imports are forbidden by default. Use events or an ADR exception.

## Events

- Platform may listen to core events.
- Platform may publish technical/application events.
- Core must not listen to platform events.
- If core needs to react to a platform event, revisit module ownership.

## Security / access control split

```text
common.security
  = technical Spring/security glue

platform.accesscontrol
  = application permissions, role assignment, policy checks
```

## Idempotence split

```text
common.idempotence
  = annotations, keys, interfaces

platform.idempotence
  = persistent records, workflow, replay protection
```

## Notification placement

Default target is `platform.notification`.

`core.notification` is forbidden unless an ADR proves it owns a core business-critical invariant.
