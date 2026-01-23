# tenanttheme Specification

## Purpose

This specification defines the tenant-scoped theme lifecycle. It governs how a Theme Preset (from `catalog/theme`) is applied, persisted, versioned, and exposed as an effective theme for a tenant.

Theme preset definition and storage are explicitly out of scope and handled by `catalog/theme`.

---

## Requirements

### Requirement: T1 — Apply tenant theme

The system MUST allow applying a Theme Preset to a tenant via the command `ApplyTenantThemeCommand`.

- Command handler:
  `core/tenanttheme/application/command/handler/ApplyTenantThemeCommandHandler`
  MUST validate the existence and availability of the preset via `ThemeCatalog`
  (lookup by `presetCode`) before persisting.
- The handler MUST persist a tenant-scoped entity `TenantTheme`
  (table `tenant_theme`) and increment the version.
- After successful commit, the handler MUST publish a
  `TenantThemeUpdatedEvent`.

#### Scenario: appliquer un preset valide

- Given : un tenant T et un preset code `dark-v1` actif dans `catalog/theme`
- When : `ApplyTenantThemeCommand(tenantId=T, presetCode=dark-v1)` est envoyé
- Then :
  - une ligne existe dans `tenant_theme` pour T
  - `preset_code = dark-v1`
  - la version est incrémentée
  - `TenantThemeUpdatedEvent` est publié après commit

#### Scenario: preset invalide

- Given : le preset `archived-theme` est soft-deleted dans `catalog/theme`
- When : `ApplyTenantThemeCommand` référence `archived-theme`
- Then :
  - la commande est rejetée avec une erreur lisible
  - aucune écriture n'est effectuée
  - aucun événement n'est publié

---

### Requirement: T2 — TenantTheme persistence & RLS

Tenant theme persistence MUST be tenant-scoped and enforce Row-Level Security.

- Expected table: `tenant_theme`
  - `tenant_id`
  - `preset_code` (or preset_id if chosen internally)
  - `metadata` (JSONB: mode, density, overrides, etc.)
  - `version`
  - `created_at`, `updated_at`, `created_by`
- All read/write access MUST be protected by Postgres RLS policies.

#### Scenario: persistance tenant-scoped

- Given : Postgres with RLS enabled on `tenant_theme`
- When : `ApplyTenantThemeCommand` writes for tenant T
- Then :
  - the row is inserted successfully
  - the row is visible only to authorized sessions for tenant T

---

### Requirement: T3 — Validation & consistency

Before persisting, the module MUST validate that:

- the referenced preset exists and is active in `ThemeCatalog`
- the provided tenantId is valid and resolvable

#### Scenario: validation cross-module

- Given : `ThemeCatalog` contient `modern-light`
- And : tenantId is valid
- When : handler valide la commande
- Then : la persistance continue

---

### Requirement: T4 — Events & idempotence

Handlers MUST publish `TenantThemeUpdatedEvent` after commit and guarantee command idempotence.

- Event payload MUST include:
  - `tenantId`
  - `presetCode`
  - `version`
  - `timestamp`
  - `initiator`

Idempotence MUST be ensured via one of the following (implementation choice):

- optimistic locking on `(tenant_id, version)`
- compare-and-set on `(tenant_id, preset_code)`
- command id / deduplication key

#### Scenario: commande réessayée

- Given : `ApplyTenantThemeCommand` was processed but client timed out
- When : the same command is retried
- Then :
  - no duplicate state is created
  - the resulting tenant theme state is consistent
  - no duplicate final event is emitted (or emission is safely deduplicated)

---

### Requirement: T5 — Deactivate tenant theme

The system MUST support deactivation or reset of a tenant theme via `DeactivateTenantThemeCommand`.

- Behavior:
  - removes or deactivates the tenant-specific theme
  - reverts to platform default resolution
  - increments version
  - publishes `TenantThemeUpdatedEvent`

#### Scenario: reset tenant theme

- Given : tenant T has an applied theme
- When : `DeactivateTenantThemeCommand(tenantId=T)` is executed
- Then :
  - tenant-specific theme is removed or marked inactive
  - default resolution applies
  - an update event is published

---

### Requirement: T6 — API surface (ports, commands, queries)

The module MUST expose:

- Command contracts:
  - `ApplyTenantThemeCommand`
  - `DeactivateTenantThemeCommand`
- Query contract:
  - `ResolveTenantThemeQuery` returning the effective tenant theme
- Outgoing ports:
  - `TenantThemePersistencePort`
  - (read) `TenantThemeReaderPort`

Handlers MUST depend on ports only; infrastructure adapters MUST enforce RLS.

#### Scenario: port abstraction

- Given : a Postgres persistence adapter
- When : handler calls `TenantThemePersistencePort.save`
- Then : the adapter persists data according to RLS policies

---
