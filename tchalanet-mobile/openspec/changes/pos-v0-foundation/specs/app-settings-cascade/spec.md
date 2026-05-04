# Spec 04 — App settings cascade extension

## Domain

`catalog.settings` (extension).

## ADDED Requirements

### Requirement: `app_setting` supports AGENT level

The `app_setting` table SHALL support a 5-level cascade: GLOBAL → TENANT → OUTLET → TERMINAL → AGENT.
An `agent_id` column is added. The scope constraint is updated to enforce that each level has exactly the right non-null scope columns.

#### Scenario: AGENT-level setting requires agent_id only

- **WHEN** a setting is inserted with `level='AGENT'` and `agent_id IS NOT NULL`, `outlet_id IS NULL`, `terminal_id IS NULL`
- **THEN** the constraint passes

#### Scenario: AGENT-level setting with outlet_id rejected

- **WHEN** a setting is inserted with `level='AGENT'` and both `agent_id` and `outlet_id` set
- **THEN** the constraint rejects the row

---

### Requirement: Override flags control cascade depth

Three boolean flags SHALL be added to `app_setting`: `is_overridable_by_outlet`, `is_overridable_by_terminal`, `is_overridable_by_agent` (all default false).
These flags are set at the TENANT (or GLOBAL) row of a key and constrain the maximum level to which the key may be overridden.

#### Scenario: Agent cannot override without flag

- **WHEN** `SettingsResolverPort.resolve(key, ctx)` is called with an agent context
- **AND** the TENANT-level row has `is_overridable_by_agent=false`
- **THEN** the TENANT-level value is returned regardless of any AGENT-level row

#### Scenario: Agent can override with flag

- **WHEN** the TENANT-level row has `is_overridable_by_agent=true` and an AGENT-level row exists
- **THEN** the AGENT-level value is returned

---

### Requirement: `SettingKey` registry enum is the source of truth

A `SettingKey` Java enum SHALL declare every `(namespace, key)` pair with metadata:
`namespace`, `key`, `category` (HARDWARE/DISPLAY/WORKFLOW/PRICING/SECURITY/FISCAL), `maxLevel`, `validator`, `defaultValue`, `auditPolicy`.

At application startup, a `SettingRegistryValidator` bean SHALL verify that all active DB rows have a corresponding `SettingKey` entry. Any unknown key causes startup to fail.

#### Scenario: Unknown DB key causes startup failure

- **WHEN** an `app_setting` row exists with a `(namespace, key)` not in `SettingKey`
- **THEN** application startup fails with a descriptive error

#### Scenario: All SettingKey defaults can be parsed

- **WHEN** each `SettingKey.defaultValue` is parsed through its `validator`
- **THEN** no `IllegalArgumentException` is thrown

---

### Requirement: `SettingsResolverPort` implements cascade with clamp

The resolver SHALL:

1. Build the ordered candidate list (AGENT → TERMINAL → OUTLET → TENANT → GLOBAL)
2. Clamp by `key.maxLevel` — skip levels above the max
3. Gate AGENT / TERMINAL / OUTLET levels by the respective override flag on the TENANT row
4. For VIRTUAL terminal: treat `terminal.ownerAgentId == ctx.agentId` as an ownership exception that allows AGENT-level override even if `is_overridable_by_terminal=false`
5. Return the most specific row that passes all gates; fall back to `SettingKey.defaultValue`

#### Scenario: Clamp by maxLevel

- **WHEN** `SettingKey.maxLevel = TENANT` and a TERMINAL-level row exists
- **THEN** the TERMINAL row is skipped and the TENANT row is returned

#### Scenario: Virtual terminal owner exception

- **WHEN** `is_overridable_by_agent=true`, `terminal.kind=VIRTUAL`, `terminal.ownerAgentId=ctx.agentId`
- **THEN** the AGENT-level row is returned even if `is_overridable_by_terminal=false`

#### Scenario: Falls back to default

- **WHEN** no DB row exists at any level for a key
- **THEN** `SettingKey.defaultValue` is returned

---

### Requirement: `EffectiveSetting<T>` carries editable flag

The `EffectiveSetting` read-model SHALL include a boolean `editable` field.
`editable=true` if and only if the calling context has the right permission and the key allows override at the caller's level.
The Flutter client SHALL use this flag to show or hide edit controls.

#### Scenario: editable=false for non-overridable key

- **WHEN** `GetEffectiveSettingQuery` is resolved for an AGENT caller on a key with `is_overridable_by_agent=false`
- **THEN** `EffectiveSetting.editable=false`

---

### Requirement: `UpsertSettingCommand` validates ownership and permissions

- At GLOBAL level: requires `setting.global.write`
- At TENANT level: requires `setting.tenant.write`
- At OUTLET/TERMINAL level: requires `setting.outlet.write` / `setting.terminal.write`
- At AGENT level: requires `setting.self.write` AND caller is the agent OR `setting.tenant.write`
- The value SHALL be parsed through `SettingKey.validator` before persistence

#### Scenario: Invalid value rejected

- **WHEN** `UpsertSettingCommand` carries a value that fails the key's validator
- **THEN** a 422 `SETTING_VALUE_INVALID` is returned

---

### Requirement: `SettingChangedEvent` published after-commit for every mutation

Every upsert, soft-delete, restore, and hard-delete SHALL emit a `SettingChangedEvent` after-commit.
The cache eviction listener uses this event to invalidate scoped cache entries.

#### Scenario: SettingChangedEvent emitted on upsert

- **WHEN** `UpsertSettingCommand` is handled successfully
- **THEN** `SettingChangedEvent(change=UPSERT)` is published

---

### Requirement: Schema additions applied in-place to existing migration

The following DDL SHALL be added to the existing migration that creates `app_setting`:

```sql
ALTER TABLE app_setting DROP CONSTRAINT IF EXISTS chk_app_setting_level;
ALTER TABLE app_setting ALTER COLUMN level TYPE varchar(16);
ALTER TABLE app_setting ADD CONSTRAINT chk_app_setting_level
  CHECK (level IN ('GLOBAL','TENANT','OUTLET','TERMINAL','AGENT'));

ALTER TABLE app_setting
  ADD COLUMN IF NOT EXISTS agent_id                   uuid    NULL,
  ADD COLUMN IF NOT EXISTS is_overridable_by_outlet   boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS is_overridable_by_terminal boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS is_overridable_by_agent    boolean NOT NULL DEFAULT false;

ALTER TABLE app_setting DROP CONSTRAINT IF EXISTS chk_app_setting_scope;
ALTER TABLE app_setting ADD CONSTRAINT chk_app_setting_scope CHECK (
  (level = 'GLOBAL'   AND tenant_id IS NULL     AND outlet_id IS NULL AND terminal_id IS NULL AND agent_id IS NULL) OR
  (level = 'TENANT'   AND tenant_id IS NOT NULL  AND outlet_id IS NULL AND terminal_id IS NULL AND agent_id IS NULL) OR
  (level = 'OUTLET'   AND tenant_id IS NOT NULL  AND outlet_id IS NOT NULL AND terminal_id IS NULL AND agent_id IS NULL) OR
  (level = 'TERMINAL' AND tenant_id IS NOT NULL  AND terminal_id IS NOT NULL AND agent_id IS NULL) OR
  (level = 'AGENT'    AND tenant_id IS NOT NULL  AND agent_id IS NOT NULL)
);

DROP INDEX IF EXISTS ux_app_setting_active_scope;
CREATE UNIQUE INDEX ux_app_setting_active_scope ON app_setting (
    COALESCE(tenant_id,   '00000000-0000-0000-0000-000000000000'::uuid),
    level,
    COALESCE(outlet_id,   '00000000-0000-0000-0000-000000000000'::uuid),
    COALESCE(terminal_id, '00000000-0000-0000-0000-000000000000'::uuid),
    COALESCE(agent_id,    '00000000-0000-0000-0000-000000000000'::uuid),
    namespace, setting_key
) WHERE deleted_at IS NULL AND active = true;
```

#### Scenario: Schema validates after migration

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `ddl-auto=validate` passes for the `app_setting` table
