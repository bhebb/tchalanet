# Design: tenanttheme-runtime-v1

## Boundary

```text
catalog.theme_preset
→ global preset definition and allowed customization surface

platform.tenanttheme
→ tenant preset choice, default mode, allowed token overrides, runtime resolution

identity/user_preference
→ user theme mode preference LIGHT/DARK/SYSTEM

platform.entitlement
→ plan-gated theme/branding capabilities
```

## Target package structure

```text
platform/tenanttheme/
  api/
    TenantThemeApi.java
    model/request/
    model/view/
    model/event/

  internal/
    adapter/TenantThemeApiAdapter.java
    model/TenantTheme.java
    service/
      TenantThemeAdminService.java
      TenantThemeRuntimeService.java
      TenantThemeResolver.java
      TenantThemeTokenValidator.java
      TenantThemeFallbackService.java
    persistence/
      entity/TenantThemeJpaEntity.java
      repository/
      adapter/
      mapper/
    web/admin/TenantThemeAdminController.java
    web/publicruntime/PublicThemeRuntimeController.java
```

## catalog.theme_preset

Global table.

Recommended columns:

```text
id
code
vendor
label_key
description
config
active
is_default
sort_order
created_at
updated_at
deleted_at
```

Recommended target for config:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "config", columnDefinition = "jsonb", nullable = false)
private JsonNode config;
```

If migration from `text` is too expensive in V1, keep `text` temporarily but validate and parse in mapper/service.

### config shape

`config` defines preset tokens and allowed customization surface.

```json
{
  "modes": ["light", "dark"],
  "defaultMode": "light",
  "tokens": {
    "light": {
      "color.primary": "#6750A4",
      "color.secondary": "#625B71",
      "color.surface": "#FFFFFF",
      "color.onSurface": "#1C1B1F",
      "shape.radius.md": "12px",
      "typography.fontFamily": "roboto",
      "density.default": "comfortable"
    },
    "dark": {
      "color.primary": "#D0BCFF",
      "color.secondary": "#CCC2DC",
      "color.surface": "#141218",
      "color.onSurface": "#E6E0E9",
      "shape.radius.md": "12px",
      "typography.fontFamily": "roboto",
      "density.default": "comfortable"
    }
  },
  "editableTokens": [
    "color.primary",
    "color.secondary",
    "shape.radius.md",
    "typography.fontFamily",
    "density.default"
  ],
  "allowedFonts": ["system", "roboto", "poppins"]
}
```

This global config is not returned raw to public runtime.

## tenant_theme

Recommended columns:

```text
id
tenant_id
preset_code
default_mode
token_overrides
active
is_default
version
created_at
updated_at
deleted_at
```

Recommended entity:

```java
@Entity
@Table(name = "tenant_theme")
@Getter
@Setter
public class TenantThemeJpaEntity extends BaseTenantEntity {

  @Column(name = "preset_code", nullable = false, length = 128)
  private String presetCode;

  @Column(name = "default_mode", nullable = false, length = 16)
  private String defaultMode = "SYSTEM";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "token_overrides", columnDefinition = "jsonb")
  private Map<String, String> tokenOverrides;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "is_default", nullable = false)
  private boolean defaultTheme = false;

  @Column(name = "version", nullable = false)
  private long version = 1L;
}
```

`metadata` is removed or migrated to `token_overrides`. Free-form metadata is forbidden.

## Views

### TenantThemeAdminView

```text
presetCode
defaultMode
tokenOverrides
resolvedTokens
editableTokens
allowedFonts
customTokensAllowed
customFontAllowed
version
updatedAt
```

### ThemeRuntimeView

```text
presetCode
mode
tokens
isDefault
version
```

Do not expose tenantId on public runtime, raw `theme_preset.config`, audit fields, or arbitrary metadata.

## Runtime resolution

Public:

```text
theme_preset.config
+ tenant_theme.token_overrides validated
+ tenant_theme.default_mode
= ThemeRuntimeView
```

Connected user:

```text
theme_preset.config
+ tenant_theme.token_overrides validated
+ user_preference.themeMode
= ThemeRuntimeView
```

## Endpoints

Platform catalog preset management:

```text
GET    /platform/catalog/theme-presets
GET    /platform/catalog/theme-presets/overview
POST   /platform/catalog/theme-presets
PUT    /platform/catalog/theme-presets/{id}
DELETE /platform/catalog/theme-presets/{id}
```

Tenant admin theme management:

```text
GET    /admin/theme                ← TenantThemeAdminView (presetCode, defaultMode, active, version)
GET    /admin/theme/presets        ← list ThemePresetView from catalog
POST   /admin/theme/preset         ← apply preset (permission: theme.manage, feature: THEME_PRESET_SELECTION)
PATCH  /admin/theme/settings       ← update defaultMode LIGHT|DARK|SYSTEM (permission: theme.manage) [V1]
PATCH  /admin/theme/tokens         ← [V2] token overrides (feature: THEME_CUSTOM_TOKENS)
DELETE /admin/theme                ← deactivate / reset to fallback (permission: theme.manage)
```

Public runtime:

```text
GET /public/theme/runtime
```

Connected bootstrap includes `theme` in `GET /tenant/me/bootstrap`.

## Permissions

Add:

```text
catalog.theme.read
catalog.theme.manage
theme.read
theme.manage
entitlement.read
```

SUPER_ADMIN gets catalog theme management and theme management. TENANT_ADMIN gets theme.read/theme.manage and entitlement.read for UI pre-validation. OPERATOR/CASHIER receive runtime theme through bootstrap, not admin permission.

## Entitlements / features

La customisation theme (choix preset, tokens à venir) est potentiellement payante. Runtime et lecture sont toujours gratuits.

| Action | Permission | Feature gate |
|---|---|---|
| `GET /admin/theme` | `theme.read` | aucun |
| `GET /admin/theme/presets` | `theme.read` | aucun |
| `POST /admin/theme/preset` | `theme.manage` | `theme.preset_selection` |
| `DELETE /admin/theme` | `theme.manage` | aucun |
| `GET /public/theme/runtime` | aucun | aucun |
| `PATCH /admin/theme/tokens` (V2) | `theme.manage` | `theme.custom_tokens` |

Feature keys définis dans `PlanFeatureKeys` (catalog.plan.api) — `THEME_PRESET_SELECTION`, `THEME_CUSTOM_TOKENS`, `THEME_CUSTOM_FONT`.

## Controller validation

Controllers use `ApiResponse<T>`, `@CurrentContext`, no tenantId in admin requests, `@PreAuthorize("hasPermission('theme.read/manage')")`, `@RequiredFeature` for static feature gates, Bean Validation for request shape, and delegate token validation to `TenantThemeTokenValidator`.

## Token validation

`TenantThemeTokenValidator` checks every override key is listed in preset `editableTokens`, values match allowed formats, font uses allowed code, unknown keys are rejected, and no raw CSS injection is accepted.

## Events/cache

`TenantThemeUpdatedEvent` is published after commit for cache invalidation and bootstrap refresh.

Catalog theme preset updates evict catalog caches and must invalidate tenant runtime theme cache or publish a preset-updated event.
