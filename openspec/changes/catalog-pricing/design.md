# Design — catalog.pricing

## Overview

This design describes the minimal architecture for the `pricing` catalog: a read contract used by `core.sales`, an admin surface for maintenance, and cache rules. It follows the catalog conventions under `openspec/context/75-catalog-rules.md`.

---

## Components

1. `PricingCatalog` (api) — read-only facade used by consumers.
2. `PricingCatalogImpl` (internal) — implementation that queries `PricingOddsJpaRepository` and maps entities to minimal types.
3. `PricingOddsEntity` + `PricingOddsJpaRepository` — internal JPA persistence.
4. `PricingAdminService` — write logic (create/update/softDelete) + cache eviction.
5. `PricingAdminController` — admin HTTP endpoints returning `ApiResponse<T>` and exposing DTOs.
6. Cache namespaces and constants under `catalog/pricing/internal/infra/cache` and `catalog/pricing/cache`.

---

## Key decisions

- Use a single cache name for odds (`catalog:pricing:odds`) and cache per tenant/game/betType/betOption using a composite key.

- Admin endpoints under `/platform/pricing-odds` (platform-admin scope) and secured with `SUPER_ADMIN` authority.

- Do not expose repository via Spring Data REST; keep JPA entities internal only.

- DTOs for admin responses: `PricingOddsResponse` to avoid leaking JPA entities. Use `ApiResponse<T>` envelope consistently.

---

## Data shapes

- `PricingOddsResponse` (admin view):

  - `id: UUID`
  - `tenantId: UUID` (nullable if global)
  - `gameCode: String`
  - `betType: String` (enum name)
  - `betOption: Short` (nullable)
  - `odds: BigDecimal`
  - `active: boolean`
  - `createdAt` / `updatedAt` timestamps

- Cache key convention: `tenantId + ':' + gameCode + ':' + betType + ':' + betOption`

---

## Tradeoffs & alternatives

- Per-tenant caches vs global caches: caching per tenant reduces collisions but increases key cardinality. We choose composite keys to keep cache compact and consistent with `PricingCatalogImpl`.

- Exposing a public read endpoint for odds lookup vs purely internal catalog: the design keeps read contract internal (consumed by Java services) but admin endpoints remain platform HTTP for manual maintenance.

- Validation: basic validation is done at controller/service level; deep validation of `odds` range is out-of-scope for the MVP.

---

## Security

- Admin controller annotated with `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`.
- No tenant-scoped admin endpoints in MVP.

---

## Testing

- Unit tests for mapping and caching behavior.
- Service tests for cache eviction.
- Optional integration tests for the REST controller.

---

## Migration

- No DB schema changes required for MVP if `pricing_odds` already exists. If not present, add migration script in `tchalanet-server/src/main/resources/db/migration` and reference it in `tasks.md`.

---

_Design notes maintained with change-id `catalog-pricing`._
