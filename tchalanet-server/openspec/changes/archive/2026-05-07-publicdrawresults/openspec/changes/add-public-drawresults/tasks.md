# Tasks — add-public-drawresults

## 1. Core drawresult public projections

- [x] Add `PublicNextResultTimeView`.
- [x] Add `PublicDrawResultView`.
- [x] Add `PublicDrawResultSlotView` with `history` always non-null.
- [x] Avoid exposing internal IDs in public-facing response DTOs.
- [x] Decide if internal projection may keep typed IDs internally while mapper hides them.

Suggested records:

```java
public record PublicNextResultTimeView(
    Instant expectedAt,
    LocalDate localDate,
    LocalTime localTime,
    String timezone,
    long countdownSeconds,
    String status
) {}
```

```java
public record PublicDrawResultView(
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    Map<String, Object> haiti,
    Map<String, Object> source
) {}
```

```java
public record PublicDrawResultSlotView(
    String slotKey,
    String provider,
    String label,
    String timezone,
    LocalTime drawTime,
    boolean active,
    PublicNextResultTimeView next,
    PublicDrawResultView latest,
    List<PublicDrawResultView> history
) {}
```

## 2. Core drawresult query model

- [x] Add `ListPublicDrawResultSlotsQuery`.
- [x] Add validation/normalization for `slotKeys`, `provider`, `includeHistory`, `historyLimit`.
- [x] History limit default: `5` when `includeHistory=true` and requested value invalid/missing.
- [x] History limit max: `10`.
- [x] When `includeHistory=false`, force `historyLimit=0`.

Suggested query:

```java
public record ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider,
    boolean includeHistory,
    int historyLimit
) implements Query<List<PublicDrawResultSlotView>> {}
```

## 3. Reader port and adapter

- [x] Add `PublicDrawResultSlotReaderPort`.
- [x] Implement adapter under `core.drawresult.infra.persistence`.
- [x] Reader must not call `TchContext.requireTenantId()`.
- [x] Reader must not join tenant `draw` or `draw_channel`.
- [x] Reader starts from `result_slot` and enriches with `draw_result`.
- [x] Reader executes no history lookup when `includeHistory=false`.
- [x] Reader returns active slots by default.
- [x] For ~10 slots, no pagination required on slot endpoint/query.

Suggested backend strategy:

```text
listSlots(includeHistory=false):
  1. query active result_slot rows
  2. query latest draw_result per slot
  3. compute next/countdown with Clock
  4. assemble history = List.of()

listSlots(includeHistory=true):
  1. query active result_slot rows
  2. query latest draw_result per slot
  3. query recent history limited N per slot
  4. compute next/countdown with Clock
  5. assemble
```

## 4. Next/countdown calculation

- [x] Add `ResultSlotScheduleCalculator` or equivalent application service.
- [x] Compute from `result_slot.draw_time + result_slot.timezone`.
- [x] Inject `Clock`.
- [x] Return `expectedAt`, local date/time, timezone, countdown seconds.
- [x] MVP statuses: `WAITING`, `LATE`, `DISABLED`.

MVP formula:

```text
if slot inactive -> DISABLED
else if today's draw time in slot timezone is after now -> today at draw time
else -> tomorrow at draw time
countdownSeconds = max(0, Duration.between(now, expectedAt).seconds)
```

## 5. Public feature

- [x] Create/rename feature package `features.publicdrawresults`.
- [x] Add public DTOs and mapper.
- [x] Add controller endpoint for slots if needed by public page/mobile/terminal.
- [x] Keep PageModel provider internal call through QueryBus, not via HTTP endpoint.
- [x] Add advanced history endpoint/query separately.

Suggested endpoints:

```text
GET /api/v1/public/draw-results/slots?includeHistory=false
GET /api/v1/public/draw-results/slots?includeHistory=true&historyLimit=5
GET /api/v1/public/draw-results/history?from=YYYY-MM-DD&to=YYYY-MM-DD&slotKeys=NY_MID&page=0&size=50
```

## 6. PageModel provider

- [x] Rename/replace old draws provider usage for public result widget.
- [x] Use source `public_draw_results`.
- [x] Call `ListPublicDrawResultSlotsQuery(..., includeHistory=false, historyLimit=0)`.
- [x] Do not call tenant-scoped `ListLatestDrawsWithResultsQuery` from PageModel public home.
- [x] Do not require default tenant for public results widget.

Example:

```java
queryBus.send(new ListPublicDrawResultSlotsQuery(slotKeys, provider, false, 0));
```

## 7. Advanced history search

- [x] Add `SearchPublicDrawResultsQuery`.
- [x] Back it with paginated DB query.
- [x] Apply max date range, e.g. 90 days.
- [x] Apply max size, e.g. 100.
- [x] Default range: last 7 days.
- [x] Filter by `slotKeys`, `provider`, `from`, `to`, optional public statuses.

## 8. Draw query boundary decision

- [x] Keep current `core.draw` query candidates while reviewing.
- [x] Mark public PageModel result widgets as using `core.drawresult`, not `core.draw`.
- [x] Retain `core.draw` queries for private dashboards, vendor POS draw lifecycle, admin, and tenant calendar.
- [ ] Later decide whether old draw latest-result queries are still needed.

## 9. Tests

- [x] Query handler normalizes slot keys/provider.
- [x] `includeHistory=false` does not execute history lookup.
- [x] `includeHistory=false` returns `history = List.of()`.
- [x] `includeHistory=true` defaults history limit to 5.
- [x] `includeHistory=true` caps history limit to 10.
- [x] Public reader works without tenant context.
- [x] PageModel provider calls query with `includeHistory=false`.
- [x] History endpoint enforces date range and page size.
