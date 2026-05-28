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

- [ ] Define `GetCashierDashboardStatsQuery` or equivalent. *(query exists in core.analytics.api but not wired to cashier assembler — cashier web uses GetCashierDashboardOverviewQuery from core.sales)*
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
