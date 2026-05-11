# Spec — core.drawresult public queries

## ADDED Requirements

### Requirement: Public draw result slots query

`core.drawresult` SHALL expose a query named `ListPublicDrawResultSlotsQuery` for public slot-oriented result display.

Suggested signature:

```java
public record ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider,
    boolean includeHistory,
    int historyLimit
) implements Query<List<PublicDrawResultSlotView>> {}
```

#### Scenario: Query is global

- **GIVEN** no tenant context is bound
- **WHEN** `ListPublicDrawResultSlotsQuery` is handled
- **THEN** the query SHALL succeed using global `result_slot` and `draw_result` data
- **AND** it SHALL NOT call `TchContext.requireTenantId()`.

#### Scenario: Query with slot filters

- **GIVEN** slot keys `ny_mid`, `FL_EVE`, and duplicates
- **WHEN** the query is handled
- **THEN** slot keys SHALL be normalized to uppercase
- **AND** duplicates SHALL be removed.

#### Scenario: Query with provider filter

- **GIVEN** provider `ny`
- **WHEN** the query is handled
- **THEN** provider SHALL be normalized to uppercase
- **AND** only matching provider slots SHALL be returned.

### Requirement: Query controls history loading

`ListPublicDrawResultSlotsQuery` SHALL use `includeHistory` to control whether recent history is loaded.

#### Scenario: includeHistory false

- **GIVEN** `includeHistory=false`
- **WHEN** the query is handled
- **THEN** `historyLimit` SHALL be treated as `0`
- **AND** the reader SHALL NOT execute history SQL
- **AND** each returned slot view SHALL have `history = List.of()`.

#### Scenario: includeHistory true

- **GIVEN** `includeHistory=true`
- **AND** `historyLimit=5`
- **WHEN** the query is handled
- **THEN** the reader SHALL return up to 5 recent historical results per slot.

#### Scenario: includeHistory true with invalid limit

- **GIVEN** `includeHistory=true`
- **AND** `historyLimit <= 0`
- **WHEN** the query is handled
- **THEN** `historyLimit` SHALL default to 5.

#### Scenario: includeHistory true with excessive limit

- **GIVEN** `includeHistory=true`
- **AND** `historyLimit > 10`
- **WHEN** the query is handled
- **THEN** `historyLimit` SHALL be capped to 10.

### Requirement: Next expected result time calculation

The query SHALL return next expected result time and countdown for each result slot.

#### Scenario: Current time is before today's slot draw time

- **GIVEN** a result slot with timezone `America/New_York` and draw time `14:30`
- **AND** current time in that timezone is before `14:30`
- **WHEN** the query is handled
- **THEN** `next.expectedAt` SHALL point to today at `14:30` in that timezone
- **AND** `countdownSeconds` SHALL be non-negative.

#### Scenario: Current time is after today's slot draw time

- **GIVEN** a result slot with timezone `America/New_York` and draw time `14:30`
- **AND** current time in that timezone is after `14:30`
- **WHEN** the query is handled
- **THEN** `next.expectedAt` SHALL point to tomorrow at `14:30` in that timezone.

#### Scenario: Slot inactive

- **GIVEN** an inactive result slot
- **WHEN** public slot views are assembled
- **THEN** the next status SHALL be `DISABLED`
- **AND** the slot MAY be omitted from default active-slot queries unless explicitly requested.

### Requirement: Public draw result history search query

`core.drawresult` SHALL expose a separate paginated query for advanced public history search.

Suggested signature:

```java
public record SearchPublicDrawResultsQuery(
    List<String> slotKeys,
    String provider,
    LocalDate from,
    LocalDate to,
    String status,
    TchPageable pageable
) implements Query<TchPage<PublicDrawResultView>> {}
```

#### Scenario: History search is paginated

- **GIVEN** a public history search
- **WHEN** the query is handled
- **THEN** it SHALL apply pagination
- **AND** it SHALL enforce configured size and date range limits.
