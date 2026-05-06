# Feature publicdrawresults

Public vertical for global draw result display.

Boundary:

- `features.publicdrawresults` calls `core.drawresult` through `QueryBus`.
- Public readers use global `result_slot + draw_result`.
- Public readers must not require tenant context.
- Public readers must not query `draw` or `draw_channel`.
- Public DTOs must not expose internal UUIDs by default.

Endpoints are mounted under the global `/api/v1` servlet path:

- `GET /public/draw-results/slots`
  - non-paginated slot list for lightweight public/mobile/terminal display
  - returns slot metadata, latest result, next expected time/countdown, `history=[]`
- `GET /public/draw-results/slots/details`
  - non-paginated slot list with recent per-slot history
  - `historyLimit` defaults to `5` and is capped to `10`
- `GET /public/draw-results/history`
  - paginated advanced history search
  - supports date range and optional slot/provider filters

PageModel integration:

- `PublicDrawResultsProvider` uses source `public_draw_results`.
- It calls `QueryBus.send(new ListPublicDrawResultSlotsQuery(..., false, 0))`.
- It does not call HTTP endpoints.
- It does not call tenant-scoped `core.draw` queries.
