# Tasks

## 1. API and models

- [ ] Create `platform.publiccontent.api.PublicContentApi`.
- [ ] Create API models:
  - [ ] `PublicContentItemView`
  - [ ] `PublicContentAdminItemView`
  - [ ] `PublicContentStatus`
  - [ ] `PublicContentSurface`
  - [ ] `PublicContentSourceType`
- [ ] Include fields: id, sourceType, sourceId, title, description/snippet, contentHtml, link, author, status, surfaces, publishedAt, expiresAt, priority, categories.

## 2. Internal services migration

- [ ] Move/rename `NewsAggregationService` to `PublicContentAggregationService`.
- [ ] Move/rename `PublicNewsService` to `PublicContentQueryService`.
- [ ] Move/rename `AdminNewsService` to `PublicContentAdminService`.
- [ ] Move `ExternalNewsService` to `ExternalRssNewsService`.
- [ ] Move `InternalNewsService` to `InternalPublicContentService`.
- [ ] Move `HiddenNewsService` to `HiddenPublicContentService`.
- [ ] Move provider classes: `LotteryDailyRssClient`, `RomeNewsMapper`, `NewsProvider`.
- [ ] Move scheduler to `platform.publiccontent.internal.scheduler.PublicContentRefreshScheduler`.

## 3. Cache correctness

- [ ] Split `refreshExternalSnapshot()` from `getExternalSnapshot()`.
- [ ] Ensure public requests read cache first and refresh only as fallback.
- [ ] Add explicit aggregated public cache key if needed.
- [ ] Replace `forceRefresh()` empty snapshot behavior with real refresh + eviction.
- [ ] Keep cache names functional and declare TTL through `CacheSpecProvider` if using Spring cache.

## 4. Admin HTTP

- [ ] Move admin controller to `/platform/public-content/news` or `/platform/news`.
- [ ] Keep `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`.
- [ ] Add `@AuditLog` on upsert/status/hide/show/force-refresh.
- [ ] Add `@Valid` on request bodies.
- [ ] Replace raw `UUID` path variable with `String` ID or a compatible typed wrapper, because external IDs may not be UUID.
- [ ] Use `ApiResponse` explicitly for new code if consistent with current controller style.

## 5. Public HTTP

- [ ] Keep `/public/news` endpoint.
- [ ] Add optional `limit` and `surface` support if needed.
- [ ] Ensure public response model does not expose internal implementation classes.

## 6. PageModel integration

- [ ] Update public home provider/assembler to inject `PublicContentApi`.
- [ ] Remove imports from `features.news` in `features.pagemodel`.
- [ ] Add integration tests for public home payload with internal + external news.

## 7. Tests

- [ ] Unit test internal upsert does not erase previous items.
- [ ] Unit test status/publication window filtering.
- [ ] Unit test surface filtering.
- [ ] Unit test hidden overlay filtering.
- [ ] Unit test RSS failure returns cached/stale or empty gracefully.
- [ ] Controller tests for SUPER_ADMIN protection and audit metadata if available.
