# Change: tenanttheme-runtime-v1

## Decision

Tchalanet separates global theme presets from tenant theme runtime configuration.

```text
catalog.theme        = global theme presets managed by SUPER_ADMIN
platform.tenanttheme = tenant theme choice, allowed overrides, and runtime theme resolution
```

Tenant theme runtime exposes `ThemeRuntimeView`, not `TenantThemeView` and not raw `theme_preset.config`.

`tenant_theme.metadata` is replaced by `token_overrides`.

Theme customization is controlled by both access-control permissions and plan entitlements when applicable.

## Why

The current model risks mixing global presets, tenant theme selection, public runtime tokens, metadata/custom overrides, and admin/runtime views.

If `tenant_theme` contains only `tenant_id -> preset_code`, it is close to a link table. That is acceptable for V1, but the model must prepare controlled overrides without free-form metadata.

## What

Global:

- `theme_preset` defines presets, tokens, default mode, editable token surface, allowed fonts.
- only SUPER_ADMIN manages presets.

Tenant:

- `tenant_theme` stores selected preset, default mode, token overrides, active/default, version.
- tenant admins can choose presets and default mode.
- custom token/font changes require permissions plus entitlements.
- runtime endpoints return safe resolved tokens.

## Impact

Database:

- `theme_preset.config` should target JSON/JSONB.
- `tenant_theme.metadata` becomes `token_overrides`.
- `tenant_theme` adds `default_mode`, `active`, `version`.

Backend:

- split `TenantThemeService` into admin/runtime/resolver/validator services.
- rename `DefaultTenantThemeApi` to `TenantThemeApiAdapter`.
- add `/admin/theme/**` and `/public/theme/runtime`.

Frontend:

- public homepage can call `/public/theme/runtime` in parallel with settings, i18n, and PageModel.
- connected bootstrap can include `ThemeRuntimeView` resolved with user preference mode.

## Non-goals

- Full theme builder UI.
- Arbitrary CSS injection.
- Custom font upload.
- Cloudinary logo/branding workflow.
- Moving i18n/settings into tenanttheme.

## Success criteria

- `TenantThemeView` is not used as public runtime contract.
- `ThemeRuntimeView` contains only safe resolved tokens.
- `metadata` is removed/renamed to `token_overrides`.
- Admin endpoints use permissions and entitlements.
- Public runtime endpoint exists or is wired into public bootstrap.
