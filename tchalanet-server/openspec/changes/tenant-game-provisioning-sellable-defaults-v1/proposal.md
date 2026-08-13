# tenant-game-provisioning-sellable-defaults-v1

## Why

Tenant provisioning advertises default lottery games and draw channels. The seeded demo tenant also
has sellable tenant-game defaults, but Java provisioning created tenant games with null stake bounds,
which made backend setup/readiness views report missing stake configuration even though the games
were enabled and visible on POS.

## What

- Align Java tenant-game provisioning with the seeded tenant-game defaults.
- Create default tenant games as enabled, visible on POS, ordered from catalog sort order, and
  sellable with default stake bounds.
- Keep admins able to update or disable those tenant games after provisioning.

## Impact

Freshly provisioned default-lottery tenants expose the same operational game defaults as the seeded
tenant and can be represented by admin UI without inventing a different game activation state.

## Non-goals

- Changing pricing or limit-policy ownership.
- Changing draw-channel ownership.
- Migrating existing tenants with manually edited tenant games.
