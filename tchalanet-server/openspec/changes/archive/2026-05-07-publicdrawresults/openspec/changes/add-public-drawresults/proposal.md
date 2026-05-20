# OpenSpec Change — add-public-drawresults

## Why

The public home, public results page, mobile app, and POS/terminal need to display lottery results without depending on tenant-scoped `core.draw` queries.

Current PageModel/public draws experiments can accidentally require `TchContext.requireTenantId()` because they use tenant draw/read models. For public result display, this is the wrong boundary: the public UI needs global `result_slot + draw_result` data, not tenant `draw + draw_channel` lifecycle data.

## What

Introduce/standardize a public vertical feature:

```text
features.publicdrawresults
```

It exposes public result data backed by global `core.drawresult` queries.

Main decision:

```text
Public draw results are represented by result slots.
Each result slot returns:
- slot metadata
- latest result
- next expected result time + countdown
- optional recent history controlled by includeHistory/historyLimit
```

A single internal query supports both lightweight PageModel usage and richer details usage:

```java
ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider,
    boolean includeHistory,
    int historyLimit
)
```

Rules:

- `includeHistory=false`: return slot metadata + latest + next/countdown only; do not execute history lookup; return `history = List.of()`.
- `includeHistory=true`: return slot metadata + latest + next/countdown + recent history; default history limit 5; max 10.
- PageModel `PublicDrawResultsProvider` must call with `includeHistory=false`.
- Public results detail page, mobile app, and terminal clients may call with `includeHistory=true`.
- Advanced history search remains a separate paginated query/endpoint.

## Impact

### New / changed modules

```text
core.drawresult.application.query.model.ListPublicDrawResultSlotsQuery
core.drawresult.application.query.model.SearchPublicDrawResultsQuery
core.drawresult.application.query.projection.PublicDrawResultSlotView
core.drawresult.application.query.projection.PublicDrawResultView
core.drawresult.application.query.projection.PublicNextResultTimeView
core.drawresult.application.port.out.PublicDrawResultSlotReaderPort
core.drawresult.application.query.handler.ListPublicDrawResultSlotsQueryHandler
features.publicdrawresults
features.pagemodel.dynamic.providers.PublicDrawResultsProvider
```

### Boundary

```text
features.publicdrawresults -> QueryBus -> core.drawresult
```

Must not depend on:

```text
core.draw persistence
core.draw_channel persistence
TchContext.requireTenantId()
tenant draw lifecycle
```

### Existing `core.draw` queries

Keep `core.draw` queries for tenant dashboards/admin/vendor/POS where tenant draw lifecycle matters. Do not use `core.draw` for public global result widgets.

## Non-goals

- Do not remove existing tenant draw queries yet.
- Do not redesign result ingestion/fetch/apply.
- Do not make public result display tenant-scoped in this change.
- Do not expose internal UUIDs in public responses unless explicitly approved later.
