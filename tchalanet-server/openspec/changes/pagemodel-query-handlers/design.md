# Design — PageModel Dynamic Provider Query Handlers

## Existing mechanism

Keep the existing `PageModelDynamicResolver` and `PageModelDynamicProvider` contract.

Providers are selected by:

```java
supports(String logicalId, String widgetType, String source)
```

The binding source must be normalized in `snake_case`.

Example:

```json
{
  "type": "RecentTicketsWidget",
  "binding": {
    "mode": "dynamic",
    "source": "cashier_recent_tickets"
  },
  "props": {
    "title_key": "dashboard.cashier.recent_tickets.title",
    "limit": 5
  }
}
```

## Provider rule

A provider is a thin adapter between PageModel and QueryBus.

Allowed inside provider:

- Read `widgetConfig.props()`.
- Validate simple provider props such as `limit`, `include_history`, `file_key`.
- Read tenant/user/roles from `TchRequestContext`.
- Call `QueryBus.ask(...)`.

Forbidden inside provider:

- JDBC, JPA, repository access.
- Business decisions.
- Settlement, sale, approval, permission logic.
- Cross-domain SQL.
- Manual tenant filtering beyond passing typed context to queries.

## Query ownership rule

Queries live in the source-of-truth bounded context:

| Widget/provider type                     | Query owner                                                        |
| ---------------------------------------- | ------------------------------------------------------------------ |
| Cashier tickets, sales totals, approvals | `core.sales`                                                       |
| Cashier session                          | `core.possession` or current session owning domain                 |
| Next draws / vendable draws              | `core.draw`                                                        |
| Public results by slot                   | `core.drawresult` with `core.draw` read projection if needed       |
| Public news                              | `features.news` or `catalog.news` depending current implementation |
| Admin KPIs                               | `core.sales` or `features.reporting` later                         |
| Admin draw operations                    | `core.draw`                                                        |
| Admin alerts                             | `features.notification` / `features.ops`                           |
| Superadmin system health                 | `features.ops` later                                               |
| Superadmin tenants                       | `catalog.tenant` or platform admin later                           |

## SQL projection rule

Dashboard widgets should read from optimized projections where possible.

Acceptable MVP:

- JDBC readers using `NamedParameterJdbcTemplate` in the owning bounded context.
- SQL aggregation with small, indexed filters.
- Simple cross-table joins inside the same bounded context.
- Minimal stable read-only cross-domain projection only if already exposed by an API/port.

Avoid:

- SQL in `features.pagemodel`.
- Large JPA object graph loading for dashboards.
- Returning domain aggregates to PageModel widgets.
- Public response leaking internal IDs.

## Response shape rule

Each query returns a view DTO specific to the widget/use case.

Example:

```java
public record CashierRecentTicketView(
    String publicCode,
    String status,
    Instant soldAt,
    long stakeTotalCents,
    long potentialPayoutCents,
    String drawLabel,
    int lineCount
) {}
```

Do not expose:

- `TicketId`
- `DrawId`
- `TenantId`
- internal terminal UUIDs
- address IDs
- full address details on public endpoints

## Error behavior

The existing resolver catches provider exceptions and reports `WidgetDynamicError`.

Provider handlers should prefer safe empty payloads only when absence is expected.

Examples:

- Recent tickets: empty list is valid.
- Session required for cashier: return `active=false` view rather than throwing, unless context is invalid.
- Public results: empty slots list is valid if no results are configured.
- Unknown provider source: resolver returns `NO_PROVIDER`.

## Caching

MVP default: no provider-level cache.

Use domain query/read-side cache later only for:

- public results by slot
- public news
- public plans
- admin KPIs if expensive

Provider must not own cache names. Cache belongs to the source domain.

## Security

- Tenant/private queries must use `TchRequestContext` and typed IDs.
- Client PageModel JSON must never provide tenantId, cashierId, sessionId, or performedBy.
- Public widgets must use a public-safe context and masked DTOs.
- Admin providers must require tenant admin/operator permissions at route/controller level or through existing method security.
- Superadmin providers later must be platform-scoped and SUPER_ADMIN-only.
