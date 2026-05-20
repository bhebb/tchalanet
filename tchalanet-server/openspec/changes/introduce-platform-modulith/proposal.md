# Change Proposal — Introduce Platform Layer and Modulith Boundaries

## Problem

Transversal application capabilities currently drift into `common` or `core`.
This makes `common` too fat and pollutes `core` with non-core domains.

## Proposed solution

Introduce a `platform` layer and Maven module.

```text
common     = technical shared kernel
catalog    = reference catalogs
platform   = transversal application service modules
core       = critical business domains
features   = UI/BFF leaf modules
app        = runtime assembly
```

## Final naming decision

Use `platform` as the Java/backend layer name.

Clarify in documentation:

```text
platform/ package layer != /api/v1/platform/** platform admin HTTP scope
```

## Scope

- Add macro Maven modules.
- Add `tchalanet-platform`.
- Add Spring Modulith and ArchUnit gates.
- Migrate audit/accesscontrol/tenantuser/tenantconfig/tenanttheme/notification out of `core` where applicable.
- Defatten `common` for document, communication, persistent idempotence, and access-control decisions.
- Align core modules to `api/` + `internal/`.
- Keep features as HTTP/BFF leaf modules.

## Non-goals

- No one Maven module per domain/capability yet.
- No forced CQRS/hexagonal architecture inside platform.
- No Java API for features by default.
- No renaming of `/api/v1/platform/**` HTTP scope.
