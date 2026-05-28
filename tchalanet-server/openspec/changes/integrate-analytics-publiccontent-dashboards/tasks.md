# Tasks

## 1. Public home

- [ ] Update `PublicHomePayloadAssembler` to use `PublicContentApi`.
- [ ] Keep `home.news` payload shape stable.
- [ ] Continue using `json_file` fragments for hero/features/tchala.
- [ ] Add tests for internal + external public news aggregation.

## 2. Tenant admin dashboard

- [ ] Replace `TenantDashboardStatsService` injection with `QueryBus.ask(GetTenantDashboardStatsQuery)`.
- [ ] Add optional comparison mode: today/yesterday and 7 days/previous 7 days.
- [ ] Add public content/internal announcements widget using `PublicContentApi`.
- [ ] Keep readiness but make games/pricing tenant-aware or mark UNKNOWN.
- [ ] Use tenant timezone for “today”.
- [ ] Log or expose partial read warnings instead of fully silent catches.

## 3. Platform admin dashboard

- [ ] Replace `PlatformDashboardStatsService` with `QueryBus.ask(GetPlatformDashboardStatsQuery)`.
- [ ] Consider moving platform health behind `platform.ops.api` or equivalent stable API.
- [ ] Add platform public content/internal announcements widget.
- [ ] Keep onboarding DRAFT tenants query efficient; add catalog status filter later.

## 4. Cashier POS dashboard

- [ ] Define `GetCashierDashboardStatsQuery` or equivalent.
- [ ] Return identity/session summary.
- [ ] Return sales by open draw.
- [ ] Return payout attention.
- [ ] Return offline sync attention placeholder if needed.
- [ ] Keep payload compact for POS/mobile.

## 5. PageModel dynamic resolver hardening

- [ ] Enrich `NO_PROVIDER` errors with logicalId, widgetId, widgetType and source.
- [ ] Consider provider support lock by logicalId.
- [ ] Ensure `doc == null` returns 404 in public runtime if page is missing.
- [ ] Return at least `List.of(currentLang)` if languages are absent.
- [ ] Review `JsonFileProvider` cache key for language if localized fragments are introduced.
- [ ] Consider `JsonNode.deepCopy()` if consumers may mutate cached nodes.

## 6. V2 backlog

- [ ] User preference to hide public content/news/announcements by surface.
- [ ] Dashboard widget personalization.
- [ ] Advanced charts and drilldowns.
