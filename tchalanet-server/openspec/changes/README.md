# Tchalanet OpenSpec Pack — publiccontent + core.analytics

This ZIP contains 5 separated OpenSpec changes:

1. `migrate-news-to-platform-publiccontent`
2. `create-core-analytics`
3. `migrate-feature-stats-to-core-analytics`
4. `migrate-reporting-readers-to-core-analytics`
5. `integrate-analytics-publiccontent-dashboards`

Intent:

- Move reusable news/public content out of `features.news` into `platform.publiccontent`.
- Create `core.analytics` as the owner of reliable projections, KPIs, recompute and purge.
- Migrate legacy `features.stats` persistence/listeners/read services into `core.analytics`.
- Keep `features.reporting` focused on UI/export generation while analytics reads move to `core.analytics`.
- Integrate public home, tenant admin, platform admin/superadmin, cashier POS and web/mobile PageModel surfaces with stable APIs/queries.

Design decision summary:

```text
platform.publiccontent
= editorial/public/network content, RSS aggregation, internal announcements, surface/audience targeting

platform.notification
= transactional/operational targeted messages, read/delivery state, action required

core.analytics
= KPIs, projections, aggregates, recompute, purge, DB indexes, event projectors

features.reporting
= report UI/export orchestration only

features.tenantadmin / features.platformadmin / features.cashier / features.pagemodel
= BFF/PageModel consumers via QueryBus or stable platform APIs
```
