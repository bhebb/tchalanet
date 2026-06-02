# Tasks: tenantgame-runtime-v1

## Jackson 3 rule

- [x] `TenantGameJpaEntity.flags` (JsonNode) — supprimé avec la colonne `flags`.
- [x] Nouveaux champs typés V1 : colonnes SQL typées, pas de JsonNode.
- [x] `UpdatePolicyRequest.flags` — remplacé par `UpdateTenantGameSettingsRequest` avec champs typés.
- [x] `TenantGameView.flags` — remplacé par `TenantGameAdminView` / `TenantGameRuntimeView` typés.

## catalog.game

- [x] `GameCatalog` est la seule API dont dépend `platform.tenantgame`.
- [x] `GameAdminController` déplacé vers `/platform/catalog/games` (depuis `/platform/games`).
- [x] Permissions : `hasPermission('catalog.game.manage')` (depuis `hasAuthority('SUPER_ADMIN')`).
- [x] Code normalisé en uppercase dans `GameAdminService.create()`.
- [x] Structural fields (`combination`, `minDigits`, `maxDigits`, `category`) bloqués si le jeu est utilisé par un `tenant_game` (query JDBC directe sur la table pour éviter layer violation).
- [x] Soft-delete bloqué si le jeu est utilisé par un `tenant_game`.
- [x] Cache eviction déjà présent sur tous les writes (`@CacheEvict`).

## tenant_game schema/model

- [x] Updated `tenant_game` CREATE TABLE statement (fresh DB strategy):
  - [x] Added `game_code VARCHAR(32) NOT NULL`.
  - [x] Added `visible_in_pos BOOLEAN NOT NULL DEFAULT true`.
  - [x] Added `display_order INTEGER NOT NULL DEFAULT 0`.
  - [x] Added `availability_enabled BOOLEAN NOT NULL DEFAULT false`.
  - [x] Added `availability_days VARCHAR(64)`.
  - [x] Added `start_local_time TIME`.
  - [x] Added `end_local_time TIME`.
  - [x] Removed `flags`.
- [x] `TenantGameJpaEntity` updated to match new schema.
- [x] `TenantGame` domain model updated (catalog fields removed, V1 fields added).

## Services

- [x] `DefaultTenantGameApi` → `TenantGameApiAdapter` in `internal/adapter/`.
- [x] `TenantGameService` split into:
  - [x] `TenantGameAdminService` — enable, disable, updateSettings, listGames.
  - [x] `TenantGameRuntimeService` — runtime view for POS.
  - [x] `TenantGameCatalogProjectionService` — catalog + tenant status.
  - [x] `TenantGameConfigValidator` — validation stakes, days, displayName.
  - [x] `TenantGameProvisioningService` — ensureTenantGame.
- [x] `ensureTenantGame` moved to `TenantGameProvisioningService`.
- [x] Policy update replaced by settings update.

## Admin endpoints

- [x] Replaced `/tenant/games` with `/admin/games`.
- [x] `GET /admin/games`.
- [x] `GET /admin/games/catalog`.
- [x] `POST /admin/games/{gameCode}/enable`.
- [x] `POST /admin/games/{gameCode}/disable`.
- [x] `PATCH /admin/games/{gameCode}/settings`.
- [x] `@PreAuthorize("hasPermission('tenantgame.read/manage')")` on controller.
- [ ] Entitlement/quota annotations — deferred.
- [x] Notices on writes (via events).

## Runtime

- [x] `TenantGameRuntimeView` added.
- [x] `GET /tenant/games/runtime` endpoint added.
- [x] Runtime view exposes only safe fields (no internal IDs).

## Entitlements / quotas

- [ ] Feature keys deferred.
- [ ] Quota deferred.

## Tests

- [ ] Unit/integration tests — deferred to unit-test-task.
