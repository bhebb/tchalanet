# Change: core.terminal Runtime

## Why

Terminal is a runtime device, not a passive catalog record. It manages:

- physical and virtual terminals
- assigned seller/outlet
- active terminal selection
- lock/unlock
- heartbeat/last_seen
- offline/sync state

## What

Make `core.terminal` the source of truth for terminal CRUD, status, runtime, and operational reads.

## Non-goals

- Do not move terminal CRUD into `features.tenantadmin`.
- Do not make terminal a target for limit/autonomy in MVP.
