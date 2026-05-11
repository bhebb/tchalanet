# Tasks — PageModel Query Handlers

## 0. Inventory and validation

- [ ] List all existing provider stubs under `features.pagemodel.dynamic.providers`.
- [ ] Normalize all provider `source` values to `snake_case`.
- [ ] Update PageModel JSON templates to use normalized sources.
- [ ] Confirm `PageModelDynamicResolver` uses `LinkedHashMap` for stable widget order.
- [ ] Confirm providers do not inject repositories/JPA/JDBC directly.
- [ ] Confirm provider props are read through a shared helper, for example `PageModelProviderProps`.

## 1. Public MVP providers

### 1.1 Public news

- [ ] Provider: `PublicNewsProvider`
- [ ] Source: `public_news`
- [ ] Owner: `features.news.publicnews` or existing news feature
- [ ] Check existing service/query: `PublicNewsService.listAll()` exists.
- [ ] Replace direct `listAll().stream().limit(...)` with a query if the app already has `QueryBus` conventions for news.
- [ ] Suggested query if missing: `ListPublicNewsQuery(lang, maxItems)`.
- [ ] Suggested handler: `ListPublicNewsQueryHandler`.
- [ ] Suggested DTO: `PublicNewsItemView(titleKey/title, excerpt, url, publishedAt, category)`.
- [ ] Keep public-safe response.

### 1.2 Public draw results

- [ ] Provider: `PublicDrawResultsProvider`
- [ ] Source: `public_draw_results`
- [ ] Owner: `core.drawresult` for latest results, with `core.draw` projection for next draw if needed.
- [ ] Create query if missing: `ListPublicDrawResultsBySlotQuery`.
- [ ] Create handler: `ListPublicDrawResultsBySlotQueryHandler`.
- [ ] Create view DTOs:
  - [ ] `PublicDrawResultsBySlotView`
  - [ ] `PublicSlotResultView`
  - [ ] `PublicLatestResultView`
  - [ ] `PublicNextDrawView`
  - [ ] `PublicResultHistoryItemView`
- [ ] Create reader port: `PublicDrawResultReaderPort`.
- [ ] Create JDBC adapter in `core.drawresult.infra.persistence`.
- [ ] SQL must return configured visible slots, latest result, optional history, optional next draw data.
- [ ] Read props:
  - [ ] `include_latest_by_slot`, default `true`
  - [ ] `include_next_by_slot`, default `true`
  - [ ] `include_history`, default `false`
  - [ ] `history_limit`, default `0` for home, max `10`
- [ ] Ensure no internal IDs are returned.
- [ ] Ensure response supports home page and dedicated results page.

### 1.3 Public Tchala

- [ ] Provider: `PublicTchalaProvider`
- [ ] Source: `public_tchala`
- [ ] Owner: `catalog.tchala` or current content/static source.
- [ ] Postpone if no source exists.
- [ ] Suggested query: `ListPublicTchalaEntriesQuery(lang, maxItems)`.
- [ ] Suggested DTO: `PublicTchalaEntryView(term, meaning, numbers, locale)`.

### 1.4 Public plans

- [ ] Provider: `PublicPlansProvider`
- [ ] Source: `public_plans`
- [ ] Owner: `catalog.plan` or static JSON file provider for MVP.
- [ ] Suggested query: `ListPublicPlansQuery(lang)`.
- [ ] Suggested DTO: `PublicPlanView(code, titleKey, priceLabel, features)`.

## 2. Cashier MVP providers

### 2.1 Cashier recent tickets

- [ ] Provider: `CashierRecentTicketsProvider`
- [ ] Source: `cashier_recent_tickets`
- [ ] Owner: `core.sales`
- [ ] Create query if missing: `ListCashierRecentTicketsQuery(TenantId tenantId, UserId cashierId, int limit)`.
- [ ] Create handler: `ListCashierRecentTicketsQueryHandler`.
- [ ] Create view: `CashierRecentTicketView`.
- [ ] Create/extend reader port: `TicketSummaryReaderPort.findRecentByCashier(...)`.
- [ ] Create JDBC adapter in `core.sales.infra.persistence`.
- [ ] SQL: `ticket` + line count + draw label/snapshot where available.
- [ ] Clamp limit to `1..20`, default `5`.
- [ ] Return empty list when no recent tickets.

### 2.2 Cashier session

- [ ] Provider: `CashierSessionProvider`
- [ ] Source: `cashier_session`
- [ ] Owner: current POS/session bounded context. If missing, use `core.sales` application read model for MVP.
- [ ] Create query if missing: `GetCashierSessionSummaryQuery(TenantId tenantId, UserId cashierId)`.
- [ ] Create handler: `GetCashierSessionSummaryQueryHandler`.
- [ ] Create view: `CashierSessionSummaryView(active, sessionCode, openedAt, expectedCashCents, salesTotalCents, ticketCount)`.
- [ ] Create reader port: `CashierSessionReaderPort`.
- [ ] SQL: active session for tenant + cashier, optionally aggregated sales since session open.
- [ ] If no active session, return `active=false` view, not exception.

### 2.3 Cashier overview

- [ ] Provider: `CashierOverviewProvider`
- [ ] Source: `cashier_overview`
- [ ] Owner: `core.sales`
- [ ] Create query if missing: `GetCashierDashboardOverviewQuery(TenantId tenantId, UserId cashierId, LocalDate businessDate)`.
- [ ] Create handler: `GetCashierDashboardOverviewQueryHandler`.
- [ ] Create view: `CashierDashboardOverviewView`.
- [ ] Create reader port: `CashierDashboardReaderPort`.
- [ ] SQL aggregates for current business day:
  - [ ] tickets sold count
  - [ ] sales total cents
  - [ ] potential payout total cents
  - [ ] cancelled/void count
  - [ ] pending approval count if applicable
- [ ] Business date must use tenant timezone resolver or existing time service.

### 2.4 Cashier next draws

- [ ] Provider: `CashierNextDrawsProvider`
- [ ] Source: `cashier_next_draws`
- [ ] Owner: `core.draw`
- [ ] Check if `ListNextDrawsQuery` already exists.
- [ ] Reuse if it returns vendable draw view.
- [ ] Otherwise create `ListCashierNextDrawsQuery(TenantId tenantId, int limit)`.
- [ ] Create handler: `ListCashierNextDrawsQueryHandler`.
- [ ] Create view: `CashierNextDrawView(channelCode, label, scheduledAt, cutoffAt, status, slotKey)`.
- [ ] Create reader port: `DrawSummaryReaderPort.findNextVendableDraws(...)`.
- [ ] SQL: tenant draws with status `OPEN` or next `SCHEDULED`, cutoff in future, ordered by cutoff/scheduled time.

### 2.5 Cashier quick sale

- [ ] Provider: `CashierQuickSaleProvider`
- [ ] Source: `cashier_quick_sale`
- [ ] Owner: `core.sales` application composition.
- [ ] Create query if missing: `GetCashierQuickSaleModelQuery(TenantId tenantId, UserId cashierId, String lang)`.
- [ ] Create handler: `GetCashierQuickSaleModelQueryHandler`.
- [ ] Compose from stable read APIs:
  - [ ] active vendable games/pricing from catalog/core sales prep source
  - [ ] next vendable draws from `core.draw`
  - [ ] cashier autonomy/limits summary if available
- [ ] Create view: `CashierQuickSaleModelView`.
- [ ] Avoid a large cross-domain SQL query in MVP.

## 3. Admin tenant MVP providers

### 3.1 Admin KPIs

- [ ] Provider: `AdminKpisProvider`
- [ ] Source: `admin_kpis`
- [ ] Owner: `core.sales` or `features.reporting` later.
- [ ] MVP owner: `core.sales` read projection.
- [ ] Create query if missing: `GetAdminDashboardKpisQuery(TenantId tenantId, LocalDate businessDate)`.
- [ ] Create handler: `GetAdminDashboardKpisQueryHandler`.
- [ ] Create view: `AdminDashboardKpisView`.
- [ ] Create reader port: `AdminSalesKpiReaderPort`.
- [ ] SQL aggregates by tenant and business date:
  - [ ] gross sales cents
  - [ ] ticket count
  - [ ] cancelled count
  - [ ] pending approvals count
  - [ ] won/unsettled amount if available
- [ ] Keep result small and cache later if expensive.

### 3.2 Admin approval queue

- [ ] Provider: `AdminApprovalQueueProvider`
- [ ] Source: `admin_approval_queue`
- [ ] Owner: `core.sales`
- [ ] Check existing approval query.
- [ ] Create if missing: `ListPendingSalesApprovalsQuery(TenantId tenantId, int limit)`.
- [ ] Create handler: `ListPendingSalesApprovalsQueryHandler`.
- [ ] Create view: `PendingSalesApprovalView`.
- [ ] Create reader port: `SalesApprovalReaderPort`.
- [ ] SQL: tickets or approval requests with status `PENDING_APPROVAL`.
- [ ] Clamp limit to `1..50`, default `10`.

### 3.3 Admin draw operations

- [ ] Provider: `AdminDrawOperationsProvider`
- [ ] Source: `admin_draw_operations`
- [ ] Owner: `core.draw`
- [ ] Create query if missing: `GetAdminDrawOperationsSummaryQuery(TenantId tenantId)`.
- [ ] Create handler: `GetAdminDrawOperationsSummaryQueryHandler`.
- [ ] Create view: `AdminDrawOperationsSummaryView`.
- [ ] Create reader port: `DrawOperationsReaderPort`.
- [ ] SQL summary:
  - [ ] open draws count
  - [ ] closed draws awaiting result count
  - [ ] resulted draws awaiting settlement count
  - [ ] next due draw
  - [ ] last applied result timestamp if available

### 3.4 Admin alerts

- [ ] Provider: `AdminAlertsProvider`
- [ ] Source: `admin_alerts`
- [ ] Owner: `features.notification` or `features.ops` depending existing code.
- [ ] Create query if missing: `ListAdminDashboardAlertsQuery(TenantId tenantId, int limit)`.
- [ ] Create handler: `ListAdminDashboardAlertsQueryHandler`.
- [ ] Create view: `AdminDashboardAlertView(level, code, titleKey, message, createdAt)`.
- [ ] Sources may include:
  - [ ] failed batch notifications
  - [ ] disabled gates
  - [ ] pending approvals over threshold
  - [ ] draw results stuck/provisional
- [ ] MVP may return an empty list if notification source is not ready.

## 4. Superadmin post-MVP providers

Do not implement in MVP unless explicitly requested.

### 4.1 Platform system health

- [ ] Provider: `SuperAdminSystemHealthProvider`
- [ ] Source: `superadmin_system_health`
- [ ] Owner: `features.ops`
- [ ] Query: `GetPlatformSystemHealthQuery`
- [ ] Sources: API health, DB, Redis, Meilisearch, Keycloak, Edge service.
- [ ] Security: SUPER_ADMIN only, platform scope.

### 4.2 Platform batch status

- [ ] Provider: `SuperAdminBatchStatusProvider`
- [ ] Source: `superadmin_batch_status`
- [ ] Owner: `features.ops` + `common.batch`
- [ ] Query: `GetPlatformBatchStatusQuery`
- [ ] Sources: batch gates, last runs, failures, skipped due gate.
- [ ] Security: SUPER_ADMIN only.

### 4.3 Platform tenants

- [ ] Provider: `SuperAdminTenantsProvider`
- [ ] Source: `superadmin_tenants`
- [ ] Owner: `catalog.tenant` / platform tenant admin.
- [ ] Query: `ListPlatformTenantSummariesQuery`
- [ ] Sources: tenants active/suspended/trial, plan, last activity.
- [ ] Security: SUPER_ADMIN only.

### 4.4 Platform version

- [ ] Provider: `SuperAdminVersionProvider`
- [ ] Source: `superadmin_version`
- [ ] Owner: `features.ops`.
- [ ] Query: `GetPlatformVersionSummaryQuery`
- [ ] Sources: build info, `VERSIONS.md`, app version endpoint, edge/web versions later.
- [ ] Security: SUPER_ADMIN only.

## 5. Testing

- [ ] Unit test each provider calls expected query with props defaults.
- [ ] Unit test each query handler clamps limits.
- [ ] JDBC adapter tests with Testcontainers or existing persistence test setup.
- [ ] PageModel dynamic resolver integration test:
  - [ ] page model with multiple dynamic widgets
  - [ ] all providers found
  - [ ] error list empty
  - [ ] widget keys preserved
- [ ] Error path test:
  - [ ] unknown source returns `NO_PROVIDER`
  - [ ] provider exception returns `PROVIDER_ERROR`
- [ ] Security test:
  - [ ] private providers use current tenant/user context
  - [ ] public draw result does not expose internal IDs
  - [ ] superadmin providers are not wired into default dashboards for MVP
