# Proposal — catalog.pricing (MVP)

## Intent

Formalize `pricing` as a Catalog domain and align its public/admin API with project conventions.

`pricing` is the referential source for odds/multipliers used by `core.sales` and other consumers. This change documents the contract, cache strategy, admin surface, and operational constraints and provides clear tasks and a spec delta to validate implementations.

---

## Context packs

- `openspec/context/75-catalog-rules.md`
- `tchalanet-server/docs/conventions/cache.md`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/DOMAIN_PRICING.md`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/**`

---

## Near-code references

- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/DOMAIN_PRICING.md`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/api/PricingCatalog.java`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/internal/PricingCatalogImpl.java`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/internal/persistence/PricingOddsEntity.java`
- `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/internal/persistence/PricingOddsJpaRepository.java`

---

## Scope of this change (MVP)

1. Define the public read contract (catalog API) and DTOs for pricing lookups.
2. Ensure admin CRUD endpoints follow project conventions:
   - admin endpoints under `/platform/pricing-odds`
   - admin endpoints return `ApiResponse<T>` and expose stable DTOs (not JPA entities)
3. Define and document cache names and eviction rules.
4. Provide spec delta and tasks for reviewers and CI validation.

Out of scope: tenant pricing feature expansion, complex pricing rules evaluation, external pricing feeds ingestion.

---

## Deliverables

- `openspec/changes/catalog-pricing/proposal.md` (this file)
- `openspec/changes/catalog-pricing/tasks.md` (checklist for implementation and validation)
- `openspec/changes/catalog-pricing/design.md` (architectural notes & tradeoffs)
- `openspec/changes/catalog-pricing/specs/pricing/spec.md` (spec delta with requirements and scenarios)

---

## Validation

Follow the validation step in the project: run `./node_modules/.bin/openspec validate catalog-pricing --strict --no-interactive` and ensure no errors. Include unit/slice tests referenced in `tasks.md`.

---

## Notes for reviewers

- The proposal favors minimal, well-documented API changes and reuses existing patterns from other catalogs (e.g., `resultslot`).
- If the team prefers a different admin path or SDR exposure, call it out in review; the openspec enforces the `ApiResponse<T>` wrapper and DTO policy.
