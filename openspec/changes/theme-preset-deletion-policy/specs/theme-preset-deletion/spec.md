# Theme Preset Deletion Policy Specification

## Purpose

This specification defines the normative policy for handling `TenantTheme` references when a `ThemePreset` is retired (soft-deleted or deactivated) in `catalog/theme`.

Goal: ensure **no tenant is broken**, maintain traceability, and make "preset unavailable" states visible and observable.

---

## ADDED Requirements

### Requirement: DP1 — No hard delete of ThemePreset

The system MUST NOT provide hard delete functionality for `ThemePreset` entities.

Allowed operations:

- `active=false` (depublication / deactivation)
- `deleted_at != NULL` (definitive retirement via soft-delete)

Read behavior:

- All read operations MUST filter `deleted_at IS NULL`
- `listActive()` MUST filter `deleted_at IS NULL AND active=true`
- `findByCode` and `findById` MUST return presets even when `active=false`
  (as long as `deleted_at IS NULL`) and MUST expose the `active` flag
  in `ThemePresetView`

#### Scenario: admin attempts to permanently delete preset

- Given: a `ThemePreset` exists with code `old-preset`
- When: admin attempts to permanently delete the preset
- Then: the operation is rejected (no hard delete method exists)
- And: admin must use `deactivate()` or `softDelete()` instead

#### Scenario: soft-deleted preset is filtered from reads

- Given: a preset with code `archived` has `deleted_at != NULL`
- When: `ThemeCatalog.findByCode("archived")` is called
- Then: `Optional.empty()` is returned (filtered out)

---

### Requirement: DP2 — No automatic tenant theme cleanup

When a preset is retired or depublished, the system MUST NOT automatically modify `tenant_theme` entries.

- No cascade delete
- No implicit reset
- Preservation for audit, debug, and reversibility purposes

#### Scenario: preset deactivation does not modify tenant themes

- Given: tenant T has `tenant_theme.preset_code = "dark-v1"`
- And: preset "dark-v1" is active
- When: admin calls `deactivate("dark-v1")` (sets `active=false`)
- Then: `tenant_theme` entry for tenant T remains unchanged
- And: `tenant_theme.preset_code` still equals "dark-v1"

#### Scenario: preset soft-delete does not modify tenant themes

- Given: tenant T has `tenant_theme.preset_code = "old-preset"`
- When: admin calls `softDelete("old-preset")`
- Then: `tenant_theme` entry for tenant T remains unchanged
- And: audit trail is preserved

---

### Requirement: DP3 — Mandatory fallback resolution

`core/tenanttheme` MUST apply a fallback when resolving the effective theme if the referenced preset is unavailable.

**Unavailable** means:

- preset not found
- preset soft-deleted
- preset inactive (`active=false`)

Fallback cascade (in order):

1. Tenant default (if configured via tenant configuration/registry)
2. Platform default (the single preset flagged as default; MUST be unique)
   - If none is flagged, fall back to conventional code `default-light`
3. Hardcoded safe preset (`default-light`) as last resort

Tenant default resolution source is implementation-defined (e.g. `TenantConfig` or `TenantCatalog` registry), but MUST be stable and deterministic.

The module MUST emit a warning notice when fallback is applied:

- Code: `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED`
- Metadata:
  `{ tenantId, requestedPresetCode, fallbackPresetCode, timestamp }`

The warning notice MUST be emitted through the standard API notice mechanism (e.g. ApiResponse notices) and MUST also be logged (see NF1).

#### Scenario: resolve theme with active preset (happy path)

- Given: tenant T has `tenant_theme.preset_code = "dark-v1"`
- And: preset "dark-v1" exists and `active=true`
- When: `ResolveTenantThemeQuery(tenantId=T)` is executed
- Then: the theme is resolved with preset "dark-v1"
- And: no fallback is applied
- And: no warning notice is emitted

#### Scenario: resolve theme with inactive preset triggers fallback

- Given: tenant T has `tenant_theme.preset_code = "old-preset"`
- And: preset "old-preset" exists but `active=false`
- And: platform default "default-light" exists and is active
- When: `ResolveTenantThemeQuery(tenantId=T)` is executed
- Then: fallback is applied
- And: platform default "default-light" is returned
- And: warning notice `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED` is emitted

#### Scenario: resolve theme with soft-deleted preset triggers fallback

- Given: tenant T has `tenant_theme.preset_code = "archived"`
- And: preset "archived" has `deleted_at != NULL`
- And: tenant has no default configured
- And: platform default "default-light" is active
- When: `ResolveTenantThemeQuery(tenantId=T)` is executed
- Then: fallback returns "default-light"
- And: warning notice is emitted

#### Scenario: resolve theme with not found preset triggers fallback

- Given: tenant T has `tenant_theme.preset_code = "missing"`
- And: no preset with code "missing" exists
- And: hardcoded safe preset "default-light" is the last resort
- When: `ResolveTenantThemeQuery(tenantId=T)` is executed
- Then: fallback returns "default-light"
- And: warning notice is emitted

---

### Requirement: DP4 — Explicit remediation opt-in

If the system provides explicit remediation actions, such remediation MUST be opt-in and MUST NOT be implicit at the moment of preset retirement.

Potential explicit remediation actions (future enhancement, out of MVP scope) MAY include:

- Admin action "migrate preset X → Y"
- Admin action "reset tenant themes using preset X"
- Batch migration job

#### Scenario: no automatic migration on preset retirement

- Given: 100 tenants reference preset "old-preset"
- When: admin retires preset "old-preset"
- Then: no automatic migration is triggered
- And: tenant themes remain unchanged
- And: fallback is applied at resolution time for each tenant

---

## MODIFIED Requirements

### Requirement: theme-preset R2 (modified) — Admin writes (internal)

The catalog MUST provide an internal admin service for management operations: create, update, deactivate, and soft-delete.

Service:

- `catalog/theme/internal/write/ThemePresetAdminService`

Operations:

- `create(...)` → new preset
- `update(...)` → modify preset
- `deactivate(ThemePresetId)` → set `active=false`
- `softDelete(ThemePresetId)` → set `deleted_at=now()` AND `active=false`

Hard delete MUST NOT exist.

Cache invalidation MUST evict:

- `catalog.theme.cache.ACTIVE_PRESETS`
- `catalog.theme.cache.PRESET_BY_CODE`

#### Scenario: deactivate preset (depublication)

- Given: preset "test-preset" exists with `active=true`
- When: admin calls `deactivate("test-preset")`
- Then: preset has `active=false`
- And: `deleted_at` remains `NULL`
- And: caches are invalidated

#### Scenario: soft-delete preset (retirement)

- Given: preset "old-preset" exists
- When: admin calls `softDelete("old-preset")`
- Then: preset has `deleted_at != NULL` AND `active=false`
- And: caches are invalidated
- And: preset is filtered from all read operations

---

## Non-Functional Requirements

### NF1 — Observability

- Fallback events MUST be logged in structured JSON format
- Log level: WARN
- Log marker: `TENANT_THEME_FALLBACK`
- Log payload MUST include:
  `tenantId`, `requestedPresetCode`, `fallbackPresetCode`, `timestamp`

### NF2 — Performance

- Fallback resolution MUST NOT introduce significant latency
- Target: < 50ms additional latency under warm cache conditions
- Fallback cascade MUST be limited to a maximum of 3 lookups
- `ThemeCatalog` reads SHOULD be cached

### NF3 — Audit

- Retirement of presets MUST preserve audit trail
  (`deleted_at`, audit columns, history if enabled)
- `tenant_theme` entries MUST remain unmodified for audit purposes

---

## Acceptance Criteria

- [ ] Hard delete functionality does not exist in `catalog/theme`
- [ ] Tenant referencing retired preset obtains effective theme via fallback
- [ ] Warning notice `THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED` is emitted
      and observable (API notices + logs)
- [ ] No `tenant_theme` entry is modified automatically when preset is retired
- [ ] Tests cover all resolution cases:
      active, inactive, soft-deleted, not found

---

## Out of Scope

- Mass remediation (batch migration) implementation
- Database schema changes (no FK cascade)
- Automatic correction scheduler or batch

---

## Notes

- Remediation is intentionally explicit and opt-in
- Fallback is transparent to tenants
- Historical tenant theme state is preserved for traceability
