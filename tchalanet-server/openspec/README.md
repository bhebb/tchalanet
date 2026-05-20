# Tchalanet — Modulith + Platform Layer Migration Pack v5

This pack captures the latest accepted working decisions for the backend architecture migration.

## Final naming decision

The Java/backend layer name is **`platform`**.

- `platform/` = internal Java/backend layer for transversal application service modules.
- `/api/v1/platform/**` = HTTP Platform Admin scope, generally SUPER_ADMIN-facing.

Documentation MUST use the terms precisely:

- **platform layer** / **platform module** for Java packages.
- **platform admin scope** for HTTP routes.

## Architecture direction

Tchalanet is a **Modular Monolith** using **Spring Modulith** as the global boundary model.

Internal archetypes:

| Layer      | Archetype                             |
| ---------- | ------------------------------------- |
| `common`   | Technical Shared Kernel               |
| `catalog`  | Simple DDD / Reference Catalog        |
| `platform` | Technical/Application Service Modules |
| `core`     | Clean Architecture / Hexagonal / CQRS |
| `features` | Vertical Slice / BFF leaf modules     |
| `app`      | Spring Boot assembly/runtime          |

## High-level implementation order

1. Create macro Maven modules.
2. Create the `platform` Maven module and package structure.
3. Add Spring Modulith + ArchUnit gates with legacy allowlists.
4. migrate catalog to the maven module
5. migrate the features to the new maven module
6. Defatten `common`. migrate basic core to the maven module
7. Migrate transversal `core` modules to `platform`.
8. Align `core` modules to `api/` + `internal/` Modulith shape.
9. Remove legacy allowlists and enforce final gates.

Start with:

- `docs/adr/ADR-001-modulith-platform-layer.md`
- `docs/architecture/IMPLEMENTATION_PLAN.md`
- `openspec/changes/introduce-platform-modulith/tasks.md`
