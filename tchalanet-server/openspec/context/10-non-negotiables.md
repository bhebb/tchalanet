## Architecture Layers (STRICT)

Backend code MUST follow these layers.

This is a hard constraint.  
Any violation requires an explicit ADR.

---

### Layers

- `common/`
  Technical transversal only.
  No business rules, no domain decisions.

- `catalog/`
  Reference and lookup data.
  Configuration, calendars, mappings, registries (read-mostly).
  No business invariants. No domain events.

- `platform/`
  Cross-cutting application service capabilities (audit, identity, accesscontrol,
  communication, document, idempotence, notification, tenantconfig, tenanttheme, address…).
  Stateful or lifecycle-bearing, but does NOT own core business-critical invariants.
  Exposes only its `api/` to other modules.
  MUST NOT depend on `core/` or `features/`.

- `core/`
  Critical business domains and invariants.
  Source of truth for money, draws, tickets, limits, security rules.
  MAY depend on `platform.api` only — never on `platform.internal`.

- `features/`
  Application layer.
  BFF, orchestration, page models, cross-domain aggregation.

---

### Dependency Rules (ENFORCED)

- `core/` MUST NOT depend on `features/`
- `core/` MUST NOT depend on `catalog/`
- `core/` MAY depend on `platform.<capability>.api` (never on `platform.<capability>.internal`)
- `catalog/` MUST NOT emit domain events
- `catalog/` MUST NOT contain business invariants
- `platform/` MUST NOT depend on `core/` or `features/`
- `platform.<a>.internal` MUST NOT depend on `platform.<b>.internal`
- `features/` MAY depend on `core/`, `catalog/`, and `platform.api`
- `common/` MUST NOT depend on any other layer

Allowed dependency graph:

```text
common
  ↑
catalog
  ↑
platform
  ↑
core
  ↑
features
```

## Intent

Business rules live only in core/
Reference data lives only in catalog/
Cross-cutting application services live only in platform/
Orchestration lives only in features/
Infrastructure and glue live only in common/
If a class does not clearly belong to one layer, it is misplaced.

## Enforcement

New code MUST respect these rules
Refactors MUST reduce violations, never add new ones
Any exception MUST be documented in an ADR
