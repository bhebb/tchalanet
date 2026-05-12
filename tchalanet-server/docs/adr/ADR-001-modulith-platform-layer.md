# ADR-001 — Introduce `platform` Layer in the Tchalanet Modulith

## Status

**Proposed — naming decision accepted**

The Java/backend layer name is accepted as `platform`.
The ADR remains Proposed until the first Spring Modulith + ArchUnit gates are implemented and passing with legacy allowlists.

## Context

Tchalanet currently distinguishes `common`, `catalog`, `core`, and `features`.
This is no longer enough.

Several capabilities are transversal and stateful, but they are neither:

- pure technical primitives (`common`),
- reference data (`catalog`),
- core lottery/money/game domains (`core`),
- nor UI/BFF orchestration (`features`).

Examples include audit, access control, tenant configuration, tenant theme resolution, user context, document generation, communication, notifications, and persistent idempotence.

Without a dedicated layer, those capabilities either pollute `core` or fatten `common`.

## Glossary

| Term                 | Definition                                                                                                            |
| -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Layer                | One of the top-level backend families: `common`, `catalog`, `platform`, `core`, `features`, `app`.                    |
| Module               | A Spring Modulith application module such as `core.sales`, `platform.audit`, `catalog.theme`, `features.tenantadmin`. |
| Platform capability  | A module under `platform.*`. It owns one transversal application service capability.                                  |
| Platform admin scope | HTTP scope `/api/v1/platform/**`; distinct from the Java `platform/` layer.                                           |

## Decision 1 — Use `platform` as the Java layer name

The Java/backend package and Maven module name is `platform`.

```text
com.tchalanet.server.platform.*
tchalanet-platform
```

Clarification:

```text
platform/ package layer != /api/v1/platform/** HTTP scope
```

Documentation MUST use:

- `platform layer` or `platform module` for Java/backend modules.
- `platform admin scope` for HTTP `/api/v1/platform/**` routes.

## Decision 2 — Define platform operationally

A module belongs in `platform` when all of these are true:

1. It is a transversal application capability used by multiple `core` and/or `features` modules.
2. It may have state, persistence, lifecycle, cache, events, or external adapters.
3. It does **not** own core business-critical invariants.

A component owns a **core business-critical invariant** when a silent wrong result during 24 hours could cause one of:

- direct financial loss,
- regulatory or contractual dispute,
- wrong game result, winner, payout, limit, settlement, draw, ticket, or monetary decision.

If yes, it belongs in `core`.

The concrete list of platform modules is a living reference, not part of this ADR. See:

```text
docs/reference/platform-modules.md
```

## Decision 3 — Platform module archetype

Each platform capability uses the same public/private shape:

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

Rules:

- `api/` is the public Java contract inside the monolith.
- `internal/` is private implementation.
- Other modules MUST NOT import `platform.<capability>.internal..`.
- CQRS is optional in `platform`.
- Hexagonal architecture is not mandatory in `platform`.
- A service + repository is acceptable when the capability is simple.

## Decision 4 — Transactions and context in platform

Default rule:

- Platform services join the caller transaction when called inside an existing transaction.
- Platform services may start their own transaction when called from web, batch, scheduler, or async flows.
- Platform services MUST use `TchRequestContext` when tenant, actor, audit, RLS, or locale matters.
- Platform services MUST support explicit system context for batch/scheduler/startup/outbox flows.

Exceptions:

- `platform.audit` may use `REQUIRES_NEW` for failure audit.
- Persistent idempotence may use transaction boundaries appropriate to reservation/commit semantics.
- Any other independent transaction requires an ADR or module README justification.

Example policy:

```java
// default: participates in caller transaction
@Transactional
public DocumentRecord createDocument(CreateDocumentRequest request) { ... }

// explicit exception: failure audit survives caller rollback
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logFailure(AuditFailureRecord record) { ... }
```

## Decision 5 — Dependency graph

Macro Maven dependencies:

```text
common      -> none
catalog     -> common
platform    -> common, catalog
core        -> common, catalog, platform
features    -> common, catalog, platform, core
app         -> common, catalog, platform, core, features
```

Spring Modulith logical dependency rules:

- `common` depends on no business/application module.
- `catalog` must not depend on `platform`, `core`, or `features`.
- `platform` must not depend on `core` or `features`.
- `core` must not depend on `features`.
- `features` are leaf modules; no module depends on `features`.
- Any module may only consume another module's public `api` / named interface.

Intra-platform rule:

- Direct `platform.* -> platform.*` imports are forbidden by default.
- Cross-platform communication should use events or a documented ADR exception.
- This avoids cycles such as `platform.audit <-> platform.identity`.

## Decision 6 — Events

Core domain events are owned by `core` modules.

Platform modules may publish technical/application events such as:

- `EmailSentEvent`
- `DocumentGeneratedEvent`
- `TenantConfigChangedEvent`
- `UserContextChangedEvent`

But:

- `core` modules MUST NOT listen to platform events.
- If a core module needs to react to an event, the event must be a core event or the design must be refactored.
- Platform may listen to core events to perform support side effects such as audit, notification, document generation, or communication.

Mechanical test:

```text
core..* must not contain @EventListener methods consuming platform..* events
```

## Decision 7 — Spring Modulith granularity

Each capability is its own Spring Modulith application module.

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

There is no single coarse `platform` Modulith module for all capabilities.

## Decision 8 — Maven module strategy

Maven is macro-level only at this stage:

```text
tchalanet-common
tchalanet-catalog
tchalanet-platform
tchalanet-core
tchalanet-features
tchalanet-app
```

Do not create one Maven module per capability or per core domain yet.
Spring Modulith + ArchUnit enforce inner module boundaries.

## Decision 9 — System scope

`SYSTEM` may exist as an internal execution scope for batch, scheduler, startup, outbox, retry, and non-HTTP automation.

Rules:

- `SYSTEM` is not a public HTTP route scope.
- `SYSTEM` does not bypass RLS automatically.
- Tenant-scoped system work must bind an explicit tenant context.
- Cross-tenant system work requires explicit job-level policy and audit.

## Consequences

Positive:

- `common` can remain a technical shared kernel.
- `core` can be reserved for critical business domains.
- Transversal application services have a stable home.
- Spring Modulith can verify package boundaries.
- Maven can compile macro areas independently.

Negative:

- The word `platform` has two meanings; documentation must be precise.
- Existing imports must be migrated gradually.
- Stronger gates require temporary legacy allowlists.
- Some migrations, especially user context and access control, will require multi-PR bridge steps.

## Acceptance criteria

This ADR can move to Accepted when:

- macro Maven modules exist,
- `platform` module exists,
- initial Spring Modulith verification exists,
- ArchUnit gates exist with explicit legacy allowlists,
- docs distinguish `platform layer` and `platform admin scope`,
- no new stateful transversal code is added to `common` or `core`.
