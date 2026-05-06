# Design — Public Draw Results

## Core decision

Public draw result display should be slot-oriented, not tenant-draw-oriented.

```text
result_slot = public/global schedule slot
              e.g. NY_MID, FL_EVE, TX_1800

draw_result = global result for a result_slot and occurred_at

draw        = tenant-scoped business lifecycle/sales draw
```

For public display we need:

```text
result_slot + latest draw_result + next expected result time
```

We do not need:

```text
tenant draw
draw_channel
TchContext tenant
```

## Why one query with includeHistory

The PageModel public home and the details/mobile/terminal screens need the same base shape, but different payload depth.

Home/PageModel:

```text
~10 slots
latest + next/countdown
no history
must be fast
```

Details/mobile/terminal:

```text
~10 slots
latest + next/countdown
recent history 5 or 10 per slot
```

So the common query is:

```java
ListPublicDrawResultSlotsQuery(slotKeys, provider, includeHistory, historyLimit)
```

This avoids duplication while preserving the performance rule:

```text
includeHistory=false MUST NOT query history.
```

## Query semantics

### includeHistory=false

Used by PageModel public home widget.

Returns per slot:

- slot key/provider/label/timezone/draw time
- latest result
- next expected result time/countdown
- `history = []`

Does not execute history lookup.

### includeHistory=true

Used by public results page details, mobile app, terminal display.

Returns per slot:

- slot key/provider/label/timezone/draw time
- latest result
- next expected result time/countdown
- recent history capped by server

History limit rules:

```text
default = 5
max = 10
invalid <= 0 -> default 5 when includeHistory=true
forced 0 when includeHistory=false
```

## Endpoint semantics

PageModel provider uses QueryBus directly and does not need an HTTP endpoint.

Public/mobile/terminal may use HTTP endpoint:

```text
GET /api/v1/public/draw-results/slots?includeHistory=false
GET /api/v1/public/draw-results/slots?includeHistory=true&historyLimit=5
```

Advanced historical search uses a separate flat paginated endpoint:

```text
GET /api/v1/public/draw-results/history?from=2026-05-01&to=2026-05-05&slotKeys=NY_MID&page=0&size=50
```

## Reader implementation

Because there are around 10 slots, keep the reader simple and maintainable.

Recommended implementation:

```text
1. Query active slots.
2. Query latest results for the selected slots.
3. If includeHistory=true, query recent history for selected slots with per-slot limit.
4. Compute next/countdown in Java using Clock and slot timezone/draw_time.
5. Assemble in memory.
```

This is one API/query call from the caller perspective, even if the adapter uses multiple optimized SQL statements internally.

## No pagination for slot list

There are around 10 slots, so no pagination is required for the slot list query in MVP.

Pagination remains required for advanced `/history` search.

## Public response constraints

Public responses should not expose internal UUIDs by default.

Allowed public fields:

```text
slotKey
provider
label
timezone
drawTime
next expected time/countdown
latest result date/status/quality/haiti/source
history recent result date/status/quality/haiti/source
```

Avoid:

```text
resultSlotId
drawResultId
tenantId
drawId
drawChannelId
internal audit/debug payloads
```

## Boundary with core.draw

`core.draw` remains useful for tenant lifecycle and dashboards:

```text
ListNextDrawsQuery
ListDrawsQuery
GetDrawWithResultQuery
ListLatestDrawsWithResultsQuery if tenant-specific view remains needed
```

But public result widgets and public result pages must use `core.drawresult`.
