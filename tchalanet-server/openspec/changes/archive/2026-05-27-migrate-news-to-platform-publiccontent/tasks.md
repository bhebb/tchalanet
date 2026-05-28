# Tasks

## 1. API and models

- [x] Create `platform.publiccontent.api.PublicContentApi`.
- [x] Create API models:
  - [x] `PublicContentItemView`
  - [x] `PublicContentAdminItemView`
  - [x] `PublicContentStatus`
  - [x] `PublicContentSurface`
  - [x] `PublicContentSourceType`
- [x] Include fields: id, sourceType, sourceId, title, description/snippet, contentHtml, link, author, status, surfaces, publishedAt, expiresAt, priority, categories.

## 2. Internal services migration

- [x] Move/rename `NewsAggregationService` to `PublicContentAggregationService`.
- [x] Move/rename `PublicNewsService` to `PublicContentQueryService`.
- [x] Move/rename `AdminNewsService` to `PublicContentAdminService`.
- [x] Move `ExternalNewsService` to `ExternalRssNewsService`.
- [x] Move `InternalNewsService` to `InternalPublicContentService`.
- [x] Move `HiddenNewsService` to `HiddenPublicContentService`.
- [x] Move provider classes: `LotteryDailyRssClient`, `RomeNewsMapper`, `NewsProvider`.
- [x] Move scheduler to `platform.publiccontent.internal.scheduler.PublicContentRefreshScheduler`.

## 3. Cache correctness

- [x] Split `refreshExternalSnapshot()` from `getExternalSnapshot()`.
- [x] Ensure public requests read cache first and refresh only as fallback.
- [x] Add explicit aggregated public cache key if needed.
- [x] Replace `forceRefresh()` empty snapshot behavior with real refresh + eviction.
- [x] Keep cache names functional and declare TTL through `CacheSpecProvider` if using Spring cache.

## 4. Admin HTTP

- [x] Move admin controller to `/platform/public-content/news`.
- [x] Keep `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`.
- [x] Add `@AuditLog` on upsert/status/hide/show. *(done; forceRefresh intentionally excluded — no target entity)*
- [x] Add `@Valid` on request bodies.
- [x] Replace raw `UUID` path variable with `String` ID.
- [ ] Use `ApiResponse` explicitly for new code if consistent with current controller style. *(returns raw objects — needs review)*

## 5. Public HTTP

- [x] Keep `/public/news` endpoint.
- [x] Add optional `limit` support.
- [x] Add optional `surface` query param support. *(added `@RequestParam(defaultValue = "PUBLIC_HOME") PublicContentSurface surface`)*
- [x] Ensure public response model does not expose internal implementation classes.

## 6. PageModel integration

- [x] Update public home provider/assembler to inject `PublicContentApi`.
- [x] Remove imports from `features.news` in `features.pagemodel`.
- [ ] Add integration tests for public home payload with internal + external news.

## 7. Tests

- [ ] Unit test internal upsert does not erase previous items.
- [ ] Unit test status/publication window filtering.
- [ ] Unit test surface filtering.
- [ ] Unit test hidden overlay filtering.
- [ ] Unit test RSS failure returns cached/stale or empty gracefully.
- [ ] Controller tests for SUPER_ADMIN protection and audit metadata if available.
