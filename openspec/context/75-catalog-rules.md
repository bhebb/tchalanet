# Catalog Rules & Map

This context defines how the `catalog/` layer is used.
Catalogs are NOT business domains.

---

## Purpose

The `catalog/` layer contains **reference and lookup data** shared across domains.

Typical usage:

- configuration
- calendars
- mappings
- registries
- read-mostly datasets

---

## What belongs in `catalog/`

Allowed:

- game definitions
- draw slots / schedules
- pricing tables (if declarative)
- external code mappings
- tenant-game registries
- static or slowly-changing data

Characteristics:

- low write frequency
- predictable lifecycle
- no complex state transitions

---

## What does NOT belong in `catalog/`

Forbidden:

- business invariants
- money calculations
- state machines
- validations that decide outcomes
- domain events

If logic decides **“what is allowed / paid / settled”** → it is `core/`.

---

## Architecture Rules

- Catalogs MAY be accessed by:
  - `core/`
  - `features/`
- Catalogs MUST NOT:
  - emit domain events
  - depend on `core/`
  - orchestrate workflows
- Catalogs SHOULD be side-effect free

---

## Technical Shape (recommended)

```text
catalog/<name>/
├─ api/              # catalog with cache is applicable
├─ domain/            # simple models / records
├─ application/       # read queries (optional)
├─ port/out/          # persistence ports
├─ infra/persistence/ # JPA / JDBC adapters
└─ infra/web/         # admin or ops endpoints (optional)
```
