# Tasks: tenantgame-runtime-v1

## Jackson 3 rule

- [ ] Tous les champs JSON (`flags` → supprimé, futurs `settings`) utilisent **Jackson 3** (`tools.jackson.*`) — jamais `com.fasterxml.jackson.*`.
  - `TenantGameJpaEntity.flags` (JsonNode) → supprimer avec la colonne `flags`.
  - Nouveaux champs typés V1 (`game_code`, `visible_in_pos`, `display_order`, availability) : colonnes SQL typées, pas de JsonNode.
  - Si un champ JSON est ajouté : `@JdbcTypeCode(SqlTypes.JSON)` + `tools.jackson.databind.JsonNode`.
  - `UpdatePolicyRequest.flags` (JsonNode) → remplacer par `UpdateTenantGameSettingsRequest` avec champs typés.
  - `TenantGameView.flags` (JsonNode) → remplacer par `TenantGameAdminView` / `TenantGameRuntimeView` typés.
  - Toute sérialisation/désérialisation JSON passe par `JsonUtils` (common).

## catalog.game

- [ ] Ensure `GameCatalog` is the only API `platform.tenantgame` depends on.
- [ ] Add/confirm `catalog.game.read` and `catalog.game.manage`.
- [ ] Expose platform/SUPER_ADMIN endpoints under `/platform/catalog/games`.
- [ ] Block or guard structural updates on used games.
- [ ] Ensure stats/listRecent remain catalog-only, not sales analytics.
- [ ] Normalize game code consistently, preferably uppercase.
- [ ] Evict caches on platform catalog writes.

## tenant_game schema/model

- [ ] Update `tenant_game` CREATE TABLE statement with new columns (fresh DB strategy - no migration needed):
  - [ ] Add `game_code VARCHAR(32) NOT NULL`.
  - [ ] Add `visible_in_pos BOOLEAN NOT NULL DEFAULT true`.
  - [ ] Add `display_order INTEGER NOT NULL DEFAULT 0`.
  - [ ] Add `availability_enabled BOOLEAN NOT NULL DEFAULT false`.
  - [ ] Add `availability_days VARCHAR(64)`.
  - [ ] Add `start_local_time TIME`.
  - [ ] Add `end_local_time TIME`.
  - [ ] Remove/deprecate `flags` unless it has a validated V1 consumer.
  - [ ] Remove/deprecate free-form `policy`.
- [ ] Update `TenantGameJpaEntity` to match new schema.

## Services

- [ ] Rename/move `DefaultTenantGameApi` if present to `TenantGameApiAdapter`.
- [ ] Split `TenantGameService` into `TenantGameAdminService`, `TenantGameRuntimeService`, `TenantGameCatalogProjectionService`, `TenantGameConfigValidator`, and `TenantGameProvisioningService`.
- [ ] Move `ensureTenantGame` to provisioning service.
- [ ] Replace policy update with settings update.

## Admin endpoints

- [ ] Replace `/tenant/games` with `/admin/games`.
- [ ] Add `GET /admin/games`.
- [ ] Add `GET /admin/games/catalog`.
- [ ] Add `POST /admin/games/{gameCode}/enable`.
- [ ] Add `POST /admin/games/{gameCode}/disable`.
- [ ] Add `PATCH /admin/games/{gameCode}/settings`.
- [ ] Add `tenantgame.read/manage` permissions.
- [ ] Add entitlement/quota annotations or service checks.
- [ ] Add audit on writes.

## Runtime

- [ ] Add `TenantGameRuntimeView`.
- [ ] Expose runtime via `/tenant/games/runtime` or connected bootstrap.
- [ ] Ensure public/private runtime never exposes admin-only config.
- [ ] Ensure sales uses runtime view or stable query/API, not admin controller.

## Entitlements / quotas

- [ ] Add feature keys: `tenantgame.management`, `tenantgame.availability`, `tenantgame.advanced_settings`, `tenantgame.premium_games`.
- [ ] Add optional quota: `tenant_games.max_enabled` / `tenant_games.enabled`.
- [ ] Add dynamic feature check for gameCode-gated games.

## Tests

- [ ] Tenant admin lists tenant games.
- [ ] Tenant admin lists catalog with tenant status.
- [ ] Tenant admin enables active catalog game.
- [ ] Tenant admin cannot enable inactive/deleted catalog game.
- [ ] Tenant admin updates settings but cannot update global game fields.
- [ ] Availability rejects invalid days/time.
- [ ] Sales rejects disabled tenant game.
- [ ] Sales rejects unavailable tenant game window.
- [ ] Entitlement blocks premium game or advanced settings when plan disallows.
