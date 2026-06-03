# Design: platform-tenant-v1

## Ownership

```text
platform.tenant = owner of tenant table and tenant lifecycle.
```

`catalog.tenant` is removed or deprecated. If retained temporarily, it is only a legacy read-only projection and must not write, configure, provision, or own tenant lifecycle.

## Target package structure

```text
platform/tenant/
  api/
    TenantApi.java
    model/request/
    model/result/
    model/view/

  internal/
    adapter/TenantApiAdapter.java

    resolver/
      TenantRegistryReader.java
      JdbcTenantRegistryReader.java
      TenantContextLookupService.java

    service/
      TenantAdminService.java
      TenantProvisioningService.java
      TenantRuntimeService.java
      TenantReadinessService.java
      TenantSettingsService.java
      TenantValidator.java

    persistence/
      entity/TenantJpaEntity.java
      repository/TenantJpaRepository.java
      adapter/TenantPersistenceAdapter.java
      mapper/TenantMapper.java

    web/platform/PlatformTenantController.java
    web/admin/TenantAdminController.java
    web/runtime/TenantRuntimeController.java
```

## Pre-context tenant lookup

Tenant resolution is needed before `TchRequestContext` and RLS can be fully bound.

Allowed lookup:

```text
tenantCode -> tenantId/status/timezone/currency/defaultLanguage/defaultLocale
tenantId   -> status/timezone/currency/defaultLanguage/defaultLocale
```

The reader may use `rawDataSource` only for:

- tenant resolution before context binding;
- auth/bootstrap;
- scheduler/batch active tenant listing;
- platform-admin registry listing.

It must be read-only, isolated under `internal/resolver`, and not used by sales, payout, settlement, tenant-admin business screens, or domain queries.

## TenantJpaEntity

The existing `tenant` table remains the persistence table.

Recommended fields:

```text
id
code
name
type
status
timezone
currency
default_language
default_locale
address_id
active_theme_id // optional legacy, prefer tenanttheme runtime going forward
config           // only platform.tenant-owned base config (JSONB)
created_at
updated_at
deleted_at
```

### Config field behavior

`config` is a JSONB column containing platform.tenant-owned base configuration.

**Initialization**: During tenant creation, `config` is initialized by loading and deep-merging JSON resource files from `classpath*:tenantconfig/*.json` in sorted order:
- `calendar_rules_config.json` — `rules.businessCalendar` (defaultOpen, closedWeekdays, holidaySalesAllowed)
- `document_config.json` — `document.receipt` (enabled, template, paper size, QR code)
- `fees_config.json` — `communication.buyerTicketDelivery` (frais d'envoi SMS/WhatsApp/email — amount, currency, paidBy)
- `locale_config.json` — `locale` (defaultLanguage, defaultLocale, supportedLanguages, fallbackLanguage)

**Structure**: After merge, config contains exactly these top-level sections:
```json
{
  "rules": {
    "businessCalendar": {
      "defaultOpen": true,
      "closedWeekdays": [],
      "holidaySalesAllowed": false
    }
  },
  "document": {
    "receipt": {
      "enabled": true,
      "defaultTemplateKey": "sales.ticket.receipt.v1",
      "defaultPaperSize": "RECEIPT_80MM",
      "showQrCode": true
    }
  },
  "communication": {
    "buyerTicketDelivery": {
      "sms": { "enabled": true, "amount": 5.00, "currency": "HTG", "paidBy": "BUYER" },
      "whatsapp": { "enabled": true, "amount": 5.00, "currency": "HTG", "paidBy": "BUYER" },
      "email": { "enabled": true, "amount": 0.00, "currency": "HTG", "paidBy": "TENANT" }
    }
  },
  "locale": {
    "defaultLanguage": "fr",
    "defaultLocale": "fr-HT",
    "supportedLanguages": ["fr", "ht", "en"],
    "fallbackLanguage": "fr"
  }
}
```

**Validation**: `config` must be validated by `TenantConfigValidator` for ALL sections:

| Section | Validated | Method |
|---|---|---|
| `rules.businessCalendar` | ❌ missing | `validateRulesConfig()` — add |
| `document.receipt` | ✅ exists | `validateDocumentSettings()` — preserve |
| `communication.buyerTicketDelivery` | ✅ exists | `validateCommunicationSettings()` — preserve |
| `locale` | ❌ missing | `validateLocaleConfig()` — add |

- On creation: validate all 4 sections present, no unknown top-level keys
- On update: validate ALL sections, reject unknown keys, reject malformed values

**`validateRulesConfig()`** (new):
- `rules.businessCalendar.defaultOpen` must be boolean
- `rules.businessCalendar.closedWeekdays` must be array of valid weekday codes (e.g. `MON`, `TUE`…)
- `rules.businessCalendar.holidaySalesAllowed` must be boolean

**`validateLocaleConfig()`** (new):
- `locale.defaultLanguage` must be non-blank IETF language code
- `locale.defaultLocale` must be non-blank locale code
- `locale.supportedLanguages` must be non-empty array
- `locale.fallbackLanguage` must be in `supportedLanguages`

**Immutability**: Some config keys may be immutable after tenant creation (e.g., locale defaults). Document which keys are mutable vs immutable.

**Not allowed inside `tenant.config`**: games, theme tokens, permissions, pricing odds, exposure limits, promotions, sales/payout/settlement rules, terminal/session/outlet data.

`config` must not become a dumping ground. If new tenant-level settings are needed, create explicit columns or a separate capability module.

## Views

### TenantAdminView

May include id, code, name, type, status, timezone, currency, defaultLanguage, defaultLocale, addressId, createdAt, updatedAt, readiness.

### TenantRuntimeView

Public/private safe view includes only tenantCode, displayName, statusPublic, timezone, currency, defaultLanguage, defaultLocale, supportedLocales.

It must not expose raw config JSON, deleted_at, address internals, raw status reasons, or admin metadata.

## Endpoints

Platform scope:

```text
GET    /platform/tenants
POST   /platform/tenants
GET    /platform/tenants/{tenantId}
PATCH  /platform/tenants/{tenantId}
POST   /platform/tenants/{tenantId}/activate
POST   /platform/tenants/{tenantId}/suspend
POST   /platform/tenants/{tenantId}/archive
POST   /platform/tenants/{tenantId}/provision
POST   /platform/tenants/{tenantId}/admins
```

Tenant admin scope:

```text
GET    /admin/tenant
PATCH  /admin/tenant/settings
GET    /admin/tenant/readiness
```

Runtime:

```text
GET /public/tenant/runtime
GET /tenant/runtime
```

## Permissions

Add:

```text
tenant.read
tenant.create
tenant.update
tenant.activate
tenant.suspend
tenant.archive
tenant.admin.create
tenant.override
tenant.readiness.read
tenant.settings.read
tenant.settings.manage
```

SUPER_ADMIN gets platform tenant management permissions. TENANT_ADMIN gets `tenant.settings.read`, `tenant.settings.manage`, and `tenant.readiness.read` for its own tenant.

## Entitlements

`platform.tenant` does not replace `platform.entitlement`.

Tenant operations that enable plan-gated features must use `@RequiredFeature` / `@RequiredQuota` or `EntitlementApi` inside services for dynamic checks.

Add `entitlement.read` for connected bootstrap UI pre-validation.

## Controller validation

Controllers use `@CurrentContext`, do not accept tenantId in `/admin/**`, use `ApiResponse<T>`, use typed IDs, use `@PreAuthorize("hasPermission('...')")`, validate request shape, and delegate business validation to services.

## Services

Split tenant lifecycle responsibilities across focused services:

- **TenantAdminService** — Admin operations (create, activate, suspend, archive, identity updates)
- **TenantProvisioningService** — Provisioning logic (first admin creation, config initialization, readiness checks)
- **TenantRuntimeService** — Runtime views (public/private safe tenant runtime endpoints)
- **TenantReadinessService** — Readiness summary (check if tenant is ready for sales/operations)
- **TenantSettingsService** — Mutable tenant settings (config updates, internal settings management)
- **TenantConfigValidator** — Config validation (initialize, update, all sections)

**Config initialization** (provisioning):
```java
// In TenantProvisioningService
JsonNode defaultConfig = loadAndMergeConfigResources(); // load classpath*:tenantconfig/*.json
TenantConfigValidator.validateInitialConfig(defaultConfig); // ensure all sections present
tenant = TenantConfig.createDraft(..., defaultConfig);
```

**Config update** (settings):
```java
// In TenantSettingsService
TenantConfigValidator.validateConfigUpdate(newConfig); // validate all sections, reject unknown keys
tenant = tenant.updateConfig(newConfig, now);
```

## Migration plan

1. Introduce `platform.tenant` package.
2. Move `TenantJpaEntity` and write repository from `platform.tenantconfig`.
3. Extract config initialization logic to `TenantProvisioningService`.
4. Create `TenantConfigValidator` with comprehensive validation for all config sections.
5. Move raw registry reader from `catalog.tenant` into `platform.tenant.internal.resolver`.
6. Fully migrate all `catalog.tenant` usages to `platform.tenant` (including scheduler/batch).
7. Remove `catalog.tenant` package after migration complete.
8. Rename `platform.tenantconfig` references to `platform.tenant`.
