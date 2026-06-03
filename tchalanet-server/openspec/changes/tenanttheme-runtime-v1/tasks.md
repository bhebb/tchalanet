# Tasks: tenanttheme-runtime-v1

## Jackson 3 rule

- [x] `TenantThemeJpaEntity.metadata` (JSONB) — supprimé.
- [x] `ThemePresetJpaEntity.config` (TEXT) — conservé TEXT pour V1 (migration jsonb différée).
- [x] `TenantTheme.metadata` (Map<String,String>) — supprimé du domain model.
- [x] `ThemeRuntimeView.tokens` : `Map<String, String>` (résolu, pas de JsonNode brut).
- [x] Jackson 3 utilisé dans `TenantThemeRuntimeService` pour extraire les tokens du config.

## catalog.theme

- [x] `ThemeCatalog` API confirmée read-only.
- [x] `findDefault()` ajouté à `ThemeCatalog` et implémenté dans `ThemePresetCatalogImpl`.
- [x] `ThemePresetAdminService` — platform/SUPER_ADMIN only.
- [x] URL déplacée vers `/platform/catalog/theme-presets` (depuis `/platform/theme-presets`).
- [x] Permission : `hasPermission('catalog.theme.manage')` (depuis `hasAuthority('SUPER_ADMIN')`).
- [x] `sort_order`, `description` ajoutés dans CREATE TABLE `theme_preset`.
- [x] Validate preset config JSON before save — `ThemePresetConfigValidator` (modes, defaultMode, tokens structure, editableTokens/allowedFonts arrays).
- [x] `TenantThemeFallbackService` utilise `findDefault()` directement.

## tenant_theme schema/model

- [x] Updated `tenant_theme` CREATE TABLE statement (fresh DB strategy):
  - [x] Removed `metadata` column.
  - [x] Added `default_mode VARCHAR(16) NOT NULL DEFAULT 'SYSTEM'`.
  - [x] Added `active BOOLEAN NOT NULL DEFAULT true`.
  - [x] `version` updated to DEFAULT 1.
  - [x] `token_overrides jsonb` column ajoutée (nullable, null = aucun override).
- [x] `TenantThemeJpaEntity` updated.
- [x] `TenantThemePersistenceAdapter` updated (deactivate soft, findActiveByTenantId added).
- [x] `TenantThemeJpaRepository` updated (findByTenantIdAndActive added).
- [x] Domain model `TenantTheme` updated (defaultMode, active added; metadata removed).

## Services

- [x] `DefaultTenantThemeApi` → `TenantThemeApiAdapter` in `internal/adapter`.
- [x] `TenantThemeService` split into:
  - [x] `TenantThemeAdminService` — apply preset, deactivate, admin view.
  - [x] `TenantThemeRuntimeService` — runtime view resolution with token extraction.
  - [x] `TenantThemeTokenValidator` — V1 stub (no overrides).
  - [x] `TenantThemeFallbackService` — already existed, updated to use `findDefault()`.
- [x] Fallback never returns null.
- [x] Runtime returns safe `ThemeRuntimeView`.

## Admin endpoints

- [x] Replaced `/tenant/theme` with `/admin/theme`.
- [x] `GET /admin/theme`.
- [x] `GET /admin/theme/presets`.
- [x] `POST /admin/theme/preset`.
- [x] `DELETE /admin/theme`.
- [x] Use `ApiResponse<T>`.
- [x] Permission annotations `theme.read/manage`.
- [x] `PATCH /admin/theme/settings` — update `defaultMode` (LIGHT/DARK/SYSTEM), gate `theme.manage`.
- [ ] `PATCH /admin/theme/tokens` — deferred V2 (token overrides).

## Public/runtime endpoints

- [x] `GET /public/theme/runtime` — `PublicThemeRuntimeController`.
- [x] `GET /tenant/theme/runtime` — authenticated runtime endpoint.
- [x] `ThemeRuntimeView` added.
- [x] Public runtime uses tenant context.

## Entitlements

- [x] `PlanFeatureKeys.THEME_PRESET_SELECTION` ajouté dans `PlanFeatureKeys`.
- [x] `PlanFeatureKeys.THEME_CUSTOM_TOKENS`, `THEME_CUSTOM_FONT` ajoutés (V2 readiness).
- [x] `@RequiredFeature(THEME_PRESET_SELECTION)` sur `POST /admin/theme/preset`.
- [x] Token/font overrides gated deferred V2 — framework en place.
- Logique : preset selection = plan payant potentiel. Création tenant + runtime = toujours gratuit.

## Tests

- [x] `TenantThemeTokenValidatorTest` — 3 tests (null, empty, non-empty rejected in V1).
- [x] `TenantThemeRuntimeServiceTest` — 7 tests (mode resolution, token extraction, fallback, inactive preset).
- [x] `TenantThemeAdminServiceTest` — 7 tests (apply preset, settings update, validation, version increment).
- [x] `ThemePresetConfigValidatorTest` — 11 tests (valid, null, blank, invalid JSON, missing fields, unknown mode key).
