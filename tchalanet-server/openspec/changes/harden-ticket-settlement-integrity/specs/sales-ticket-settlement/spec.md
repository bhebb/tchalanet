## ADDED Requirements

### Requirement: Settlement uses `ticket_settlement` as idempotence guard

`RecordDrawTicketsResultCommandHandler` SHALL insert one row in `ticket_settlement(tenant_id, ticket_id, draw_result_id)` BEFORE applying `Ticket.markResulted` for each ticket. If the INSERT fails because the row already exists (UNIQUE conflict), the handler SHALL skip the ticket without loading or mutating it. The skip SHALL be counted in a `skipped` metric and logged at INFO level.

The INSERT SHALL happen in the same transaction (`@TchTx`) as the `Ticket.save(...)` so that both either commit or roll back together.

#### Scenario: First settlement of a ticket inserts ticket_settlement row

- **GIVEN** a `Ticket(SOLD, NOT_RESULTED)` and a `DrawResult` with id `dr-1`
- **WHEN** `RecordDrawTicketsResultCommand` is processed
- **THEN** a row `(tenant_id, ticket_id, dr-1)` is inserted in `ticket_settlement`
- **AND** the ticket is updated to `(SOLD, WON|LOST, UNSETTLED)`
- **AND** `TicketResultedEvent` is published after commit

#### Scenario: Re-play of the same draw skips already-settled tickets

- **GIVEN** 3 tickets for draw `d-1` already settled in a previous run (3 rows in `ticket_settlement`)
- **WHEN** `RecordDrawTicketsResultCommand(drawId=d-1, drawResultId=dr-1)` is processed again (re-play)
- **THEN** the handler skips all 3 tickets
- **AND** the result is `processed=0, won=0, lost=0, skipped=3`
- **AND** the batch does NOT throw `IllegalStateException`

#### Scenario: Mixed batch — some tickets already settled, some not

- **GIVEN** 5 tickets for draw `d-2`: 2 already in `ticket_settlement`, 3 not
- **WHEN** `RecordDrawTicketsResultCommand` is processed
- **THEN** the result is `processed=3, skipped=2`
- **AND** only 3 new rows are added to `ticket_settlement`
- **AND** only 3 `TicketResultedEvent` are published

### Requirement: `DrawResultedEventListener` uses `ProcessedEventPort`

The `DrawResultedEventListener` SHALL gate processing of `DrawResultAppliedEvent` through `ProcessedEventPort` with key `(eventId, "RecordDrawTicketsResult")`. If the event has already been processed, the listener SHALL log INFO and return without sending the command. After successful command dispatch, the listener SHALL mark the event as processed.

#### Scenario: First reception of an event triggers settlement

- **GIVEN** `DrawResultAppliedEvent(eventId=e-1, ...)` was never processed
- **WHEN** the listener receives it
- **THEN** `RecordDrawTicketsResultCommand` is sent via `CommandBus`
- **AND** the event is marked processed via `ProcessedEventPort`

#### Scenario: Second reception of the same event is skipped

- **GIVEN** `DrawResultAppliedEvent(eventId=e-1, ...)` was already processed
- **WHEN** the listener receives it again
- **THEN** no command is sent
- **AND** an INFO log entry mentions the skip

### Requirement: `Ticket.forceResult` refuses SETTLED tickets

`Ticket.forceResult(payout, when)` and `Ticket.forceResult(payout, resultStatus, when)` SHALL throw `IllegalStateException` (or domain-equivalent) if the ticket is in `settlementStatus = SETTLED`. The state of the ticket SHALL NOT change.

This protects against silently downgrading a paid-out ticket from `SETTLED` back to `UNSETTLED`, which could trigger a duplicate payout.

#### Scenario: Override of UNSETTLED ticket succeeds

- **GIVEN** a `Ticket(SOLD, LOST, UNSETTLED, winning=0)`
- **WHEN** `Ticket.forceResult(100, WON, now)` is called
- **THEN** the ticket transitions to `(SOLD, WON, UNSETTLED, winning=100, resultedAt=now)`

#### Scenario: Override of SETTLED ticket throws

- **GIVEN** a `Ticket(SOLD, WON, SETTLED, winning=100)` (payout already executed)
- **WHEN** `Ticket.forceResult(50, LOST, now)` is called
- **THEN** `IllegalStateException` is thrown
- **AND** the ticket remains `(SOLD, WON, SETTLED, winning=100)` — no field changed

### Requirement: Override endpoint returns 409 for SETTLED tickets

`OverrideTicketResultCommandHandler` SHALL check `ticket.settlementStatus` before calling `Ticket.forceResult`. If `SETTLED`, the handler SHALL throw `ProblemRest.conflict("Cannot override result of an already SETTLED ticket. ...")`. The HTTP response SHALL be `409 Conflict` with a Problem JSON body.

The 409 response is preferred over the `IllegalStateException` 500 that would result from `forceResult`, to give a clean API contract.

#### Scenario: Override on UNSETTLED ticket via API succeeds

- **WHEN** `PATCH /tenant/tickets/{id}/result/override` is called with valid payload on an UNSETTLED ticket
- **THEN** the response is HTTP 200 with `ApiResponse.success(null)`
- **AND** `TicketResultOverriddenEvent` is published after commit

#### Scenario: Override on SETTLED ticket returns 409

- **GIVEN** the ticket has `settlementStatus = SETTLED`
- **WHEN** `PATCH /tenant/tickets/{id}/result/override` is called
- **THEN** the response is HTTP 409 Conflict with body explaining the constraint
- **AND** no `TicketResultOverriddenEvent` is published

### Requirement: `OverrideTicketResultCommand` uses `resultStatus` directly (not `TicketStatus`)

`OverrideTicketResultCommand` and `OverrideTicketResultRequest` SHALL expose a single field `resultStatus: TicketResultStatus` instead of `status: TicketStatus`. The handler reads only this field. Clients SHALL NOT be able to send `saleStatus` or `settlementStatus` through this endpoint (those are managed by other workflows).

`resultStatus` MUST be `WON` or `LOST` — `NOT_RESULTED` and `OVERRIDDEN` are rejected with `400 Bad Request`.

#### Scenario: Request with valid resultStatus is accepted

- **WHEN** the request body is `{ "totalPayout": 100, "resultStatus": "WON", "reason": "...", "performedBy": "...", "performedAt": "..." }`
- **THEN** the command is dispatched and the override applied

#### Scenario: Request with resultStatus = NOT_RESULTED is rejected

- **WHEN** the request body contains `"resultStatus": "NOT_RESULTED"`
- **THEN** the response is HTTP 400 with body explaining `resultStatus must be WON or LOST`

#### Scenario: Request with deprecated `status` shape is rejected (BREAKING)

- **WHEN** the request body contains `"status": { "saleStatus": ..., "resultStatus": ..., "settlementStatus": ... }` (old shape)
- **THEN** the response is HTTP 400 (deserialization error or validation error) — clients MUST migrate to `resultStatus`
