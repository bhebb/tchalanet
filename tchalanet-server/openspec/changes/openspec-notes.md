# Cross-change notes

## Non-negotiable architecture rules

- `features` is a leaf layer. One feature must not consume another feature as a Java dependency.
- Shared reusable capabilities move to `platform.<capability>.api`, `core.<domain>.api`, `catalog.<name>.api`, or `common` only if purely technical.
- `core.analytics` owns persistence, projections, event consumers, recompute and purge for analytics.
- Features consume analytics through `QueryBus` and stable queries.
- Public content is not notification.

## Terminology

- `publiccontent`: editorial/network/public content, planned/published by platform, displayed by surface/audience.
- `notification`: targeted operational/transactional message, often event-triggered, user/tenant/role scoped, with read/delivery status.
- `analytics`: derived read truth, not the source of financial truth; computed from sales, settlement, payout, session and eventually ledger.

## Source-of-truth rule for metrics

- Sales/ticket counts and gross sales come from `core.sales`.
- Winnings/gains calculated come from settlement/resulted ticket facts.
- Paid payouts come from `core.payout` payments, not merely payable tickets.
- Ledger may become the future accounting truth.
- Analytics projections must be recomputable from source data.

## SQL baseline considerations

The existing DB baseline already contains `public.set_updated_at()` triggers for many tables including `stats_daily` and `stats_draw`, plus `public.increment_draw_exposure(...)` as an atomic upsert function.

The `core.analytics` migration must:

- replace `trg_stats_daily__set_updated_at` / `trg_stats_draw__set_updated_at` with `trg_analytics_daily__set_updated_at` / `trg_analytics_draw__set_updated_at` where tables are renamed;
- add equivalent triggers for new analytics tables such as `analytics_session`, `analytics_recompute_run`, `analytics_daily_rollup` if created;
- consider SQL atomic primitive functions inspired by `increment_draw_exposure`, for example `increment_analytics_daily(...)`;
- keep business decisions in Java/projectors, and SQL functions limited to atomic upsert/increment primitives.
