# Tasks: tenanttheme-runtime-v1

## Jackson 3 rule

- [ ] Tous les champs JSON (`metadata` → supprimé, `token_overrides` différé V2, `config` preset) utilisent **Jackson 3** (`tools.jackson.*`) — jamais `com.fasterxml.jackson.*`.
  - `TenantThemeJpaEntity.metadata` (JSONB) → supprimer, pas de `token_overrides` en V1.
  - `ThemePresetJpaEntity.config` (TEXT) → cible `@JdbcTypeCode(SqlTypes.JSON)` + `tools.jackson.databind.JsonNode`.
  - `TenantTheme.metadata` (Map<String,String>) → supprimer du domain model.
  - `ThemeRuntimeView.tokens` : `Map<String, String>` (résolu, pas de JsonNode brut).
  - Toute sérialisation/désérialisation JSON passe par `JsonUtils` (common).

## catalog.theme

- [ ] Add/confirm `ThemeCatalog` read-only API.
- [ ] Add `findDefault()` if useful.
- [ ] Ensure `ThemePresetAdminService` is platform/SUPER_ADMIN only.
- [ ] Move platform endpoints to `/platform/catalog/theme-presets`.
- [ ] Add `catalog.theme.read` and `catalog.theme.manage`.
- [ ] Validate preset config JSON before save.
- [ ] Add `sort_order`, `description` if missing.
- [ ] Decide V1: keep config text temporarily or migrate to jsonb.

## tenant_theme schema/model

- [ ] Update `tenant_theme` CREATE TABLE statement (fresh DB strategy - no migration needed):
  - [ ] Remove `metadata` column.
  - [ ] Add `default_mode VARCHAR(16) NOT NULL DEFAULT 'SYSTEM'`.
  - [ ] Add `active BOOLEAN NOT NULL DEFAULT true`.
  - [ ] Add `version BIGINT NOT NULL DEFAULT 1`.
  - [ ] No `token_overrides` column needed for V1 (deferred to V2).
- [ ] Update `TenantThemeJpaEntity` to match new schema (remove metadata field).
- [ ] Update entity, mapper, persistence adapter.
- [ ] Update domain model `TenantTheme`.

## Services

- [ ] Rename `DefaultTenantThemeApi` to `TenantThemeApiAdapter` and move to `internal/adapter`.
- [ ] Split `TenantThemeService` into `TenantThemeAdminService`, `TenantThemeRuntimeService`, `TenantThemeResolver`, `TenantThemeTokenValidator`, and `TenantThemeFallbackService`.
- [ ] Ensure fallback never returns null.
- [ ] Ensure runtime returns safe `ThemeRuntimeView`.

## Admin endpoints

- [ ] Replace `/tenant/theme` with `/admin/theme`.
- [ ] Add `GET /admin/theme`.
- [ ] Add `GET /admin/theme/presets`.
- [ ] Add `POST /admin/theme/preset`.
- [ ] Add `PATCH /admin/theme/settings`.
- [ ] Add optional `PATCH /admin/theme/tokens`.
- [ ] Add `DELETE /admin/theme`.
- [ ] Use `ApiResponse<T>`.
- [ ] Use permission annotations and `@RequiredFeature` where needed.
- [ ] Add audit to writes.

## Public/runtime endpoints

- [ ] Add `GET /public/theme/runtime`.
- [ ] Add `ThemeRuntimeView`.
- [ ] Ensure public runtime uses tenant resolver/context.
- [ ] Include theme in connected `/tenant/me/bootstrap`.

## Entitlements

- [ ] Add feature keys: `theme.preset_selection`, `theme.custom_tokens`, `theme.custom_font`, `branding.logo`, `branding.custom_assets`.
- [ ] Use `RequiredFeatureAspect` for static checks.
- [ ] Use `EntitlementApi.requireFeature(...)` for dynamic token/preset checks.

## Tests

- [ ] Tenant admin can select preset if permission and feature allow.
- [ ] Tenant admin cannot update tokens without feature.
- [ ] Unknown token override is rejected.
- [ ] Public runtime does not expose raw config or tenantId.
- [ ] Fallback preset is applied when selected preset is inactive.
- [ ] Theme updated event is published after commit.
