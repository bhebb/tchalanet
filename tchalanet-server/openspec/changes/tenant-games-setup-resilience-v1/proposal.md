# Tenant games setup resilience v1

## Why

The tenant games setup page composes tenant-game settings, catalog labels, limit assignments,
and pricing rules. A failure in an informational or diagnostic slice currently aborts the whole
page, even though the tenant games and their editable settings remain useful to an administrator.

## What

- Keep the tenant-game list as the required slice.
- Degrade catalog labels, tenant limit assignments, and pricing reads independently.
- Return safe empty fallbacks and stable targeted API notices for degraded slices.
- Preserve the API response envelope in the Angular page so targeted notices are rendered locally.

## Impact

The page remains usable during partial platform/core outages and clearly marks that its readiness
data is incomplete. A failed pricing read still makes the affected games appear incomplete; it does
not silently make them ready.

## Non-goals

- Changing game activation rules or pricing validation.
- Changing the separate limits or draw-sales-matrix pages.
- Adding a new persistence model or migration.
