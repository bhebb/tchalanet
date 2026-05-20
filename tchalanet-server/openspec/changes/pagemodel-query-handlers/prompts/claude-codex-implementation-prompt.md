# Claude/Codex Prompt — Implement PageModel Provider Query Handlers

You are working in the Tchalanet backend.

Goal: implement query handlers behind existing `features.pagemodel.dynamic.providers` stubs.

Do not create a new PageModel provider framework. The existing resolver is the source:

- `PageModelDynamicResolver`
- `PageModelDynamicProvider`
- `PageDynamicPayload`
- `WidgetDynamicError`

Follow this workflow for each provider:

1. Identify `source`.
2. Normalize source to `snake_case` if needed.
3. Identify owner bounded context.
4. Search for an existing query/read model.
5. If a query exists, wire provider to `QueryBus.ask(...)`.
6. If missing, create:
   - query record
   - query handler
   - response view DTO
   - reader port
   - JDBC reader adapter with optimized SQL projection
   - tests
7. Do not place SQL/JPA/repository code in `features.pagemodel`.
8. Do not return internal IDs in public widget payloads.
9. Use `TchRequestContext` for tenant/user context.
10. Clamp all provider limits.

MVP order:

1. `cashier_recent_tickets`
2. `cashier_session`
3. `cashier_overview`
4. `cashier_next_draws`
5. `cashier_quick_sale`
6. `public_draw_results`
7. `public_news`
8. `admin_approval_queue`
9. `admin_kpis`
10. `admin_draw_operations`
11. `admin_alerts`

Post-MVP only:

- `superadmin_system_health`
- `superadmin_batch_status`
- `superadmin_tenants`
- `superadmin_version`

Important architecture rules:

- `features.pagemodel` is BFF composition only.
- Queries live in owning domains such as `core.sales`, `core.draw`, `core.drawresult`, `features.ops`.
- Controllers/providers must remain thin.
- Query handlers are side-effect free.
- Reader adapters apply optimized SQL.
- Use typed IDs outside persistence.
- Tenant/user IDs come from `TchRequestContext`, not JSON props.

Start with `CashierRecentTicketsProvider` because it validates the full pipeline end-to-end.
