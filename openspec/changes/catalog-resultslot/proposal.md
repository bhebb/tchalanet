# Proposal — catalog.resultslot (MVP)

## Intent

Formalize `resultslot` as a **Catalog domain**.

`resultslot` defines the **global reference of expected result slots**
(time, timezone, provider, projection rules).
It is a **read-mostly, stable lookup** consumed by core domains
(draw scheduling, result ingestion, reporting).

This proposal does **not** introduce new functionality.
Its purpose is to clarify **ownership, boundaries, APIs, and constraints**
and to align the existing code with Tchalanet architectural rules.

---

## Context packs

- 10-non-negotiables.md
- 20-backend-rules.md
- 75-catalog-rules.md
- 70-draw.md

---

## Near-code references

- tchalanet-server/src/main/java/com/tchalanet/server/catalog/README.md
- tchalanet-server/src/main/java/com/tchalanet/server/catalog/resultslot/\*\*
- tchalanet-server/src/\*\*/DOMAIN_RESULTSLOT.md
- tchalanet-server/docs/flows/results_pipeline.md
- tchalanet-server/docs/conventions/cache.md
- ARCHITECTURE.md

---

## Domain role

`resultslot` is a **pure referential catalog**.

It answers:

- What external result is expected?
- When does it occur (timezone + draw_time)?
- Which provider is authoritative?
- Which projection rules apply?

It does NOT:

- Trigger result fetching
- Trigger batch execution
- Apply results to draws
- Emit domain events
- Contain business workflows or lifecycle state

---

## Scope

### In scope

- Definition of global result slots:
  - slotKey
  - provider
  - timezone
  - draw_time
  - days_of_week
- Projection configuration (`projection_cfg`)
- Source configuration (`source_cfg`)
- Read-only lookup APIs
- Cache strategy and eviction rules
- Admin maintenance of the catalog (platform scope)
- API contract: admin endpoints return the standardized `ApiResponse<T>` wrapper (project-wide convention)

### Out of scope

- External provider fetching
- Draw instantiation
- Ticket settlement
- Payout logic
- Tenant enable/disable rules
- Any domain state machine

---

## Domain model

### Entity

- `ResultSlot`
  - Global entity (no tenant_id)
  - Soft-deletable (`deleted_at` nullable)
  - Versioned (optimistic lock column)

### Invariants

- `slotKey` is globally unique
- A slot represents **one expected result per calendar occurrence**
- Timezone and draw_time are authoritative
- Changes are rare and controlled
- No tenant-specific configuration

---

## Public API (Catalog facade)

Package:
`com.tchalanet.server.catalog.resultslot.api`

Facade:

- `ResultSlotCatalog`

Methods (MVP):

- `List<ResultSlotView> listActive()`
- `Optional<ResultSlotView> findByKey(String slotKey)`
- `ResultSlotView requireByKey(String slotKey)`

Rules:

- Read-only only
- No CQRS
- No CommandBus
- No domain events

---

## Views

### ResultSlotView (recommended)

Fields:

- `slotKey` (String) — ex: `NY_MID`
- `provider` (String) — e.g. `NY` / `FL` / `GA`
- `timezone` (ZoneId)
- `drawTime` (LocalTime)
- `daysOfWeek` (canonical string: `MON,TUE,...`)
- `active` (boolean)
- `labelKey` (String, optional, i18n)

Goal:

- Lightweight
- Cache-friendly
- Stable contract

---

## Cache strategy

Cache namespace: `catalog:resultslot`

Caches (MVP):

- `catalog:resultslot:active` — caches the `listActive()` result
- `catalog:resultslot:by_key:{slotKey}` — caches individual lookups

TTL:

- ~20 hours (read-mostly)

Eviction:

- On admin write only (create/update/delete)
- No TTL-based invalidation required for correctness

---

## Writes & administration

- Result slots are **platform-admin managed**
- Writes are synchronous and internal to the catalog module
- No CommandBus
- No domain events

Implementation guidance:

- Admin controller under `/platform/result-slots` (platform scope) and MUST return the project-standard `ApiResponse<T>` wrapper for all 2xx responses.
- Internal service + repository
- Explicit cache eviction on writes (targeted keys when possible)

---

## Dependencies

### Allowed

- `common/`

### Forbidden

- `core/`
- `features/`
- command/query bus
- batch
- domain events

Catalog rules are enforced strictly.

---

## Consumers

Known consumers (read-only):

- `catalog.drawchannel`
- `core.resultprovider`
- `core.draw` (calendar reference only)
- `features.public*`
- `features.stats`

All consumers MUST treat `resultslot` as immutable reference data.

---

## Data model (summary)

- Table: `result_slot`
- Global table (no tenant_id)
- Soft delete supported via `deleted_at`
- Version column for admin updates
- Schema defined via Flyway migrations

---

## Architecture enforcement

- Packages split: `api` vs `internal`
- ArchUnit rules (example):
  - No classes in `..catalog.resultslot..` should depend on `..core..`
  - No classes in `..catalog.resultslot..` should depend on `com.tchalanet.server.common.bus..`
  - No dependency from external packages to `..catalog.resultslot.internal..`

---

## Non-goals

- Tenant overrides
- Slot grouping
- Multi-provider arbitration
- Retry or fallback strategies
- Event publication

---

## Open questions

None for MVP.
