# OpenSpec Change — `pos-v0-foundation`

> **Status**: Proposed
> **Order**: ⓶ — depends on `rename-pos-to-terminal` > **Type**: foundation (schema, domain extensions, permissions, events)
> **Risk**: medium — touches several core domains

## Why

After the rename, the next layer is the **foundation** for the v0 POS app:

- `core.terminal` gets `kind` (PHYSICAL/VIRTUAL) and `owner_agent_id` for VIRTUAL terminals owned by an agent (mobile use case).
- `core.outlet` gets `kind` (FIXED/MOBILE/VIRTUAL) and `owner_agent_id` for ambulant or virtual outlets.
- `core.session` (`SalesSession`) gets `opening_float`, `closing_amount`, `expected_amount`, `variance`, `status` extended to `OPEN | CLOSED | ABORTED`, plus auto-close listeners.
- `catalog.settings` gets a 5-level cascade GLOBAL → TENANT → OUTLET → TERMINAL → AGENT, with `is_overridable_by_*` flags and a Java registry of `SettingKey`.
- `core.accesscontrol` gets new permission keys for self-write, terminal scopes, and approval roles.

This change ships the **schema, domain logic, ports, handlers, events, audit and cache** — but no BFF or Flutter consumption yet. Those land in `pos-v0-features`.

## What Changes

- `core.terminal` — extend `Terminal` aggregate: `kind` (PHYSICAL/VIRTUAL, immutable), `owner_agent_id`, `status` (ACTIVE/DISABLED/ARCHIVED). New commands: `CreateTerminalCommand`, `ProvisionVirtualTerminalCommand` (idempotent), `UpdateTerminalCommand`, `ChangeTerminalStatusCommand`. New events: `TerminalCreatedEvent`, `TerminalStatusChangedEvent`, `TerminalReassignedEvent`. Cache entries added. Migrations: `ALTER TABLE terminal ADD …`.
- `core.outlet` — extend `Outlet` aggregate: `kind` (FIXED/MOBILE/VIRTUAL), `owner_agent_id`. Field classification (OPERATIONAL vs JURIDICAL). New commands: `UpdateOutletCommand` (field-whitelist enforced), `ReassignOutletOwnerCommand`, `ChangeOutletStatusCommand`. New events: `OutletCreatedEvent`, `OutletUpdatedEvent`, `OutletKindOrOwnerChangedEvent`, `OutletStatusChangedEvent`. Migrations: `ALTER TABLE outlet ADD …`.
- `core.session` — extend `SalesSession`: `opening_float`, `closing_amount`, `expected_amount`, `variance`, `variance_note`, `tickets_count`, `total_stake_htg`, `closed_by`. Status `ABORTED` added. New commands: `AbortSalesSessionsForUserCommand`, `AbortSalesSessionsForTerminalCommand`. New events: `SalesSessionOpenedEvent`, `SalesSessionClosedEvent`, `SalesSessionAbortedEvent`. Cross-domain listeners. Migrations: `ALTER TABLE sales_session ADD …`.
- `catalog.settings` — extend `app_setting` table: `AGENT` level, `agent_id`, `is_overridable_by_outlet/_terminal/_agent` flags. New unique index. `SettingKey` registry enum. `SettingsResolverPort`. Migrations: `ALTER TABLE app_setting …`.
- `core.accesscontrol` — new permission keys for terminal, outlet, settings, session, sales, limits, autonomy, print, sync. Flyway seed migration.
- Cross-domain — 7 event listeners wiring the above domains.
- All modifications are `ALTER TABLE` in existing migration files — **no new migration file**, DB recreated from scratch.

## Capabilities

### New Capabilities

- `terminal-kinds`: PHYSICAL/VIRTUAL terminal lifecycle with kind invariants, lazy provisioning, scoped auth, cache.
- `outlet-kinds`: FIXED/MOBILE/VIRTUAL outlet lifecycle with field whitelist and owner self-write.
- `sales-session-v0`: Extended `SalesSession` aggregate with float, z-report, abort, cross-domain listeners.
- `app-settings-cascade`: 5-level cascade resolver, `SettingKey` registry, override flags, scoped cache.
- `pos-permissions`: Full permission catalogue for POS surface + sales + limits + sync.
- `pos-events`: Authoritative catalogue of after-commit events and their listener wiring.

### Modified Capabilities

_(none — all capabilities are new)_

## Impact

- **Java sources**: `core.terminal`, `core.outlet`, `core.session`, `catalog.settings`, `core.accesscontrol`, plus listener classes in each domain.
- **SQL migrations**: existing migration files for `terminal`, `outlet`, `sales_session`, `app_setting` and the permission seed are edited in-place.
- **Tests**: handler tests, listener tests, ArchUnit rules.
- **OpenAPI**: new commands/queries exposed from each domain surface.
- **Cache**: 4 new named caches registered.
- **Dependencies**: no new external library; relies on existing Spring Events, Hibernate Envers, Redis (L2).
