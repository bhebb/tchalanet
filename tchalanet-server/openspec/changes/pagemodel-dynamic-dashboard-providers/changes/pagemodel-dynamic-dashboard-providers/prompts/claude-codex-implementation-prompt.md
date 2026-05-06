# Claude/Codex Implementation Prompt — PageModel dynamic providers + JSON fragments

You are working in the Tchalanet server repo.

Goal: implement the OpenSpec change `pagemodel-dynamic-dashboard-providers`.

## Context

Tchalanet already has:

- `features.pagemodel.dynamic.PageModelDynamicProvider`
- `features.pagemodel.dynamic.PageModelDynamicResolver`
- `features.pagemodel.shared.PageDynamicPayload`
- `features.pagemodel.shared.WidgetDynamicError`

Do not replace this mechanism. Extend it.

## Mandatory first task

Decompose large PageModel JSON templates into sub-JSON fragments for repeated shell/navigation pieces:

- header links
- footer links
- sidebar links
- menus
- support links
- legal links
- quick actions
- profile menu links

Create resources under:

```text
src/main/resources/pagemodel/fragments/
```

Start with:

```text
pagemodel/fragments/public/header.links.json
pagemodel/fragments/public/footer.links.json
pagemodel/fragments/private/cashier/sidebar.links.json
pagemodel/fragments/private/cashier/header.links.json
pagemodel/fragments/private/cashier/quick_actions.links.json
```

Use `label_key` in JSON fragments. Do not duplicate translated labels.

## Implement generic JSON provider

Add:

```text
features/pagemodel/dynamic/providers/json/PageModelJsonFragmentRegistry.java
features/pagemodel/dynamic/providers/json/JsonFileProvider.java
```

Rules:

- provider source: `json_file`
- providerKey: `json_file`
- read `props.file_key`
- resolve through whitelist registry
- do not accept raw path
- no filesystem traversal
- load classpath resource
- parse JSON using existing non-deprecated `JsonUtils` from `common`
- do not introduce another ObjectMapper or duplicated JSON utility
- do not use deprecated JSON utility methods

Check the existing `JsonUtils` API before coding and use the current non-deprecated method.

## Normalize dynamic sources

Replace ambiguous sources with snake_case:

```text
news -> public_news
cashier.overview -> cashier_overview
quick_sale -> cashier_quick_sale
recent_tickets -> cashier_recent_tickets
session -> cashier_session
```

Provider `providerKey()` should equal the source.

## Providers to implement or stub cleanly

P0:

- `PublicNewsProvider` source `public_news`
- `PublicDrawResultsProvider` source `public_draw_results`
- `CashierOverviewProvider` source `cashier_overview`
- `CashierQuickSaleProvider` source `cashier_quick_sale`
- `CashierRecentTicketsProvider` source `cashier_recent_tickets`
- `CashierSessionProvider` source `cashier_session`
- `CashierNextDrawsProvider` source `cashier_next_draws`
- `CashierLimitsProvider` source `cashier_limits`

P1:

- `AdminKpisProvider` source `admin_kpis`
- `AdminDrawOperationsProvider` source `admin_draw_operations`
- `AdminApprovalQueueProvider` source `admin_approval_queue`
- `AdminAlertsProvider` source `admin_alerts`
- `SuperAdminSystemHealthProvider` source `superadmin_system_health`
- `SuperAdminBatchStatusProvider` source `superadmin_batch_status`
- `SuperAdminTenantsProvider` source `superadmin_tenants`
- `SuperAdminVersionProvider` source `superadmin_version`

If a query does not exist yet, create a minimal application query contract in the owning domain or leave a clear TODO in the provider with tests skipped/disabled only if project convention allows it. Prefer implementing provider skeletons after confirming existing query names.

## Architecture rules

- `features.pagemodel` is a BFF/composition feature.
- Providers must not access repositories, JPA entities, SQL, or business tables directly.
- Providers call QueryBus or stable application services.
- Business logic stays in core/catalog domains.
- Tenant/context comes from `TchRequestContext`, not from client body.
- Public payloads must not expose internal IDs.

## Resolver hardening

Change:

```java
Map<String, Object> widgets = new HashMap<>();
```

To:

```java
Map<String, Object> widgets = new LinkedHashMap<>();
```

Keep partial errors.

## Test expectations

Add tests for:

- registry resolves known key
- registry rejects unknown key
- `JsonFileProvider` loads a known fragment
- `JsonFileProvider` rejects missing `file_key`
- resolver preserves partial error behavior
- providers support exact snake_case source
- cashier PageModel has no `NO_PROVIDER` for configured dynamic sources, once queries/providers exist

## Files from this OpenSpec to consult

- `proposal.md`
- `design.md`
- `tasks.md`
- `source-registry.md`
- `specs/features-pagemodel-dynamic-providers/spec.md`
- `json/pagemodel-templates/private.dashboard.cashier.updated.json`
- `json/pagemodel-templates/public.home.dynamic-widgets.patch.json`
- `json/pagemodel-templates/public.draw_results.template.json`
