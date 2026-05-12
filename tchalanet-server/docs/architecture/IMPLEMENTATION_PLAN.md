# Implementation Plan — Maven Modules, Platform, Modulith, Migration

## Guiding rule

No big-bang migration. Create boundaries first, add gates, then migrate one capability at a time.

## Phase 0 — Freeze decisions

- Keep Java/backend layer name: `platform`.
- Document distinction between `platform layer` and `platform admin scope`.
- Stop adding stateful transversal code to `common`.
- Stop adding non-core transversal modules to `core`.

## Phase 1 — Create macro Maven modules

Target structure:

```text
tchalanet-server/
  pom.xml
  tchalanet-common/
  tchalanet-catalog/
  tchalanet-platform/
  tchalanet-core/
  tchalanet-features/
  tchalanet-app/
```

Dependency graph:

```text
common      -> none
catalog     -> common
platform    -> common, catalog
core        -> common, catalog, platform
features    -> common, catalog, platform, core
app         -> all
```

Commands:

```bash
./mvnw -pl tchalanet-common verify
./mvnw -pl tchalanet-platform -am verify
./mvnw -pl tchalanet-core -am verify
./mvnw -pl tchalanet-app -am verify
./mvnw clean verify
```

Rule:

- targeted validation is allowed during development;
- `./mvnw clean verify` is mandatory before merge.

## Phase 2 — Create `platform` module

Create package root:

```text
com.tchalanet.server.platform
```

Initial capabilities:

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

Each capability follows:

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

## Phase 3 — Add gates before heavy migration

Add Spring Modulith verification and ArchUnit gates with legacy allowlists.

Required gates:

- no cross-module import of `..internal..`,
- `platform` must not depend on `core` or `features`,
- `catalog` must not depend on `platform`, `core`, or `features`,
- `features` are leaves,
- `common` is independent,
- controllers do not access repositories,
- raw UUID is forbidden outside persistence,
- core does not listen to platform events.

## Phase 4 — Defatten common

Keep in common:

```text
bus primitives
Typed IDs
context primitives
paging primitives
problem/exception primitives
validation primitives
cache abstractions
security technical glue
idempotence annotations/key primitives
AfterCommit/transaction helpers
```

Move or split:

```text
document workflow/state       -> platform.document
communication workflow/state  -> platform.communication
persistent idempotence        -> platform.idempotence
permission decisions          -> platform.accesscontrol
```

## Phase 5 — Migrate transversal core modules

Recommended order:

1. `platform.document` and `platform.communication` if currently in common or scattered.
2. `core.audit` -> `platform.audit`.
3. `core.tenanttheme` -> `platform.tenanttheme`.
4. `core.tenantconfig` -> `platform.tenantconfig`.
5. `core.accesscontrol` -> `platform.accesscontrol`.
6. `core.tenantuser` -> `platform.identity`.
7. `core.notification` -> `platform.notification` if it exists and is not business-core.

High fan-in migration pattern:

```text
PR 1: Create platform.<x>.api bridge delegating to legacy core.<x>.
PR 2: Flip consumers to platform.<x>.api progressively.
PR 3: Move implementation to platform.<x>.internal and delete legacy core.<x>.
```

Use the bridge pattern for `tenantconfig`, `accesscontrol`, and `tenantuser/identity`.

## Phase 6 — Align core to Modulith API/internal

For each `core.<domain>`:

```text
core/<domain>/
  api/
    command/
    query/
    event/
    model/
  internal/
    domain/
    application/
    infra/
```

Public core API contains:

- commands,
- queries,
- public integration events,
- read/result models.

Internal core contains:

- aggregates,
- domain services,
- handlers,
- ports,
- JPA entities,
- repositories,
- controllers,
- batch/cache/adapters.

## Phase 7 — Keep features as leaf modules

`features` exposes HTTP contracts, not Java APIs.

Pattern:

```text
features/<feature>/<slice>/
  web/
  app/
  model/
  mapper/
```

Rules:

- No `features.<x>.api` by default.
- No module imports `features`.
- Shared reusable behavior moves to `core`, `platform`, `catalog`, or `common`.

## Phase 8 — Remove allowlists and enforce final gates

Migration is done when:

- no `core.audit`, `core.accesscontrol`, `core.tenantuser`, `core.tenantconfig`, `core.tenanttheme`, or `core.notification` classes remain,
- no stateful document/communication/idempotence workflow remains in `common`,
- all `..internal..` imports from other modules are gone,
- Spring Modulith verification passes,
- ArchUnit passes,
- `./mvnw clean verify` passes.
