# tasks.md

Change-id: catalog-pricing

## Tasks (checklist)

- [ ] Review existing pricing code and docs (`DOMAIN_PRICING.md`, repo, DTOs).

- [x] Ensure public catalog API exists: `PricingCatalog` with method `BigDecimal oddsFor(TenantId, String gameCode, BetType, Short betOption)` (use existing interface if present).

- [x] Add/verify cache names constants under `catalog/pricing/internal/infra/cache` and public `catalog/pricing/cache`:

  - `catalog:pricing:odds` (or equivalent)

- [x] Ensure `PricingCatalogImpl` uses `@Cacheable` with the cache name and key convention.

- [x] Implement admin service + controller (if not present) under:

  - Service: `catalog/pricing/internal/admin/PricingAdminService` (create/update/delete + list/find)
  - Controller: `catalog/pricing/internal/infra/web/PricingAdminController` under `/platform/pricing-odds`
  - Admin endpoints MUST return `ApiResponse<T>` and expose DTOs (not JPA entities).

- [x] Add DTO for admin responses: `PricingOddsResponse` (id, tenantId, gameCode, betType, betOption, odds, active, createdAt, updatedAt).

- [x] Ensure `@CacheEvict` is applied on write methods to evict `catalog:pricing:odds` (all entries) when create/update/delete happen.

- [x] Add unit tests:

  - `PricingCatalogImplTest` : listActive filtering, default odds fallback
  - `PricingAdminServiceTest` : create/update/softDelete evicts cache

- [ ] Add integration/slice tests for controller (optional but recommended).

- [ ] Update docs: add a short `catalog/pricing/README.md` describing API endpoints, cache names, and example requests.

- [ ] Run openspec validation:

  - `./node_modules/.bin/openspec validate catalog-pricing --strict --no-interactive`

- [ ] Prepare PR with link to `openspec/changes/catalog-pricing`, checklist and necessary SQL migrations.
