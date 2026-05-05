# Spec 03 — `SalesSession` (extended)

## Domain

`core.session` (extension after `rename-pos-to-terminal`).

## ADDED Requirements

### Requirement: SalesSession carries z-report fields

The `SalesSession` aggregate SHALL include:
`openingFloat`, `closingAmount`, `expectedAmount`, `variance`, `varianceNote`, `closedBy`, `ticketsCount`, `totalStakeHtg`.

On `close(closingAmount, varianceNote, by, clock)`:

- `expectedAmount = openingFloat + totalStakeHtg`
- `variance = closingAmount − expectedAmount`
- `status → CLOSED`

On `abort(reason, by, clock)`:

- `status → ABORTED`; `closedAt` and `closedBy` set.

#### Scenario: Close computes z-report correctly

- **WHEN** `close(closingAmount=500, ...)` is called on a session with `openingFloat=100` and `totalStakeHtg=350`
- **THEN** `expectedAmount=450`, `variance=50`, `status=CLOSED`

#### Scenario: Close requires status OPEN

- **WHEN** `close(...)` is called on a CLOSED or ABORTED session
- **THEN** a domain exception is thrown

---

### Requirement: Status ABORTED added

`SessionStatus` SHALL include `ABORTED` in addition to `OPEN` and `CLOSED`.
The DB constraint on `status` SHALL be updated to `CHECK (status IN ('OPEN','CLOSED','ABORTED'))`.
The close constraint SHALL allow `ABORTED` for the non-null `closed_at` check.

#### Scenario: ABORTED session has closed_at set

- **WHEN** `AbortSalesSessionCommand` is handled
- **THEN** `status=ABORTED`, `closed_at IS NOT NULL`

---

### Requirement: Abstract session counters incremented by TicketPlacedEvent listener

`core.session` SHALL contain `SalesSessionTicketCountersListener` which listens to `TicketPlacedEvent` (from `core.sales`), increments `ticketsCount` and `totalStakeHtg` on the session, and is idempotent via `ProcessedEventPort` with `handler_key = "sales-session.ticket-counters"`.

#### Scenario: Counter idempotent on duplicate event

- **WHEN** the same `TicketPlacedEvent` is received twice
- **THEN** counters are incremented only once

#### Scenario: Counter unavailable if session absent

- **WHEN** `TicketPlacedEvent` references a `sessionId` that does not exist
- **THEN** the listener logs a warning and marks the event processed (no exception)

---

### Requirement: `OpenSalesSessionCommand` validates business rules

The handler SHALL enforce:

1. `ctx.appUserId` not null
2. `ctx.roles` contains `AGENT` (else 403 `NOT_ALLOWED_TO_SELL`)
3. Outlet exists in tenant; user is assigned OR caller is TENANT_ADMIN (audited override)
4. Terminal exists, status=ACTIVE, in `outletId`, in tenant
5. If `terminal.kind == VIRTUAL`: `terminal.ownerAgentId == ctx.appUserId` (else 403)
6. No other OPEN session for `(tenantId, terminalId)` (else 409 `SESSION_ALREADY_OPEN`)
7. `openingFloat >= 0`

#### Scenario: Rejects non-AGENT caller

- **WHEN** `OpenSalesSessionCommand` is handled by a caller without role `AGENT`
- **THEN** 403 `NOT_ALLOWED_TO_SELL` is returned

#### Scenario: Rejects double-open on same terminal

- **WHEN** a second `OpenSalesSessionCommand` targets a terminal that already has an OPEN session
- **THEN** 409 `SESSION_ALREADY_OPEN` is returned

#### Scenario: VIRTUAL terminal owner check

- **WHEN** `OpenSalesSessionCommand` targets a VIRTUAL terminal whose `ownerAgentId != ctx.appUserId`
- **THEN** 403 is returned

---

### Requirement: `AbortSalesSessionsForUserCommand` and `AbortSalesSessionsForTerminalCommand` implemented

Both bulk-abort commands SHALL:

- Load all OPEN sessions for the given scope
- Call `session.abort(reason, by, clock)` on each
- Return count of sessions aborted
- Be idempotent (if no OPEN sessions, return 0)

#### Scenario: AbortSalesSessionsForUserCommand aborts all user sessions

- **WHEN** `AbortSalesSessionsForUserCommand(appUserId)` is handled and 2 OPEN sessions exist for that user
- **THEN** both sessions have `status=ABORTED` and count=2 is returned

---

### Requirement: Session queries include close snapshot

`GetSessionCloseSnapshotQuery` SHALL return a `SessionCloseSnapshot` read-model for a BFF preview endpoint:

```java
record SessionCloseSnapshot(SessionId id, Instant openedAt, Duration duration,
    long ticketsCount, BigDecimal totalStakeHtg, BigDecimal openingFloat, BigDecimal expectedAmount)
```

#### Scenario: Snapshot available for OPEN session

- **WHEN** `GetSessionCloseSnapshotQuery` is called on an OPEN session
- **THEN** a `SessionCloseSnapshot` is returned with correct fields

---

### Requirement: Session events emitted after-commit

Three events SHALL be published:

```java
SalesSessionOpenedEvent(SessionId, TenantId, OutletId, TerminalId, AppUserId, BigDecimal openingFloat, Instant openedAt, boolean viaAdminOverride)
SalesSessionClosedEvent(SessionId, TenantId, OutletId, TerminalId, AppUserId, BigDecimal closingAmount, BigDecimal expectedAmount, BigDecimal variance, Instant closedAt, AppUserId closedBy)
SalesSessionAbortedEvent(SessionId, TenantId, OutletId, TerminalId, AppUserId, String reason, Instant abortedAt, AppUserId abortedBy)
```

#### Scenario: SalesSessionClosedEvent carries variance

- **WHEN** a session is closed with a non-zero variance
- **THEN** `SalesSessionClosedEvent.variance` matches the computed value

---

### Requirement: Session cache

Two named caches SHALL be maintained:

| Cache                              | TTL L1 | TTL L2 | Eviction events                                                                  |
| ---------------------------------- | ------ | ------ | -------------------------------------------------------------------------------- |
| `core.session.current_by_terminal` | 30 s   | 2 min  | `SalesSessionOpenedEvent`, `SalesSessionClosedEvent`, `SalesSessionAbortedEvent` |
| `core.session.current_for_user`    | 30 s   | 2 min  | same                                                                             |

#### Scenario: Cache evicted on session close

- **WHEN** `SalesSessionClosedEvent` is received
- **THEN** both cache entries for the closed session's terminal and user are invalidated

---

### Requirement: Schema additions applied in-place to existing migration

The following DDL SHALL be added to the existing migration that creates `sales_session`:

```sql
ALTER TABLE sales_session
  ADD COLUMN IF NOT EXISTS opening_float   numeric(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS closing_amount  numeric(18,2) NULL,
  ADD COLUMN IF NOT EXISTS expected_amount numeric(18,2) NULL,
  ADD COLUMN IF NOT EXISTS variance        numeric(18,2) NULL,
  ADD COLUMN IF NOT EXISTS variance_note   text NULL,
  ADD COLUMN IF NOT EXISTS closed_by       uuid NULL,
  ADD COLUMN IF NOT EXISTS tickets_count   bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_stake_htg numeric(18,2) NOT NULL DEFAULT 0;

ALTER TABLE sales_session DROP CONSTRAINT IF EXISTS chk_sales_session_status;
ALTER TABLE sales_session ADD  CONSTRAINT chk_sales_session_status
  CHECK (status IN ('OPEN','CLOSED','ABORTED'));

ALTER TABLE sales_session DROP CONSTRAINT IF EXISTS chk_sales_session_close;
ALTER TABLE sales_session ADD  CONSTRAINT chk_sales_session_close CHECK (
  (status = 'OPEN' AND closed_at IS NULL AND closing_amount IS NULL) OR
  (status IN ('CLOSED','ABORTED') AND closed_at IS NOT NULL)
);
```

#### Scenario: Schema validates after migration

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `ddl-auto=validate` passes for the `sales_session` table
