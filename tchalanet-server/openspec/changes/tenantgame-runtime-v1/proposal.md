# Change: tenantgame-runtime-v1

## Decision

Tchalanet separates global game catalog definitions from tenant game runtime configuration.

```text
catalog.game         = global game definitions managed by SUPER_ADMIN/platform
platform.tenantgame  = tenant-specific activation, display, stake bounds, simple availability, and runtime game views
```

Tenant admins do not modify `catalog.game`. They activate and configure allowed tenant game settings through `platform.tenantgame`.

The word `policy` is removed from tenant game admin operations. Tenant game updates are `settings`, not global policy changes.

## Why

`catalog.game` defines what a game is. If a tenant changes `combination`, `minDigits`, `maxDigits`, or payout algorithm for the same `gameCode`, sales/settlement/payout/reporting become unsafe.

Tenants still need to decide what they offer: enable/disable, display name, POS visibility, display order, simple min/max stake, and simple availability window.

Those are tenant runtime settings, not global game rules.

## What

Update `platform.tenantgame`:

- add explicit `tenant_game` columns for V1 settings;
- remove free-form `policy` from tenant admin endpoints;
- add `/admin/games` operations;
- add `/admin/games/catalog` projection combining `GameCatalog` + tenant status;
- add runtime view for POS/sales/bootstrap;
- protect operations with permissions and entitlements/quotas when applicable.

## Impact

Database:

- add `game_code`, `visible_in_pos`, `display_order`, simple availability columns;
- avoid `tenant_game_availability` table in V1;
- remove/rename `flags` if not strictly needed.

Backend:

- split `TenantGameService` into admin/runtime/catalog projection/validator/provisioning services;
- add typed admin/runtime views;
- use `TenantGameApiAdapter`.

Frontend:

- tenant admin can have tabs: tenant games, catalog, settings/availability/pricing links.
- POS gets runtime games from bootstrap or runtime endpoint.

## Non-goals

- Dynamic JSON schema per game.
- Tenant custom game rules.
- Tenant changes to global combination/minDigits/maxDigits.
- Complex availability calendar/table.
- Pricing odds management inside tenantgame.
- Limits/promotions inside tenantgame.

## Success criteria

- Tenant admin operations never modify `catalog.game`.
- `UpdateTenantGamePolicyRequest` is replaced by settings requests.
- Tenant game runtime view is safe and typed.
- Every `tenant_game` column has a V1 consumer.
- Permissions and entitlement checks are documented and applied.
