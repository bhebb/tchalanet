# ADR-001 — Modular Monolith + Platform Layer

## Status

Proposed — must be reviewed before marking `Accepted`.

## Context

Tchalanet currently has four documented backend families:

- `common` — shared technical kernel;
- `catalog` — reference/read-mostly data;
- `core` — critical business domains;
- `features` — UI/BFF vertical slices.

Several existing components do not fit cleanly in these four families:

- audit;
- access control;
- tenant user / user context;
- tenant configuration;
- tenant theme;
- document generation/storage;
- communication delivery;
- idempotence;
- security.

Keeping those components in `core` pollutes `core` with non-core business domains. Moving them to `common` would make `common` stateful and application-specific, which violates its role as a thin technical shared kernel.

This ADR therefore does two things:

1. introduces a fifth backend layer: `platform`;
2. formalizes backend module archetypes under one global modular-monolith architecture.

## Terminology decision

Tchalanet adopts **Spring Modulith as a framework** for module boundary verification and documentation, not merely “modulith” as an informal idea.

Required implementation artifacts:

- application module declarations through package-level metadata where useful;
- named interfaces for public APIs (`api` packages);
- module dependency verification tests;
- ArchUnit rules for architectural constraints not covered by Spring Modulith.

## Decision

Tchalanet is a **Modular Monolith using Spring Modulith**.

Each module belongs to exactly one archetype:

```text
common   = Technical Shared Kernel
catalog  = Simple DDD / Reference Catalog
platform = Technical/Application Service Module
core     = Clean Architecture / Hexagonal / CQRS Domain
features = Vertical Slice / BFF Leaf Module
```

## Operational definition of `platform`

`platform/` hosts transversal application capabilities that satisfy these criteria:

1. the capability is reused by multiple `core` domains and/or `features`;
2. the capability has persistent state, lifecycle, workflow, external integration, or application-level policy;
3. the capability does **not** own critical Tchalanet business invariants such as money movement, ticket lifecycle, draw lifecycle, payout rules, result settlement, sales eligibility, or limit computation;
4. the capability is not a read-mostly reference dataset;
5. the capability is not a stateless low-level technical primitive.

### Inclusion/exclusion test

Ask these questions in order:

| Question | Destination |
|---|---|
| Is it a stateless technical primitive or interface used everywhere? | `common` |
| Is it read-mostly reference/lookup data with no lifecycle? | `catalog` |
| Does it decide money, winners, tickets, payouts, limits, draw lifecycle, settlement, or other critical business truth? | `core` |
| Is it a UI screen, flow, dashboard, or BFF aggregation? | `features` |
| Is it stateful/transversal/application-support behavior reused by multiple modules? | `platform` |

If the answer is still unclear, do not create a package. Add a short ADR or OpenSpec note before implementation.

## Platform archetype

Every platform capability follows this shape:

```text
platform/<capability>/
  api/
    XxxApi.java
    model/
      XxxRequest.java
      XxxResult.java
      XxxView.java

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

- `api/` is the only Java surface consumable by other modules.
- `internal/` is private to the capability.
- `service/` owns the application behavior.
- `persistence/` is allowed for platform-owned tables/entities.
- `web/` is allowed for admin/ops endpoints.
- `event/` is allowed for technical/integration events.
- CQRS and hexagonal layering are optional inside `platform`; do not force them by default.

## Dependency matrix

Allowed dependencies:

```text
common   -> none
catalog  -> common
platform -> common, catalog
core     -> common, catalog, platform.api
features -> common, catalog.api, platform.api, core.api
app      -> common, catalog, platform, core, features
```

Forbidden dependencies:

```text
common   -> catalog/platform/core/features
catalog  -> platform/core/features
platform -> core/features
core     -> features
features -> features internals of another feature by default
any      -> another module's internal package
any      -> another module's persistence package
```

`features` are leaf modules. They expose HTTP contracts, not Java APIs.

## Platform events

Platform modules may publish **technical/application integration events**, for example:

- `EmailSentEvent`;
- `DocumentGeneratedEvent`;
- `TenantConfigChangedEvent`;
- `UserContextUpdatedEvent`.

Platform modules must not publish events that represent core business facts, for example:

- `TicketSoldEvent`;
- `PayoutApprovedEvent`;
- `DrawResultAppliedEvent`;
- `LimitBreachedEvent`.

Those facts belong to `core`.

Platform event rules:

- events must use typed IDs;
- events must carry tenant/user/correlation data when needed for async/retry;
- events must not expose JPA entities;
- event listeners must be idempotent when retryable;
- cross-module side effects must happen after commit when triggered by transactional work.

## Component placement decisions

| Current / candidate | Target | Decision |
|---|---|---|
| `core.audit` | `platform.audit` | Audit is transversal compliance/traceability, not a core lottery domain. |
| `core.accesscontrol` | `platform.accesscontrol` | Permissions/role checks are application-support policy. Domain eligibility still remains in owning core. |
| `core.tenantuser` | `platform.usercontext` | App user profile/context is transversal support state. |
| `core.tenantconfig` | `platform.tenantconfig` | Effective tenant settings/overrides are transversal support state. |
| `core.tenanttheme` | `platform.tenanttheme` | Effective tenant theme/overrides are platform support. Global presets remain catalog. |
| document workflows | `platform.document` | Document generation/storage/delivery metadata is transversal application service. Pure render helpers may remain common only if stateless. |
| communication delivery | `platform.communication` | Email/SMS/push delivery, routing, templates, delivery status are transversal application service. |
| idempotence persistence/workflow | `platform.idempotence` | Stateful idempotency records/workflows are platform. Small annotations/interfaces may remain common. |
| security technical glue | `common.security` | Spring/security primitives stay common. |
| security application decisions | `platform.accesscontrol` | Permission policies and role assignments move to platform. |
| theme presets | `catalog.theme` | Read-mostly preset reference data. |
| setting definitions | `catalog.settings` | Read-mostly metadata/default definitions. |
| sales/draw/payout/limits/ticket lifecycle | `core.*` | Critical business truth. |

## Migration strategy

Migration must be incremental. Do not batch unrelated migrations.

### Freeze rule

After this ADR is accepted:

- no new transversal stateful/application capability may be added to `common`;
- no new non-core transversal capability may be added to `core`;
- new work must target `platform` unless a documented exception exists.

### Recommended order

1. **Create Maven and Modulith baseline**
   - macro Maven modules;
   - Spring Modulith tests;
   - ArchUnit gates;
   - no behavior move yet.

2. **Create `platform` skeleton**
   - root package;
   - package-info metadata;
   - README and package archetype;
   - no mass migration yet.

3. **Low-risk platform services first**
   - `platform.communication`;
   - `platform.document`;
   - `platform.audit` if it has limited core imports.

4. **Common defattening**
   - move stateful document/communication/idempotence workflows out of common;
   - keep only stateless technical primitives in common.

5. **High-impact transversal migrations**
   - `core.accesscontrol` -> `platform.accesscontrol`;
   - `core.tenantuser` -> `platform.usercontext`;
   - `core.tenantconfig` -> `platform.tenantconfig`;
   - `core.tenanttheme` -> `platform.tenanttheme`.

6. **Core API alignment**
   - create `core.<domain>.api.command/query/event/model`;
   - move handlers/domain/infra under `internal`;
   - update features to consume APIs only.

7. **Remove compatibility shims**
   - no deprecated imports;
   - no temporary package aliases;
   - no ArchUnit allowlists without ADR references.

### Verification after each migration

Each migration PR must run:

```bash
./mvnw verify
```

When the touched module is known, also run the narrow module verification, for example:

```bash
./mvnw -pl tchalanet-platform,tchalanet-core,tchalanet-features,tchalanet-app -am test
```

Each PR must state:

- source package(s);
- target package(s);
- public API exposed;
- imports changed;
- routes preserved or intentionally changed;
- RLS/context impact;
- Flyway impact;
- ArchUnit/Modulith exceptions added or removed.

## Required tests and gates

This ADR requires adding or updating tests for:

- Spring Modulith module verification;
- no import of another module's `.internal` package;
- no dependency from `platform` to `core` or `features`;
- no dependency from `catalog` to `platform`, `core`, or `features`;
- no dependency from `common` to app/business modules;
- features are leaf modules;
- core application/domain do not depend on infra;
- UUID is forbidden outside allowed persistence/infra zones;
- no repositories/entities are consumed outside owning internal packages.

If a rule cannot pass immediately due to legacy code, add:

- an explicit temporary allowlist;
- a TODO with target migration issue/spec;
- a removal condition.

## Consequences

### Positive

- `common` remains thin and technical.
- `core` is protected for critical business domains.
- `platform` becomes the explicit home for transversal stateful/application services.
- `features` remain leaf BFF/UI modules.
- Module dependencies become testable.
- Agents and developers get a placement decision matrix.

### Negative / Cost

- There are multiple internal archetypes to learn.
- Migration will break many imports, especially around `tenantuser`, `accesscontrol`, and `tenantconfig`.
- Compatibility shims may be needed temporarily.
- Spring Modulith and ArchUnit tests must be maintained.
- PRs touching moved components must update docs/specs/tests, not just packages.

## Non-goals

- Do not force every platform component into Clean Architecture/CQRS.
- Do not create one Maven module per capability at this stage.
- Do not expose features as Java APIs.
- Do not move critical business decisions from `core` to `platform`.
- Do not use `platform` as a dumping ground for unclear code.
