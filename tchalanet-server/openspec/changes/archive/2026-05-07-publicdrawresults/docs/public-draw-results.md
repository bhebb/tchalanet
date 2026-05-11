# Public Draw Results — Architecture Note

## Purpose

Public result display should be based on global result slots and draw results, not tenant draw lifecycle.

```text
features.publicdrawresults
→ QueryBus
→ core.drawresult public queries
→ result_slot + draw_result
```

## Use cases

### Public home / PageModel

Needs a lightweight payload:

```text
~10 active slots
latest result
next expected result time
countdown
no history
```

Use:

```java
new ListPublicDrawResultSlotsQuery(slotKeys, provider, false, 0)
```

### Public results details page

Needs richer payload:

```text
~10 active slots
latest result
next expected result time
countdown
history 5/10 per slot
```

Use:

```java
new ListPublicDrawResultSlotsQuery(slotKeys, provider, true, 5)
```

### Mobile / terminal

Can consume the same detailed query or HTTP endpoint with `includeHistory=true`.

### Advanced search

Use separate paginated history endpoint/query:

```text
GET /api/v1/public/draw-results/history
```

## Query contract

```java
public record ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider,
    boolean includeHistory,
    int historyLimit
) implements Query<List<PublicDrawResultSlotView>> {}
```

Rules:

```text
includeHistory=false:
  - no history SQL
  - historyLimit forced to 0
  - history = []

includeHistory=true:
  - history SQL enabled
  - default limit = 5
  - max limit = 10
```

## Suggested response shape

```json
{
  "items": [
    {
      "slotKey": "NY_MID",
      "provider": "NY",
      "label": "New York Midday",
      "timezone": "America/New_York",
      "drawTime": "14:30",
      "active": true,
      "next": {
        "expectedAt": "2026-05-05T18:30:00Z",
        "localDate": "2026-05-05",
        "localTime": "14:30",
        "timezone": "America/New_York",
        "countdownSeconds": 3600,
        "status": "WAITING"
      },
      "latest": {
        "resultDate": "2026-05-04",
        "occurredAt": "2026-05-04T18:30:00Z",
        "status": "CONFIRMED",
        "quality": "OK",
        "haiti": {
          "pick3": "123",
          "pick4": "4567"
        },
        "source": {
          "pick3": "123",
          "pick4": "4567"
        }
      },
      "history": []
    }
  ]
}
```

For details/mobile/terminal, `history` contains up to 5/10 recent results.

## Boundary with core.draw

`core.draw` remains tenant-scoped and useful for:

```text
vendor dashboard
admin dashboard
private draw lifecycle
sellable next draws
OPEN/CLOSED/RESULTED/SETTLED status
```

Public result display should not depend on `core.draw`.

## Implementation notes

Because there are about 10 slots, no pagination is needed for the slots query.

Recommended adapter strategy:

```text
1. select active result slots
2. select latest results for those slots
3. optionally select history if includeHistory=true
4. compute next/countdown using Clock
5. assemble in memory
```

This keeps the API/query as one call while allowing simple optimized SQL internally.
