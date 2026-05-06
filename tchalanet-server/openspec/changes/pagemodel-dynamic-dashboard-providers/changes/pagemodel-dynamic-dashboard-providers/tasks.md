# Tasks — PageModel Dynamic Providers + JSON Fragment Provider

## 1. Decompose large PageModel JSON into sub-JSON fragments FIRST

- [x] Inventory current PageModel JSON templates for public home and private dashboards.
- [ ] Identify duplicated shell/navigation blocks:
  - [ ] header links
  - [ ] footer links
  - [ ] sidebar links
  - [ ] menu entries
  - [ ] support links
  - [ ] legal links
  - [ ] quick action links
  - [ ] profile menu links
- [x] Create resource folder:

```text
src/main/resources/pagemodel/fragments/
```

- [ ] Create public fragments:

```text
src/main/resources/pagemodel/fragments/public/header.links.json
src/main/resources/pagemodel/fragments/public/footer.links.json
src/main/resources/pagemodel/fragments/public/main_menu.links.json
src/main/resources/pagemodel/fragments/public/legal.links.json
src/main/resources/pagemodel/fragments/public/support.links.json
```

- [ ] Create private common fragments:

```text
src/main/resources/pagemodel/fragments/private/common/footer.links.json
src/main/resources/pagemodel/fragments/private/common/profile_menu.links.json
```

- [x] Create cashier fragments:

```text
src/main/resources/pagemodel/fragments/private/cashier/sidebar.links.json
src/main/resources/pagemodel/fragments/private/cashier/header.links.json
src/main/resources/pagemodel/fragments/private/cashier/quick_actions.links.json
```

- [ ] Create admin fragments:

```text
src/main/resources/pagemodel/fragments/private/admin/sidebar.links.json
src/main/resources/pagemodel/fragments/private/admin/header.links.json
src/main/resources/pagemodel/fragments/private/admin/management_menu.links.json
```

- [ ] Create superadmin fragments:

```text
src/main/resources/pagemodel/fragments/private/superadmin/sidebar.links.json
src/main/resources/pagemodel/fragments/private/superadmin/header.links.json
src/main/resources/pagemodel/fragments/private/superadmin/platform_menu.links.json
```

- [x] Replace duplicated inline links in PageModel JSON with dynamic `json_file` widgets/sections where the renderer supports it.
- [x] Keep i18n values as `label_key`; do not duplicate translated labels in fragments.

## 2. Normalize PageModel dynamic source names

- [x] Replace ambiguous sources:

```text
news -> public_news
cashier.overview -> cashier_overview
quick_sale -> cashier_quick_sale
recent_tickets -> cashier_recent_tickets
session -> cashier_session
```

- [x] Use only snake_case for `binding.source`.
- [x] Align `providerKey()` with `binding.source`.

## 3. Implement JSON fragment registry

- [x] Add class:

```text
features/pagemodel/dynamic/providers/json/PageModelJsonFragmentRegistry.java
```

- [x] Implement a whitelist map from `file_key` to classpath resource path.
- [x] Reject unknown keys with a safe exception.
- [x] Do not accept raw paths from templates.
- [ ] Add unit tests for:
  - [x] known key resolves
  - [x] unknown key is rejected
  - [x] path traversal strings are rejected because they are not in registry

## 4. Implement generic JsonFileProvider using common JsonUtils

- [x] Add class:

```text
features/pagemodel/dynamic/providers/json/JsonFileProvider.java
```

- [x] `supports(...)` must match `source = json_file`.
- [x] Read `props.file_key`.
- [x] Resolve file through `PageModelJsonFragmentRegistry`.
- [x] Load classpath resource.
- [x] Parse JSON with the existing non-deprecated `JsonUtils` from `common`.
- [x] Do not duplicate ObjectMapper configuration.
- [x] Do not use deprecated JSON utility methods.
- [x] Return parsed JSON as `Object`, `Map<String,Object>`, or JsonNode depending on current PageModel serialization behavior.
- [ ] Add errors:
  - [x] `MISSING_PROP` when `file_key` is missing
  - [x] `JSON_FRAGMENT_NOT_FOUND` when registry/resource fails
  - [x] `JSON_FRAGMENT_INVALID` when JSON parsing fails

## 5. Harden PageModelDynamicResolver

- [x] Change dynamic widgets map from `HashMap` to `LinkedHashMap`.
- [x] Keep partial failure behavior.
- [x] Add providerKey to errors.
- [ ] Consider logging provider errors safely without exposing internals to public responses.

## 6. Implement public dynamic providers

- [x] Update existing `PublicNewsProvider`:
  - [x] source `public_news`
  - [x] providerKey `public_news`
  - [x] keep `max_items` prop
- [x] Add `PublicDrawResultsProvider`:
  - [x] source `public_draw_results`
  - [ ] query `ListPublicDrawResultsBySlotQuery`
  - [ ] props: `include_history`, `history_limit`, `include_next_by_slot`, `include_latest_by_slot`
- [x] Add later/placeholder `PublicTchalaProvider`:
  - [x] source `public_tchala`
  - [ ] query/service: `ListPublicTchalaEntriesQuery` or catalog service
- [x] Add later/placeholder `PublicPlansProvider`:
  - [x] source `public_plans`
  - [ ] query/service: `ListPublicPlansQuery` or catalog plan service

## 7. Implement cashier private dashboard providers

- [x] Add `CashierOverviewProvider`:
  - [x] source `cashier_overview`
  - [ ] query `GetCashierDashboardOverviewQuery`
- [x] Add `CashierQuickSaleProvider`:
  - [x] source `cashier_quick_sale`
  - [ ] query `GetCashierQuickSaleOptionsQuery`
- [x] Add `CashierRecentTicketsProvider`:
  - [x] source `cashier_recent_tickets`
  - [ ] query `ListRecentTicketsQuery`
  - [ ] prop `max_items`
- [x] Add `CashierSessionProvider`:
  - [x] source `cashier_session`
  - [ ] query `GetActiveCashierSessionQuery`
- [x] Add `CashierNextDrawsProvider`:
  - [x] source `cashier_next_draws`
  - [ ] query `ListNextDrawsQuery`
- [x] Add `CashierLimitsProvider`:
  - [x] source `cashier_limits`
  - [ ] query `GetCashierLimitsSnapshotQuery`

## 8. Implement tenant admin dashboard providers

- [x] Add `AdminKpisProvider`:
  - [x] source `admin_kpis`
  - [ ] query `GetTenantDashboardKpisQuery`
- [x] Add `AdminDrawOperationsProvider`:
  - [x] source `admin_draw_operations`
  - [ ] query `ListTenantDrawOperationsSummaryQuery`
- [x] Add `AdminApprovalQueueProvider`:
  - [x] source `admin_approval_queue`
  - [ ] query `ListPendingSaleApprovalsQuery`
- [x] Add `AdminAlertsProvider`:
  - [x] source `admin_alerts`
  - [ ] query `ListTenantAlertsQuery`
- [ ] Add `AdminAgentsProvider`:
  - [ ] source `admin_agents`
  - [ ] query `ListTenantAgentsSummaryQuery`
- [ ] Add `AdminOutletsProvider`:
  - [ ] source `admin_outlets`
  - [ ] query `ListTenantOutletsSummaryQuery`

## 9. Implement superadmin dashboard providers

- [x] Add `SuperAdminSystemHealthProvider`:
  - [x] source `superadmin_system_health`
  - [ ] service/query ops health
- [x] Add `SuperAdminBatchStatusProvider`:
  - [x] source `superadmin_batch_status`
  - [ ] query `ListBatchGateStatusQuery`
- [x] Add `SuperAdminTenantsProvider`:
  - [x] source `superadmin_tenants`
  - [ ] query `ListPlatformTenantsSummaryQuery`
- [x] Add `SuperAdminVersionProvider`:
  - [x] source `superadmin_version`
  - [ ] build/version info service

## 10. Update PageModel templates

- [x] Update `private.dashboard.cashier` JSON:
  - [x] replace `cashier.overview` with `cashier_overview`
  - [x] replace `quick_sale` with `cashier_quick_sale`
  - [x] replace `recent_tickets` with `cashier_recent_tickets`
  - [x] replace `session` with `cashier_session`
  - [x] use `json_file` for sidebar/header/footer links when renderer supports shell dynamic fragments
- [x] Update `public.home` JSON:
  - [x] source `public_news`
  - [x] source `public_draw_results`
  - [x] source `json_file` for public nav/footer/legal/support fragments
- [x] Add/prepare `public.draw_results` JSON:
  - [x] same provider `public_draw_results`
  - [x] `include_history=true`
  - [x] `history_limit=10`

## 11. Tests

- [x] Unit test JsonFileProvider.
- [x] Unit test registry whitelist.
- [x] Unit test resolver stable order with LinkedHashMap.
- [x] Unit test each provider `supports(...)` source matching.
- [x] Integration test private cashier PageModel resolves dynamic widgets without NO_PROVIDER.
- [x] Integration test public home PageModel resolves dynamic widgets without NO_PROVIDER.
- [ ] Public endpoint test does not expose internal IDs.

## 12. Documentation

- [x] Document source registry in `DOMAIN_PAGEMODEL.md` or equivalent.
- [x] Document fragment naming convention.
- [x] Document rule: `binding.source` is snake_case and stable.
- [x] Document rule: fragments use `label_key`, not translated labels.
