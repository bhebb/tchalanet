# Tchalanet — Modular Monolith Architecture

## Decision

Tchalanet uses a Modular Monolith implemented with Spring Modulith and guarded by ArchUnit.

The architecture is unified by module boundaries, not by forcing the same internal pattern everywhere.

## Module archetypes

```text
common   = Technical Shared Kernel
catalog  = Simple DDD / Reference Catalog
platform = Technical/Application Service Module
core     = Clean Architecture / Hexagonal / CQRS Domain
features = Vertical Slice / BFF Leaf Module
```

## Dependency matrix

```text
common   -> none
catalog  -> common
platform -> common, catalog
core     -> common, catalog, platform.api
features -> common, catalog.api, platform.api, core.api
app      -> common, catalog, platform, core, features
```

Forbidden:

```text
any      -> another module's internal package
common   -> catalog/platform/core/features
catalog  -> platform/core/features
platform -> core/features
core     -> features
features -> another feature internals by default
```

## Public surfaces

| Root | Public Java surface |
|---|---|
| `common` | technical shared primitives only |
| `catalog` | `catalog.<name>.api` |
| `platform` | `platform.<capability>.api` |
| `core` | `core.<domain>.api` |
| `features` | none by default; HTTP/OpenAPI only |

## Platform definition

`platform` is for stateful or workflow-oriented transversal capabilities that support the application but do not own core lottery/money/ticket/draw/payout/limit business truth.

Examples:

```text
platform.audit
platform.accesscontrol
platform.usercontext
platform.tenantconfig
platform.tenanttheme
platform.document
platform.communication
platform.idempotence
```

## Placement decision

| Question | Destination |
|---|---|
| Stateless technical primitive? | `common` |
| Read-mostly reference data? | `catalog` |
| Critical business invariant/lifecycle? | `core` |
| UI/BFF flow? | `features` |
| Stateful transversal application service? | `platform` |

## Verification

Every migration must run:

```bash
./mvnw verify
```

Architecture tests must include Spring Modulith verification and ArchUnit gates.
