# Change: migrate-news-to-platform-publiccontent

## Status

Proposed.

## Summary

Migrate the current `features.news` implementation into a stable transversal capability named `platform.publiccontent`.

This is not just a rename from news to platform news. The product need is broader:

- consume a public RSS feed, initially LotteryDaily or equivalent;
- allow Tchalanet/platform administrators to publish internal network news/announcements;
- display public content on the public home page;
- display internal/network content on tenant admin, platform admin and later POS dashboards;
- keep this separate from transactional `platform.notification`.

## Why

`features.news` is currently consumed by `features.pagemodel` through Java imports. This violates the rule that `features` is a leaf layer and must not be used as a shared library.

`platform.publiccontent` is the right owner because public/news content is transversal, stateful/cacheable, platform-managed, and not a core financial/business invariant.

## Scope

### In scope

- Create `platform.publiccontent.api` with stable read/admin-facing Java contracts.
- Move RSS provider, aggregation, internal news, hidden content overlay, cache and scheduler from `features.news` into `platform.publiccontent.internal`.
- Keep public HTTP endpoint under `/public/news`.
- Move admin endpoint to `/platform/public-content/news` or `/platform/news`, protected by SUPER_ADMIN.
- Add audience/surface targeting for internal content:
  - `PUBLIC_HOME`
  - `TENANT_ADMIN_DASHBOARD`
  - `PLATFORM_ADMIN_DASHBOARD`
  - `POS_DASHBOARD`
- Expose API methods used by PageModel providers and dashboards.
- Add audit for admin writes and force refresh.
- Fix current cache/snapshot weaknesses.

### Out of scope V1

- User-level personalization to hide content/news per dashboard. This is V2.
- Delivery/read-status semantics. That belongs to `platform.notification`.
- Tenant-authored public content. V1 is platform/network-authored.

## Target package shape

```text
platform/publiccontent/
  api/
    PublicContentApi.java
    model/
      PublicContentItemView.java
      PublicContentStatus.java
      PublicContentSurface.java
      PublicContentSourceType.java
      PublicContentAdminItemView.java
  internal/
    news/
      PublicContentQueryService.java
      PublicContentAdminService.java
      PublicContentAggregationService.java
      InternalPublicContentService.java
      ExternalRssNewsService.java
      HiddenPublicContentService.java
      PublicContentCache.java
      PublicContentConfigProperties.java
    news/provider/
      NewsProvider.java
      LotteryDailyRssClient.java
      RomeNewsMapper.java
    scheduler/
      PublicContentRefreshScheduler.java
    web/
      PublicNewsController.java
      PlatformPublicContentAdminController.java
```

## API sketch

```java
package com.tchalanet.server.platform.publiccontent.api;

public interface PublicContentApi {
  List<PublicContentItemView> listPublicHomeNews(int limit);
  List<PublicContentItemView> listTenantAdminDashboardNews(int limit);
  List<PublicContentItemView> listPlatformAdminDashboardNews(int limit);
  List<PublicContentItemView> listPosDashboardNews(int limit);
}
```

## Behavioral rules

- RSS external content is public/general by default.
- Internal platform content may target one or more surfaces.
- Internal content is ordered before external content where appropriate.
- Content visibility uses status + publishedAt/expiresAt + hidden overlay + surface/audience.
- `platform.publiccontent` does not create action-required notifications.
- Transactional/targeted user messages remain in `platform.notification`.

## Migration plan

1. Create `platform.publiccontent.api` and internal package skeleton.
2. Move shared models from `features.news.shared` into platform publiccontent models.
3. Rename/move `PublicNewsService` to `PublicContentQueryService`.
4. Rename/move `NewsAggregationService` to `PublicContentAggregationService`.
5. Move `ExternalNewsService`, `InternalNewsService`, `HiddenNewsService`, `NewsCache`, `LotteryDailyRssClient`, scheduler and config.
6. Update `features.pagemodel.dynamic.providers.publichome.PublicHomePayloadAssembler` to inject `PublicContentApi` instead of `features.news.publicnews.PublicNewsService`.
7. Add dashboard provider usage later through integration spec.
8. Delete Java dependencies from any feature to `features.news`.
9. Remove or deprecate `features.news` package after route compatibility is confirmed.

## Current code issues to fix during migration

- `InternalNewsService.save(...)` appears to write a new snapshot from one item and may erase existing internal items. Replace with full-snapshot upsert by ID.
- `ExternalNewsService.fetchArticles()` currently calls `fetchSnapshot()` and may refetch RSS on every public request. Split `refreshSnapshot()` from `getCachedSnapshot()`.
- `AdminNewsService.forceRefresh()` currently refreshes external then writes an empty external snapshot. Replace with cache evict/refresh semantics.
- Use injected `Clock` for all `now` values.
- Use project `IdGenerator` for internal UUID-based IDs or a string wrapper if IDs can be external/non-UUID.
- Add `@Valid` and Bean Validation on admin request models.
- Add audit on all write/admin endpoints.

## Risks

- Existing public `/public/news` clients must remain compatible.
- RSS provider failure must not break public homepage.
- Cached external feed must be stale-tolerant.

## Rollout

- Keep old route shape `/public/news`.
- Add platform admin route `/platform/public-content/news`.
- Update PageModel provider first, then delete feature-to-feature dependency.
