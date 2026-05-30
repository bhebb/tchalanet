# CATALOG_SETTINGS

> **Domain**: `catalog/settings`  
> **Status**: FUNCTIONAL (meaning + semantics)  
> **Related normative docs**:
>
> - `openspec/context/75-catalog-rules.md` (Catalog rules 75 — structural/technical)
> - `docs/conventions/api/web_api.md`
> - `docs/conventions/api/pagination.md`
>
> **Purpose**: define the _functional meaning_ of Settings (keys, scopes, merge rules, validation, API semantics).  
> This document MUST NOT define layering rules (see Catalog Rules 75).

---

## 1. Role of the domain

**Settings** is a **reference configuration catalog**.

It provides:

- a typed store of configuration values, scoped by target (GLOBAL / TENANT / OUTLET / TERMINAL)
- a deterministic resolution algorithm to compute **effective settings** for a given tenant and optional outlet/terminal
- administrative CRUD for managing settings (platform/admin scope)

It does NOT:

- enforce business invariants (domains interpret settings)
- orchestrate workflows
- emit domain events

---

## 2. Functional concepts

### 2.1 Setting key identity

A setting is identified by:

- `namespace` (string) — grouping key (e.g., "pos.behavior", "ticket.verification")
- `setting_key` (string) — specific setting within namespace (e.g., "require_open_session")

Together, they form a **logical key**: `namespace.setting_key`.

**Examples**:

- `pos.behavior.require_open_session`
- `ticket.verification.public_visibility_days`
- `ui.theme.mode`

### 2.2 Types

Each setting has a declared `value_type`:

- `STRING` — plain text
- `INT` — 32-bit integer
- `LONG` — 64-bit integer
- `DECIMAL` — arbitrary precision decimal (BigDecimal)
- `BOOLEAN` — true/false
- `JSON` — JSON object, array, or primitive

**Storage**: all values are persisted as **text** (VARCHAR/TEXT) and parsed according to `value_type`.

### 2.3 Levels (scopes)

Settings are defined at one of these hierarchical levels:

| Level      | Description                | Required IDs                |
| ---------- | -------------------------- | --------------------------- |
| `GLOBAL`   | Platform-wide default      | none                        |
| `TENANT`   | Tenant-specific override   | `tenant_id`                 |
| `OUTLET`   | Outlet-specific override   | `tenant_id` + `outlet_id`   |
| `TERMINAL` | Terminal-specific override | `tenant_id` + `terminal_id` |

**Decision**: no `USER` and no `DRAW` levels in this catalog.

---

## 3. Resolution semantics (effective settings)

### 3.1 Merge order

To compute **effective settings** for a tenant context:

1. **GLOBAL** — load platform defaults
2. **TENANT** — override with tenant-specific values
3. **OUTLET** (if `outletId` provided) — override with outlet-specific values
4. **TERMINAL** (if `terminalId` provided) — override with terminal-specific values

**Rule**: "more specific wins"  
Later levels overwrite earlier levels for the same logical key (`namespace.setting_key`).

### 3.2 Output format

The resolution returns one `ResolvedSettingView` per logical key, containing:

- `namespace`
- `settingKey`
- `valueType`
- `settingValue` (as text)
- `effectiveLevel` — which level provided the final value (`GLOBAL`, `TENANT`, `OUTLET`, `TERMINAL`)

### 3.3 Filtering by namespaces

Consumers MAY filter by `namespaces` (list of strings).  
If empty or omitted: resolve **all** namespaces.

**Example**:

```
GET /tenant/settings/resolve?tenantId=...&namespaces=pos.behavior,ticket.verification
```

---

## 4. Validation rules

### 4.1 Registry-based validation

All settings MUST be pre-declared in the internal registry (`SettingsRegistry`).

When creating or updating a setting:

1. Verify that `namespace.setting_key` exists in the registry
2. Verify that `value_type` matches the registered type
3. Parse the `setting_value` to ensure it's valid for the declared type

**Example**:

- Registry declares: `pos.behavior.require_open_session` as `BOOLEAN`
- Attempting to create with `value_type=INT` → **rejected**
- Attempting to create with value `"maybe"` → **rejected** (not a valid boolean)

### 4.2 Type parsing rules

| Type      | Parsing Rule              | Example Valid Values         |
| --------- | ------------------------- | ---------------------------- |
| `STRING`  | No parsing                | Any text                     |
| `INT`     | `Integer.parseInt()`      | `"42"`, `"-10"`              |
| `LONG`    | `Long.parseLong()`        | `"9223372036854775807"`      |
| `DECIMAL` | `new BigDecimal()`        | `"123.45"`, `"0.001"`        |
| `BOOLEAN` | `Boolean.parseBoolean()`  | `"true"`, `"false"`          |
| `JSON`    | `ObjectMapper.readTree()` | `{"key":"value"}`, `[1,2,3]` |

### 4.3 Uniqueness constraint

Only **one active, non-deleted setting** may exist for a given:

- `(level, tenant_id, outlet_id, terminal_id, namespace, setting_key)`

Attempting to create a duplicate → **409 Conflict** (or validation error).

### 4.4 Level-specific requirements

| Level      | Required                   | Forbidden                               |
| ---------- | -------------------------- | --------------------------------------- |
| `GLOBAL`   | nothing                    | `tenant_id`, `outlet_id`, `terminal_id` |
| `TENANT`   | `tenant_id`                | `outlet_id`, `terminal_id`              |
| `OUTLET`   | `tenant_id`, `outlet_id`   | `terminal_id`                           |
| `TERMINAL` | `tenant_id`, `terminal_id` | nothing                                 |

---

## 5. Active & soft-delete

Only settings where:

- `active = true`
- `deleted_at IS NULL`

are considered during:

- resolution
- searches

**Deletion** is soft-delete:

- Set `deleted_at = NOW()`
- Set `active = false`

---

## 6. API semantics

### 6.1 Tenant API (read/resolve)

**Purpose**: allow tenants to query their effective settings.

**Endpoint**:

```
GET /tenant/settings/resolve
  ?tenantId=<uuid>
  &outletId=<uuid>       (optional)
  &terminalId=<uuid>     (optional)
  &namespaces=<csv>      (optional)
```

**Response**: `ApiResponse<List<ResolvedSettingView>>`

**Security**: `TENANT_ADMIN` or `TENANT_USER` role.

**Tenant context**: `tenantId` MUST come from request parameters (or security context in future).

---

### 6.2 Platform admin API (CRUD)

**Purpose**: platform administrators manage settings.

#### Search (paginated)

```
GET /platform/settings
  ?namespace=<string>    (optional, exact match)
  &settingKey=<string>   (optional, contains match)
  &level=<enum>          (optional)
  &tenantId=<uuid>       (optional)
  &active=<boolean>      (optional)
  &page=0&size=20&sort=namespace,asc
```

**Response**: `ApiResponse<TchPage<SettingView>>`

**Why PAGE**: Settings can grow significantly across tenants/outlets/terminals. Search/filter is required for admin workflows. Listing all settings is unsafe.

#### Get by ID

```
GET /platform/settings/{id}
```

**Response**: `ApiResponse<SettingView>`

#### Create

```
POST /platform/settings
Body: CreateSettingRequest {
  namespace, settingKey, settingValue, valueType, level,
  tenantId, outletId, terminalId
}
```

**Response**: `ApiResponse<SettingView>` (201 Created)

**Validation**:

- Registry check
- Type validation
- Uniqueness check
- Level requirements check

#### Update

```
PUT /platform/settings/{id}
Body: UpdateSettingRequest {
  settingValue,  // optional
  active         // optional
}
```

**Response**: `ApiResponse<SettingView>`

**Rules**:

- Only `settingValue` and `active` can be changed
- To change `level` or target IDs: delete and recreate

#### Delete

```
DELETE /platform/settings/{id}
```

**Response**: `ApiResponse<Void>` (204 No Content)

**Action**: soft-delete (set `deleted_at` + `active=false`)

---

## 7. Caching semantics

Resolution results are **cached** (internal implementation detail).

**Cache name**: `catalog:settings:resolved`  
**TTL**: 10 minutes  
**Key format**: `t=<tenantId>|o=<outletId>|m=<terminalId>|ns=<namespaces>`

**Invalidation**: automatic on write operations (`create`, `update`, `delete`).

Consumers MUST NOT be aware of caching — it's transparent.

---

## 8. Registered namespaces (initial catalog)

| Namespace             | Purpose                          |
| --------------------- | -------------------------------- |
| `pos.behavior`        | POS session/draw behavior        |
| `pos.session`         | Session lifecycle settings       |
| `pos.ticket`          | Ticket limits and rules          |
| `pos.receipt`         | Receipt printing                 |
| `ticket.verification` | Public ticket verification       |
| `payout.rules`        | Payout policies                  |
| `offline.sync`        | Offline synchronization          |
| `dashboard.stats`     | Dashboard display settings       |
| `ui.i18n`             | Internationalization             |
| `ui.theme`            | UI theme and density             |
| `ui.public_home`      | Public homepage variant          |
| `ops`                 | Operational overrides (timezone) |
| `ops.hours`           | Operating hours (JSON)           |
| `ops.outlet_day`      | Outlet day policy (JSON)         |

> **Note**: This list is not exhaustive. See `SettingsRegistry` for full catalog.

---

## 9. Examples

### 9.1 GLOBAL default

Create a global setting:

```json
POST /platform/settings
{
  "namespace": "pos.behavior",
  "settingKey": "require_open_session",
  "settingValue": "true",
  "valueType": "BOOLEAN",
  "level": "GLOBAL"
}
```

All tenants inherit this default unless overridden.

### 9.2 TENANT override

Override for tenant A:

```json
POST /platform/settings
{
  "namespace": "pos.behavior",
  "settingKey": "require_open_session",
  "settingValue": "false",
  "valueType": "BOOLEAN",
  "level": "TENANT",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000"
}
```

Tenant A will resolve to `false`, others to `true`.

### 9.3 OUTLET override

Override for outlet X within tenant A:

```json
POST /platform/settings
{
  "namespace": "pos.behavior",
  "settingKey": "require_open_session",
  "settingValue": "true",
  "valueType": "BOOLEAN",
  "level": "OUTLET",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "outletId": "789e0123-e45b-67c8-d901-234567890abc"
}
```

Outlet X will resolve to `true`, other outlets in tenant A to `false`.

### 9.4 Resolution query

Resolve for outlet X:

```
GET /tenant/settings/resolve
  ?tenantId=123e4567-e89b-12d3-a456-426614174000
  &outletId=789e0123-e45b-67c8-d901-234567890abc
  &namespaces=pos.behavior
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "namespace": "pos.behavior",
      "settingKey": "require_open_session",
      "valueType": "BOOLEAN",
      "settingValue": "true",
      "effectiveLevel": "OUTLET"
    }
  ]
}
```

---

## 10. Out of scope

- **User preferences** (theme, language) → `core/profile` (future)
- **Draw-specific overrides** → `core/draw` or dedicated catalog (future)
- **Business invariants** → defined in consuming domains (core/features)
- **Event emission** → settings do not emit domain events

---

## 11. Security & tenancy

### 11.1 Tenant context

For tenant endpoints:

- `tenantId` MUST be provided explicitly (for now)
- Future: extract from `TchRequestContext` (security context)

### 11.2 Access control

- `/tenant/settings/**` → `TENANT_ADMIN` or `TENANT_USER`
- `/platform/settings/**` → `PLATFORM_ADMIN`

### 11.3 RLS considerations

If Row-Level Security (RLS) is enabled:

- GLOBAL settings (`tenant_id IS NULL`) must be readable by all tenants
- Tenant-scoped rows filtered by `tenant_id = current_tenant_id`

**Recommended approach**: allow reads of `tenant_id IS NULL` for all authenticated users.

---

## 12. Migration from settings_bk

Existing module `catalog/settings_bk` will be **deprecated** in favor of `catalog/settings`.

Migration steps:

1. Update all consumers to use `SettingsCatalog` API
2. Migrate data (if table name changes)
3. Remove `settings_bk` module

---

## References

- **Catalog Rules 75**: `openspec/context/75-catalog-rules.md`
- **Pagination**: `docs/conventions/api/pagination.md`
- **API Response**: `docs/conventions/api/api_response.md`
- **Typed IDs**: `docs/conventions/typed_ids.md`

---

**Changelog**:

| Date       | Version | Change                                                             |
| ---------- | ------- | ------------------------------------------------------------------ |
| 2026-01-24 | 1.0     | Initial functional specification for new `catalog/settings` module |
