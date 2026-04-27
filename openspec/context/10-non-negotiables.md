## Architecture Layers (STRICT)

Backend code MUST follow these layers.

This is a hard constraint.  
Any violation requires an explicit ADR.

---

### Layers

- `common/`
  Technical transversal only.
  No business rules, no domain decisions.

- `core/`
  Critical business domains and invariants.
  Source of truth for money, draws, tickets, limits, security rules.

- `features/`
  Application layer.
  BFF, orchestration, page models, cross-domain aggregation.

- `catalog/`
  Reference and lookup data.
  Configuration, calendars, mappings, registries (read-mostly).

---

### Dependency Rules (ENFORCED)

- `core/` MUST NOT depend on `features/`
- `core/` MUST NOT depend on `catalog/`
- `catalog/` MUST NOT emit domain events
- `catalog/` MUST NOT contain business invariants
- `features/` MAY depend on `core/` and `catalog/`
- `common/` MUST NOT depend on any business layer (`core`, `features`, `catalog`)

Allowed dependency graph:

```text
common
  ↑
catalog     core
    ↑       ↑
     └── features


```

## Intent

Business rules live only in core/
Reference data lives only in catalog/
Orchestration lives only in features/
Infrastructure and glue live only in common/
If a class does not clearly belong to one layer, it is misplaced.

## Enforcement

New code MUST respect these rules
Refactors MUST reduce violations, never add new ones
Any exception MUST be documented in an ADR
