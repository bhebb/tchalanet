# Spec delta: resultslot

Change-id: catalog-resultslot

## ADDED Requirements

### Requirement: ResultSlotView

- The catalog MUST expose a read-only view `ResultSlotView` with the following fields:

  - `ResultSlotId id` — primary identifier (DB id)
  - `String slotKey` — global identifier
  - `String provider`
  - `String timezone` (canonical ZoneId string)
  - `LocalTime drawTime`
  - `int cutoffSec`
  - `String daysOfWeek` (existing parser format `MON,TUE,...`)
  - `boolean active`
  - `JsonNode sourceCfg` (opaque JSON blob, mapped as-is)
  - `JsonNode projectionCfg` (opaque JSON blob, mapped as-is)
  - `String labelKey` (optional i18n key)

- Mapping rules:
  - `sourceCfg` and `projectionCfg` MUST be exposed as opaque JSON values (e.g. Jackson `JsonNode`) and MUST NOT be parsed or transformed by the catalog; they are stored and returned "as-is" by the catalog implementation.
  - The view MUST be flat and serializable without nested domain objects (no entity references, only primitives and JSON blobs).
  - The view MUST be immutable (read-only DTO) and cache-friendly (stable JSON property names and stable ordering where applicable).

#### Scenario: List active rows

- Given the database contains multiple `result_slot` rows where some have `active = false` or `deleted_at != NULL`
- When `ResultSlotCatalog.listActive()` is called
- Then only rows with `active = true` and `deleted_at IS NULL` are returned as `ResultSlotView`

### Requirement: Catalog API

- The implementation MUST provide an interface `ResultSlotCatalog` with methods:
  - `List<ResultSlotView> listActive();`
  - `Optional<ResultSlotView> findByKey(String slotKey);`
  - `ResultSlotView requireByKey(String slotKey);` (throws NotFound)
- Methods MUST be side-effect free and MUST NOT emit domain events.

#### Scenario: Catalog side-effect free

- When `ResultSlotCatalog.listActive` is executed
- Then no domain events are emitted and no modifications are made to other tables

### Requirement: Caching

- The result of `listActive()` MUST be cached using cache name `catalog:resultslot:active`.
- Individual lookups `findByKey` MAY use `catalog:resultslot:by_key:{slotKey}`.

#### Scenario: Cache eviction on write

- Given a cached result for `listActive()`
- When an admin creates/updates/deletes a `result_slot`
- Then the catalog MUST evict the `catalog:resultslot:active` cache (and per-key entries if applicable)

### Requirement: Admin API surface

- Platform admin endpoints MUST return the standardized `ApiResponse<T>` wrapper used project-wide for all 2xx responses.
- Admin endpoints MUST NOT leak internal JPA entities; they MUST return `ResultSlotView` or other stable DTOs.
- Admin endpoints (MVP):
  - `GET /platform/result-slots/active` → `ApiResponse<List<ResultSlotView>>`
  - `GET /platform/result-slots/by-key/{slotKey}` → `ApiResponse<ResultSlotView>`
  - `POST /platform/result-slots` → `ApiResponse<ResultSlotView>` (created)
  - `PUT /platform/result-slots/{id}` → `ApiResponse<ResultSlotView>`
  - `DELETE /platform/result-slots/{id}` → `ApiResponse<Void>`

## MODIFIED/REMOVED

- None in this change
