# Spec delta: pricing

Change-id: catalog-pricing

## ADDED Requirements

### Requirement: Pricing lookup API

- The system MUST provide a pricing lookup `PricingCatalog` with the method:
  - `BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption)`
- The method MUST return a non-null `BigDecimal` (fallback to `BigDecimal.ONE` when no matching record found).
- The method MUST be cacheable using the cache name `catalog:pricing:odds` with key convention `tenantId + ':' + gameCode + ':' + betType + ':' + betOption`.

#### Scenario: Default odds fallback

- Given no pricing row exists for the provided tenant/game/betType/betOption
- When `PricingCatalog.oddsFor(...)` is called
- Then return `BigDecimal.ONE` as a default value

#### Scenario: Cacheable lookup

- Given a pricing row exists and `PricingCatalog.oddsFor(...)` has been called once
- When it is called again with the same inputs
- Then the result SHOULD be served from cache (no DB query required)

### Requirement: Admin API surface

- The admin surface MUST expose the following HTTP endpoints under `/platform/pricing-odds` and MUST return `ApiResponse<T>` for all 2xx responses:

  - `GET /platform/pricing-odds/active` → `ApiResponse<List<PricingOddsResponse>>`
  - `GET /platform/pricing-odds/{id}` → `ApiResponse<PricingOddsResponse>`
  - `POST /platform/pricing-odds` → `ApiResponse<PricingOddsResponse>` (created)
  - `PUT /platform/pricing-odds/{id}` → `ApiResponse<PricingOddsResponse>`
  - `DELETE /platform/pricing-odds/{id}` → `ApiResponse<Void>`

- Admin endpoints MUST be secured (e.g. `SUPER_ADMIN`) and MUST NOT expose internal JPA entities.

#### Scenario: Admin create -> cache evicted

- Given `catalog:pricing:odds` cache contains entries
- When an admin creates a new pricing row
- Then the cache is evicted for `catalog:pricing:odds` (allEntries=true)

#### Scenario: Admin update -> cache evicted

- Given `catalog:pricing:odds` cache contains entries
- When an admin updates an existing pricing row
- Then the cache is evicted for `catalog:pricing:odds` (allEntries=true)

#### Scenario: Admin delete -> cache evicted

- Given `catalog:pricing:odds` cache contains entries
- When an admin soft-deletes a pricing row
- Then the cache is evicted for `catalog:pricing:odds` (allEntries=true)

## MODIFIED/REMOVED

- None
