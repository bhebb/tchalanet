# OpenSpec — Global Modulith Rules (70)

> **Status**: PROPOSED until ADR-001 is accepted.

## 1. Global architecture

Tchalanet is a Modular Monolith verified by Spring Modulith and ArchUnit.

Top-level layers:

```text
common
catalog
<asl>      # Application Service Layer; final package name pending
core
features
```

## 2. Terminology

- Layer: top-level architectural family.
- Module: Spring Modulith application module such as `core.sales` or `<asl>.audit`.
- Capability: module inside `<asl>`.
- API: public Java surface.
- Internal: implementation hidden from other modules.

## 3. Dependency graph

```text
common
  ↑
catalog
  ↑
<asl>
  ↑
core
  ↑
features
```

Precise allowed dependencies:

| From | May depend on |
|---|---|
| common | external libraries only |
| catalog | common |
| `<asl>` | common, catalog api |
| core | common, catalog api, `<asl>` api, other core api where allowed |
| features | common, catalog api, `<asl>` api, core api |

## 4. Internal package rule

No module may import another module's `internal..` package.

## 5. Feature leaf rule

No project module outside `features` may import `features..` application types.

Features expose HTTP contracts, not Java APIs.

## 6. Platform HTTP scope distinction

`/api/v1/platform/**` is an HTTP scope for platform administration/SUPER_ADMIN. It is not automatically related to the Application Service Layer package name.

## 7. Verification

- Spring Modulith verifies module dependencies and named interfaces.
- ArchUnit verifies layer-specific rules and forbidden patterns.
- Maven starts as macro modules; Spring Modulith owns fine-grained boundaries.
