# Design — Analytics Reconciliation and Replay

## Decision

The operational word "replay" means **rebuild projections from transactional source snapshots**.
It does not mean republishing old domain events.

The application does not currently provide an event store containing the original
`TicketPlacedEvent`, payout, cancellation, and correction payloads. Deleting rows from
`processed_event` and publishing synthetic events would also be unsafe: it could duplicate
financial deltas and would not reproduce the original payload exactly.

The only supported repair operation will therefore be:

```text
VALIDATE
  source snapshots -> expected aggregates -> compare with projections

REBUILD_AND_VALIDATE
  source snapshots -> replace selected projections -> validate exact equality -> READY
```

`REBUILD_AND_VALIDATE` is an explicit platform-ops operation. It is never exposed as a tenant
admin or cashier action.

## Scope

The first implementation is tenant-scoped and accepts the existing command contract:

```text
tenantId, from, to, mode, repairReason
```

The maximum window remains 90 business days. A tenant repair changes only tenant-owned data:

- `analytics_daily` rows with `dimension_type = TENANT`;
- `analytics_daily` rows with `dimension_type = SELLER_TERMINAL` for that tenant;
- `analytics_draw` rows belonging to the tenant;
- `analytics_seller_terminal_draw` rows belonging to the tenant;
- `analytics_selection` rows belonging to the tenant.

`PLATFORM` daily rows are not silently changed by a tenant repair. Platform-wide repair requires a
separate all-tenant scope because a single tenant cannot prove the correctness of a global total.
Until that scope exists, a tenant repair must not mark platform metrics ready.

## Source snapshot contract

The current sale snapshot is a useful starting point but is not sufficient for a complete rebuild.
Before implementing the handler, the sales public API must expose a reconciliation snapshot that
contains immutable values captured by the source lifecycle:

- ticket identity, tenant, seller terminal, draw, draw channel, and sale channel;
- sale status and sale timestamp;
- stake, total, seller commission amount, and all charge snapshots;
- line identity and line snapshots: game, bet type, option, selection, stake, origin, and pricing
  source;
- cancellation and void timestamps, including reversal amounts where applicable;
- result and settlement timestamps and statuses;
- effective payout amount;
- payout adjustment and reversal history, with event timestamp, amount, and stable event identity;
- a source watermark that identifies the consistent read boundary.

`winningAmount` plus `paidAt` is not an adequate payout history. If the source cannot provide the
adjustment or reversal history, the operation must return `SOURCE_UNAVAILABLE` and leave existing
projections unchanged. It must never infer a historical paid amount from current configuration.

The analytics application consumes this contract through the sales public API and `QueryBus`. It
does not access sales JPA entities or tables directly.

## Metric date semantics

The rebuild must make the accounting date explicit instead of applying one date to every metric:

| Projection/data | Business date | Source fact |
| --- | --- | --- |
| Daily sale count, stake, gross, charges, commission, selections | sales date | approved sale timestamp |
| Daily cancellation/reversal deltas | lifecycle date | cancellation or reversal timestamp |
| Daily winnings and paid amount | settlement date | payout/adjustment/reversal timestamp |
| Draw aggregate | draw occurrence | draw scheduled business date; lifecycle values update the same draw |
| Seller-terminal/draw aggregate | draw occurrence | same as draw aggregate, scoped by terminal |

The existing draw tables are one row per draw occurrence, not a time series. Their `ref_date` is
the draw business date; later settlement changes update that row. `analytics_daily` remains the
time-series projection and uses the business date of the fact being applied.

The expected-value builder must use the same semantics as the event projectors. Any deliberate
change to those semantics requires a separate OpenSpec or an explicit amendment to this one.

## Rebuild transaction

The handler follows these phases:

1. **Authorize and validate input**: require platform-ops permission, a non-empty repair reason,
   a bounded tenant/date scope, and an idempotency key for repair requests.
2. **Preflight**: read source snapshots and current projections in read-only mode, set tenant
   context explicitly for every read, generate a run id, and write an audit event with `STARTED`
   status.
3. **Acquire repair lock**: take a PostgreSQL advisory lock keyed by tenant and date window. The
   analytics event listeners use the same lock for projection writes, so after-commit events wait
   instead of racing with the replacement.
4. **Mark unavailable**: the trust state for the selected tenant scopes becomes unavailable for
   the duration of the repair. Readers must not expose old values as trustworthy while replacement
   is in progress.
5. **Replace in one transaction**: delete only the selected tenant/date rows and insert the
   source-derived rows. This transaction must not touch `processed_event` and must not publish
   domain events.
6. **Validate before commit**: compare the rebuilt rows with the expected aggregates, including
   missing and orphaned rows. Commit only when the requested repair has a complete source snapshot
   and the comparison is exact.
7. **Finalize audit and trust**: after commit, write the completed audit event with before/after
   mismatches, row counts, source watermark, actor, and reason. Mark the selected scopes `READY`
   only for an exact match. On any failure, keep the old projection transaction rolled back and
   write a failed audit event while marking the scope `RECONCILIATION_REQUIRED`.
8. **Evict caches after commit**: invalidate only the tenant/date analytics caches after a
   successful replacement. Cache eviction is not part of the financial transaction.

### Audit persistence

V1 uses the existing `platform.audit.api.AuditApi` and the existing `audit_event`/`audit_log`
storage. Each run uses one generated run id and emits at least `STARTED`, `SUCCESS`,
`SOURCE_UNAVAILABLE`, or `FAILED` details containing the operation, tenant, date window, mode,
reason, actor, source watermark, row counts, and mismatch summary. The audit writes use a separate
transaction from the projection replacement, so the failure record remains durable when the
replacement rolls back.

No `analytics_recompute_run` table is introduced in this change. The current
`DOMAIN_ANALYTICS.md` reference to that table is aspirational and must be corrected when the
implementation is applied. A queryable reconciliation-run API is a separate follow-up; the
platform-ops response and audit listing are the V1 operational read path.

## API and authorization

Expose one platform-ops endpoint backed by `CommandBus`, for example:

```text
POST /api/v1/platform/ops/analytics/reconciliation
```

Request fields:

- `tenantId`, `from`, `to`;
- `mode`: `VALIDATE` or `REBUILD_AND_VALIDATE`;
- `repairReason` for rebuilds;
- request idempotency key for mutating runs.

Response fields:

- run id and terminal status;
- selected scope and source watermark;
- mismatch count and exact mismatch details;
- rebuilt row counts;
- stable failure/degradation code when the source or validation is unavailable.

The actor is taken from the authenticated platform context. It is never accepted as a client
supplied field. A `VALIDATE` request is read-only and may return `MISMATCH` without changing rows.

## Failure and concurrency rules

- A missing source snapshot, missing tenant context, projection write error, or failed exact
  comparison leaves the replacement uncommitted.
- A repair never clears `processed_event`, never reuses an old event id, and never emits a new
  business event.
- A live after-commit projection waits on the repair lock. It is processed after the repair and
  remains protected by its normal idempotency marker transaction.
- A second repair with the same idempotency key returns the original run result.
- A repair for a date with no source activity creates explicit zero rows only where the projection
  contract requires coverage. Missing draw-specific activity remains missing, not an invented draw.
- Cache eviction occurs only after a successful commit; a failed repair leaves the previous cache
  invalidation policy to the trust-state response.

## Operational runbook for the current STG mismatch

The runbook will be executable only after the handler and platform-ops endpoint are implemented:

1. Run `VALIDATE` for the affected tenant and sale business date.
2. Confirm the mismatch identifies the missing tenant/draw/seller-terminal projection and record
   the returned run id.
3. Run `REBUILD_AND_VALIDATE` for the same tenant/date with an incident or deployment reason.
4. Require `SUCCESS` and zero mismatches before considering the dashboard repaired.
5. Refresh the dashboard and reporting page, then verify tenant, draw, seller-terminal, and
   selection totals against the source ticket.
6. If the result is `SOURCE_UNAVAILABLE` or `RECONCILIATION_REQUIRED`, do not manually edit the
   analytics tables; fix the source snapshot contract or projection failure and rerun validation.

This runbook is deliberately not executable against the current branch because the rebuild handler
and endpoint do not exist yet.

## Operational tenant dashboard

The tenant-admin dashboard is an operational profitability surface. It is not a raw ticket counter
and it must remain useful when a tenant has many terminals and draw channels.

### Primary KPIs

The primary KPI row contains exactly four cards:

1. **Gross sales today** — approved stake total for the tenant business day.
2. **Seller commission payable** — the sum of the seller commission amounts snapshot at sale time.
3. **Estimated net revenue** — gross sales minus calculated winnings, seller commissions, and tenant
   charges. The label must say `estimated` when results or settlements are incomplete.
4. **Available POS** — seller terminals with operational status `ACTIVE`, excluding blocked,
   disabled, and deleted terminals. This is a configuration/availability metric and does not depend
   on whether the terminal sold today.

Ticket count and open-draw count remain available in reports and drilldowns but are not primary
dashboard KPIs. Commission-rate configuration remains on the commission settings page; the
dashboard shows the financial commission amount only.

The dashboard must not present `estimated net revenue` as final cash revenue. When the requested
period contains unsettled or unresulted draws, the response exposes the appropriate trust or
degradation state and the UI labels the value accordingly. A future cash view may expose
`net revenue on paid basis` as a separate metric; it must not silently reuse the estimated label.

### Terminal performance without a Cartesian table

The performance section uses progressive disclosure:

- **Default view: by terminal.** A server-paginated table shows 10–15 terminals per page with
  terminal, gross sales, commission payable, net revenue, and number of channels with activity.
- **Terminal detail: by channel.** Selecting one terminal loads its channel breakdown from
  `analytics_seller_terminal_draw`: channel, gross sales, commission, and net revenue.
- **Secondary view: by channel.** A segmented view lists channels, normally no more than the
  configured channel count, with gross sales, commission, net revenue, and active terminal count.
- **Full report.** A dedicated report route provides filters, server pagination, export, and an
  optional POS × channel matrix. The matrix is never rendered by default on the dashboard.

The total for the filtered scope is returned separately from the current page. The UI must not
sum the visible page and call it the tenant total. The default ordering is descending net revenue,
with an explicit filter for `all`, `with sales`, or `without sales`.

The backend must aggregate in `core.analytics` and return only the requested grouping. It must not
load all 50 terminals × 10 channels into the browser. Financial rows are available only when the
requested analytics scope is trustworthy.

### UI states

Every KPI group and the performance section has an independent state. A failure in performance
must not blank the header or the other KPI cards.

| State | Contract | UI behavior |
| --- | --- | --- |
| `loading` | Request is pending | Show stable skeletons; never show zero as a placeholder. |
| `ready` | Data is complete and trusted | Show values, totals, pagination, and the last refresh timestamp. |
| `empty` | Request succeeded with no matching rows | Show a localized explanation and the relevant action: create/activate a POS, configure a channel, or broaden the date/filter. |
| `partial` | Summary succeeded but a secondary grouping/detail failed | Keep the trusted summary, show a warning on the failed section, and provide retry for that section. |
| `unavailable` | Trust state is not `READY` or source reconciliation is pending | Hide financial values and totals; show the stable reconciliation notice and retry/refresh action. |
| `error` | Request failed without usable data | Show the shared section error with retry. Do not fabricate empty rows or zero totals. |

An empty performance result is not an error. A missing analytics projection for a scope that should
contain activity is `unavailable`, not `empty`. A successful zero-sales day is `ready` only when
the trust contract confirms that the day was evaluated and no source activity existed.

### Acceptance scenarios

- **GIVEN** 50 active POS and 10 draw channels, **WHEN** the dashboard loads, **THEN** the browser
  receives one paginated terminal page and no 500-row Cartesian dataset.
- **GIVEN** one terminal is selected, **WHEN** its performance detail loads, **THEN** only that
  terminal's channel aggregates are requested and displayed.
- **GIVEN** no active POS exists, **WHEN** the dashboard loads, **THEN** `Available POS` is `0`
  with an operational empty message, while financial KPIs remain independently stateful.
- **GIVEN** a trusted day has no approved sales, **WHEN** performance loads, **THEN** the UI shows
  a ready zero-activity state rather than an error.
- **GIVEN** analytics trust is unavailable, **WHEN** the dashboard loads, **THEN** gross sales,
  commission, and net revenue are not rendered as zero.
- **GIVEN** the performance endpoint fails, **WHEN** the dashboard loads, **THEN** the KPI row and
  other healthy sections remain visible and the performance section offers retry.

## Period comparison and PageModel migration

### Periods

The dashboard defaults to `TODAY` and exposes a compact period selector with these values:

| Period | Main window | Comparison window |
| --- | --- | --- |
| `TODAY` | tenant-local today | tenant-local yesterday |
| `YESTERDAY` | tenant-local yesterday | the preceding tenant-local calendar day |
| `THIS_WEEK` | current tenant-local week | previous week |
| `LAST_WEEK` | previous tenant-local week | the week before it |

The selector is represented in the dashboard URL as `?period=...` so it is refreshable and
shareable. The server validates the enum, resolves dates in the tenant timezone, and passes the
period into the PageModel dynamic resolution context. The browser must not calculate business dates
with its own timezone.

The KPI payload contains both windows and an explicit delta:

```json
{
  "period": "TODAY",
  "comparisonPeriod": "YESTERDAY",
  "periodLabelKey": "dashboard.period.today",
  "comparisonLabelKey": "dashboard.period.yesterday",
  "kpis": {
    "grossSales": { "value": 149.00, "delta": 20.00, "deltaPercent": 15.5 },
    "sellerCommissionPayable": { "value": 14.90, "delta": 2.00, "deltaPercent": 15.5 },
    "estimatedNetRevenue": { "value": 84.10, "delta": -4.00, "deltaPercent": -4.5 },
    "availablePos": { "value": 1, "delta": 0, "deltaPercent": 0.0 }
  }
}
```

Financial values carry the analytics trust/degradation state. `availablePos` comes from the
seller-terminal status model and does not become unavailable just because analytics is delayed.

### Current PageModel versus target PageModel

Current backend template:

```text
private.dashboard.tenant_admin (schemaVersion 2)
  ├─ dashboard.tenantAdmin.kpis          KpiGridWidget
  │    salesToday, ticketCountToday, activeSellerTerminals, openDraws
  ├─ dashboard.tenantAdmin.quickActions  QuickActionsWidget
  ├─ dashboard.tenantAdmin.salesTrend    TrendChartWidget
  ├─ dashboard.tenantAdmin.gameBreakdown BreakdownListWidget
  └─ dashboard.tenantAdmin.commission    CommissionSummaryWidget
       commission configuration, not operational profitability
```

Target backend template:

```text
private.dashboard.tenant_admin (schemaVersion 3)
  ├─ dashboard.tenantAdmin.periodSelector      PeriodSelectorWidget
  ├─ dashboard.tenantAdmin.kpis                OperationalKpiGridWidget
  │    grossSales, sellerCommissionPayable, estimatedNetRevenue, availablePos
  ├─ dashboard.tenantAdmin.terminalPerformance TerminalPerformanceWidget
  │    paginated terminal aggregates; channel detail on selection
  ├─ dashboard.tenantAdmin.salesTrend          TrendChartWidget
  │    tenant-local daily gross/net trend for the selected context
  └─ dashboard.tenantAdmin.quickActions        QuickActionsWidget (static JSON props)
```

The sales trend remains before quick actions because it is an operational insight, not merely a
navigation aid. It answers whether the tenant's activity is accelerating, slowing down, or
concentrated on a particular day within the selected period. It must use the same period and
comparison context as the KPI row, expose an explicit empty state for a trusted zero-activity
period, and expose a partial/unavailable state when analytics coverage is incomplete. Quick actions
remain last because they are secondary commands and do not describe the tenant's current health.

The `gameBreakdown` and commission-configuration widgets are removed from this dashboard JSON.
Their Angular components may remain registered because other PageModels can still use them. The
dashboard provider payload no longer exposes `ticketCountToday`, `openDraws`, or commission-rate
configuration fields.

### Provider and source plan

There remains **one grouped provider**, not one provider per widget:

| PageModel slice | Provider | Source of data | Change |
| --- | --- | --- | --- |
| `periodSelector` | `TenantAdminDashboardProvider` | URL `period` + tenant timezone | new slice; no financial read |
| `kpis` | `TenantAdminDashboardProvider` | new `core.analytics` operational KPI query + seller-terminal status query | replace current ticket/open-draw KPI payload |
| `terminalPerformance` | `TenantAdminDashboardProvider` | new `core.analytics` grouped terminal query over `analytics_seller_terminal_draw` | new paginated slice |
| `salesTrend` | `TenantAdminDashboardProvider` | `core.analytics` daily projection query | keep, align with selected period and trust state |
| `quickActions` | static PageModel JSON | no provider read | remove dynamic binding and provider payload entry |

The Java class `TenantAdminDashboardProvider` is therefore refactored, not deleted. The existing
`tenant_admin_dashboard` provider source remains stable to avoid breaking PageModel security and
runtime tests. Its payload/schema is versioned instead of introducing multiple providers that
would repeat the same analytics reads.

New public core contracts required by implementation:

- `GetTenantOperationalDashboardQuery` → period KPIs, comparison deltas, trend, and trust states;
- `GetTenantTerminalPerformanceQuery` → server-paginated terminal grouping with filtered totals;
- an existing or new seller-terminal query returning configured `ACTIVE` POS count, independent
  of analytics projections.

The performance query must support `groupBy=TERMINAL` by default and `terminalId` for channel
drilldown. A future `groupBy=CHANNEL` view can reuse the same contract; it is not a second grouped
provider.

### JSON migration rules

The backend template and the web fallback are updated together:

- increment `schemaVersion` from `2` to `3`;
- remove `dashboard.tenantAdmin.gameBreakdown` and
  `dashboard.tenantAdmin.commission` from layout and widgets;
- add `dashboard.tenantAdmin.periodSelector` and
  `dashboard.tenantAdmin.terminalPerformance`;
- change the KPI item ids and labels to the four operational metrics;
- keep quick actions in both templates, but as static props with no dynamic source;
- fallback JSON contains only the runtime warning and safe actions, never fake financial KPI zeros
  or a fake performance table;
- preserve the source name `tenant_admin_dashboard` and the functional feedback targets
  `tenant_admin_dashboard.kpis`, `tenant_admin_dashboard.performance`, and
  `tenant_admin_dashboard.analytics`.

The template change is not complete until the persisted PageModel, fallback JSON, provider switch,
web widget registry, i18n keys, and PageModel contract tests agree on the same widget ids.

Target template skeleton (illustrative; exact widget props follow the existing PageModel schema):

```json
{
  "schemaVersion": 3,
  "content": {
    "layout": {
      "rows": [
        { "id": "period", "columns": [{ "span": 12,
          "widgets": ["dashboard.tenantAdmin.periodSelector"] }] },
        { "id": "kpis", "columns": [{ "span": 12,
          "widgets": ["dashboard.tenantAdmin.kpis"] }] },
        { "id": "performance", "columns": [{ "span": 12,
          "widgets": ["dashboard.tenantAdmin.terminalPerformance"] }] },
        { "id": "salesTrend", "columns": [{ "span": 12,
          "widgets": ["dashboard.tenantAdmin.salesTrend"] }] },
        { "id": "quickActions", "columns": [{ "span": 12,
          "widgets": ["dashboard.tenantAdmin.quickActions"] }] }
      ]
    },
    "widgets": {
      "dashboard.tenantAdmin.periodSelector": {
        "type": "PeriodSelectorWidget",
        "props": { "options": ["TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK"] }
      },
      "dashboard.tenantAdmin.kpis": {
        "type": "OperationalKpiGridWidget",
        "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
      },
      "dashboard.tenantAdmin.terminalPerformance": {
        "type": "TerminalPerformanceWidget",
        "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
      },
      "dashboard.tenantAdmin.salesTrend": {
        "type": "TrendChartWidget",
        "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
      },
      "dashboard.tenantAdmin.quickActions": {
        "type": "QuickActionsWidget",
        "props": { "actions": "existing-admin-actions" }
      }
    }
  }
}
```

There is no PageModel provider to delete and no second dashboard provider to add. The migration
removes the `gameBreakdown` and commission-configuration slices from this dashboard only. The
commission configuration service and its widget remain available to the dedicated commission
settings page. The new work is in the grouped provider's contracts and in the web widgets needed
for period selection and paginated performance.

## Test design

The implementation must include:

- unit tests for source-to-aggregate semantics, date boundaries, charges, commission snapshots,
  cancellation, payout adjustment, and reversal;
- integration tests with RLS tenant context for `VALIDATE`, missing rows, orphan rows, and exact
  rebuild success;
- rollback tests proving a failed rebuild leaves projections unchanged and marks trust unavailable;
- concurrency tests proving an after-commit projection waits for the repair lock;
- idempotency tests for repeated repair requests;
- endpoint authorization and audit tests;
- an end-to-end scenario: approved sale, intentionally removed projection, `VALIDATE`,
  `REBUILD_AND_VALIDATE`, then dashboard/report reads showing the corrected values.
