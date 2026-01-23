# Catalog: Pricing

This document describes the `pricing` catalog API and admin conventions.

Endpoints (admin, platform scope)

- GET /platform/pricing-odds/active -> ApiResponse<List<PricingOddsResponse>>
- GET /platform/pricing-odds/{id} -> ApiResponse<PricingOddsResponse>
- POST /platform/pricing-odds -> ApiResponse<PricingOddsResponse>
- PUT /platform/pricing-odds/{id} -> ApiResponse<PricingOddsResponse>
- DELETE /platform/pricing-odds/{id} -> ApiResponse<Void>

Cache

- Cache name: `catalog:pricing:odds`
- Cache key convention: `tenantId + ':' + gameCode + ':' + betType + ':' + betOption`
- Eviction: admin create/update/delete must evict the cache (allEntries=true)

Notes

- Admin endpoints return the standardized `ApiResponse<T>` wrapper.
- Do not expose JPA entities in API responses; use DTOs such as `PricingOddsResponse`.
