## ADDED Requirements

### Requirement: Pending settlement queries use sale_status

Settlement pending queries MUST filter sold tickets using the physical `ticket.sale_status` column, not a non-existent `ticket.status` column.

#### Scenario: Pending tickets exist for draw

- **GIVEN** a ticket row with `draw_id = D`, `sale_status = 'SOLD'`, `result_status = 'NOT_RESULTED'`, and `deleted_at IS NULL`
- **WHEN** `existsPendingByDrawId(D)` is called
- **THEN** it returns `true`
- **AND** the native SQL does not reference `t.status`

#### Scenario: Pending count for draw

- **GIVEN** three sold non-deleted ticket rows for draw `D`
- **WHEN** `countPendingByDrawId(D)` is called
- **THEN** it returns `3`
- **AND** the native SQL filters with `t.sale_status = 'SOLD'`

### Requirement: Settlement batch limit is applied in the database

`TicketSettlementJpaAdapter.findNextBatchForDraw` MUST pass a `Pageable` to the repository query so the database limits the batch size.

#### Scenario: Batch size is smaller than eligible rows

- **GIVEN** 200 eligible `SOLD + NOT_RESULTED` tickets for a draw
- **WHEN** `findNextBatchForDraw(drawId, cursor, 50)` is called
- **THEN** the repository receives a page request with size `50`
- **AND** the adapter does not fetch all 200 rows and limit in Java

### Requirement: Settlement result processing is idempotent

Recording draw ticket results MUST be idempotent per `(tenantId, ticketId, drawResultId)` and replay-safe.

#### Scenario: Ticket was already settled for draw result

- **GIVEN** `ticket_settlement` already contains `(tenantId, ticketId, drawResultId)`
- **WHEN** draw result processing replays
- **THEN** the ticket is skipped
- **AND** the batch continues processing remaining eligible tickets
- **AND** no ticket is paid twice

### Requirement: Result override does not downgrade settled tickets

Overriding a ticket result MUST NOT silently change a `SETTLED` ticket back to `UNSETTLED`.

#### Scenario: Admin overrides settled ticket

- **GIVEN** a ticket with `settlementStatus = SETTLED`
- **WHEN** an admin override result command is handled
- **THEN** the command is rejected with conflict semantics
- **AND** the ticket settlement status remains `SETTLED`

### Requirement: Force result overload is semantically clear

The `Ticket.forceResult(payout, when)` overload MUST be renamed or removed so the result status semantics are explicit.

#### Scenario: Override as resulted shortcut is used

- **WHEN** code needs to override a ticket as resulted without passing a status
- **THEN** it calls a method named `overrideAsResulted(payout, when)` or another explicit name
- **AND** ambiguous overload usage no longer exists
