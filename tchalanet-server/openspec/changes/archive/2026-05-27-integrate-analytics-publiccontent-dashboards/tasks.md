# Tasks

## 1. Public home

- [x] Update `PublicHomePayloadAssembler` to use `PublicContentApi`.
- [x] Keep `home.news` payload shape stable.
- [x] Continue using `json_file` fragments for hero/features/tchala.
- [ ] Add tests for internal + external public news aggregation.

## 2. Tenant admin dashboard

- [x] Replace `TenantDashboardStatsService` injection with `QueryBus.ask(GetTenantDashboardStatsQuery)`.
- [x] Add optional comparison mode: today/yesterday and 7 days/previous 7 days.
- [x] Add public content/internal announcements widget using `PublicContentApi`.
- [x] Keep readiness but make games/pricing tenant-aware or mark UNKNOWN.
- [x] Use tenant timezone for "today".
- [x] Log or expose partial read warnings instead of fully silent catches.

## 3. Platform admin dashboard

- [x] Replace `PlatformDashboardStatsService` with `QueryBus.ask(GetPlatformDashboardStatsQuery)`.
- [ ] Consider moving platform health behind `platform.ops.api` or equivalent stable API. *(health still via ObjectProvider<PlatformHealthProbe> — no platform.ops.api yet)*
- [x] Add platform public content/internal announcements widget.
- [x] Keep onboarding DRAFT tenants query efficient; add catalog status filter later.

## 4. Cashier POS dashboard

- [x] Wire `GetCashierDashboardStatsQuery` to cashier web assembler. *(added `loadAnalyticsStats()` in CashierDashboardPayloadAssembler)*
- [x] Return offline sync attention placeholder. *(buildOfflineSyncPlaceholder() added)*
- [ ] Return identity/session summary. *(covered by existing identity/session widgets — V2 if enrichment needed)*
- [ ] Return sales by open draw. *(V2 — requires per-draw breakdown in analytics)*
- [ ] Return payout attention. *(V2)*
- [ ] Keep payload compact for POS/mobile. *(POS uses /tenant/cashier/home endpoint, not PageModel — N/A)*

## 5. PageModel dynamic resolver hardening

- [x] Enrich `NO_PROVIDER` errors with logicalId, widgetType and source. *(PageModelDynamicResolver updated)*
- [x] Ensure `doc == null` returns 404 in public runtime if page is missing. *(PublicPageModelService throws TchNotFoundException)*
- [x] Return at least `List.of(currentLang)` if languages are absent. *(PublicPageModelService langs fallback fixed)*
- [x] `JsonNode.deepCopy()` to prevent consumers mutating cached nodes. *(JsonFileProvider.load() now returns deepCopy)*
- [ ] Consider provider support lock by logicalId. *(deferred V2)*
- [ ] Review `JsonFileProvider` cache key for language if localized fragments are introduced. *(deferred V2)*

## 6. V2 backlog

- [ ] User preference to hide public content/news/announcements by surface.
- [ ] Dashboard widget personalization.
- [ ] Advanced charts and drilldowns.
