# Change: Use `draw_channel` as the tenant business schedule source

## Why

The current scheduler discussion exposed a boundary issue: opening/closing sales is not a global platform rule. It is a tenant/channel business rule.

Examples:

- Tenant A may close `NY_MID` sales 2 minutes before draw time.
- Tenant B may close `NY_MID` sales 10 minutes before draw time.
- A tenant may want different cutoffs per slot/channel.

A global scheduler property such as `minutes-before-draw` cannot represent this cleanly. Creating a separate tenant draw policy table also feels too broad for MVP and could duplicate configuration already naturally attached to `draw_channel`.

## Decision

Use `draw_channel` as the tenant-scoped commercial configuration for a global `result_slot`.

- `result_slot` remains global and provider/result oriented.
- `draw_channel` becomes the tenant commercial schedule/config for selling that slot.
- `draw` stores concrete generated occurrence snapshots: `scheduled_at`, `cutoff_at`.
- Schedulers stay simple and operate on generated draw snapshots.

## Scope

This change updates:

- draw channel provisioning/seed queries;
- `DrawChannelCatalog` calendar rows;
- draw generation cutoff calculation;
- close scheduler/handler semantics;
- scheduler docs/tick documentation.

This change does not add a new tenant policy table.

## Non-goals

- Do not redesign `result_slot`.
- Do not add `tenant_draw_policy` for MVP.
- Do not make fetch/apply/settle tenant-scoped.
- Do not recalculate cutoff dynamically during sell/close.
- Do not introduce complex scheduler windows.
