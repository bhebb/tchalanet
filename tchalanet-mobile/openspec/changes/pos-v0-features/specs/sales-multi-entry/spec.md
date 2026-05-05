# Spec 02 — `Ticket` + `TicketEntry` (multi-entry tickets)

## Domain

`core.sales` (extension — multi-entry model).

## ADDED Requirements

### Requirement: `drawId` lives on `TicketEntry`, not on `Ticket`

A single `Ticket` SHALL contain one or more `TicketEntry` records. Each entry targets one `drawId`, one `betTypeCode`, one `selection`, and one `amountHtg`.
The `Ticket` aggregate SHALL NOT have a `drawId` field.

#### Scenario: Multi-entry ticket persisted correctly

- **WHEN** `PlaceTicketCommand` contains 2 entries (different draws)
- **THEN** one `ticket` row and two `ticket_entry` rows are inserted atomically

#### Scenario: Single-entry ticket still works

- **WHEN** `PlaceTicketCommand` contains exactly 1 entry
- **THEN** one `ticket` row and one `ticket_entry` row are inserted

---

### Requirement: `ticket` and `ticket_entry` tables created via new migration

These tables are NEW (not a rename). A new Flyway migration file SHALL be created (next available version number).
Both tables SHALL be RLS-protected (`BaseTenantEntity`).

Schema:

**`ticket`**:

```sql
id, version, tenant_id, code varchar(32), session_id, outlet_id, terminal_id, app_user_id,
status varchar(24) CHECK IN ('PLACED','PENDING_APPROVAL','PAID','CANCELLED'),
total_amount_htg numeric(18,2), fees_amount_htg numeric(18,2),
customer_phone, sms_optin, sms_sent,
pending_block_reason, pending_limit_htg, approved_by, approved_role, approved_at, approval_reason,
cancelled_by, cancelled_at, cancel_reason,
idempotency_key varchar(128),
created_at, updated_at, created_by, updated_by, deleted_at
```

Indexes:

- `ux_ticket_code_per_tenant ON ticket(tenant_id, code) WHERE deleted_at IS NULL`
- `ux_ticket_idempotency ON ticket(tenant_id, app_user_id, idempotency_key) WHERE deleted_at IS NULL AND idempotency_key IS NOT NULL`
- `ix_ticket_session ON ticket(tenant_id, session_id, created_at DESC) WHERE deleted_at IS NULL`
- `ix_ticket_status ON ticket(tenant_id, status, created_at DESC) WHERE deleted_at IS NULL`

**`ticket_entry`**:

```sql
id, version, tenant_id, ticket_id REFERENCES ticket(id) ON DELETE CASCADE,
draw_id, bet_type_code varchar(32), selection varchar(64), amount_htg numeric(18,2),
is_winner boolean NULL, potential_payout_htg numeric(18,2) NULL,
created_at, updated_at, created_by, updated_by, deleted_at
```

Indexes:

- `ix_ticket_entry_ticket ON ticket_entry(tenant_id, ticket_id) WHERE deleted_at IS NULL`
- `ix_ticket_entry_draw ON ticket_entry(tenant_id, draw_id) WHERE deleted_at IS NULL`

#### Scenario: Schema validates after migration

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `ddl-auto=validate` passes for `ticket` and `ticket_entry`

#### Scenario: RLS enforced

- **WHEN** a query runs without setting `app.current_tenant`
- **THEN** no `ticket` rows are returned

---

### Requirement: `ticket_entry` Envers audit table created in same migration

`ticket_aud` and `ticket_entry_aud` SHALL be created in the same new migration file.

`ticket_aud` columns: mirror all `ticket` columns (nullable) + `rev int4`, `revtype int2`, FK to `revinfo(rev)`.
`ticket_entry_aud` columns: mirror all `ticket_entry` columns (nullable) + `rev int4`, `revtype int2`, FK to `revinfo(rev)`.

#### Scenario: Audit tables exist after migration

- **WHEN** all migrations run
- **THEN** `SELECT 1 FROM ticket_aud` and `SELECT 1 FROM ticket_entry_aud` succeed

---

### Requirement: `PlaceTicketCommand` validates all entries

The handler SHALL enforce:

1. Session exists, status=OPEN, owned by `ctx.appUserId` (or TENANT_ADMIN override).
2. For each entry: draw is OPEN; bet type belongs to the lottery; selection canonicalised; amount within `pos.sale.min/max` from settings.
3. Compute fees (SMS fee if `smsOptin` and `pos.notification.sms_enabled=true`).
4. `totalAmount = sum(entries.amount) + fees`.
5. Run `LimitPolicyRuntimeService` for the aggregate.
   - If BLOCK + no autonomy approval path → throw 422 `SALE_BLOCKED`.
   - If BLOCK + approval required → persist as `PENDING_APPROVAL`.
   - Else → `PLACED`.
6. Persist ticket + entries atomically.
7. After-commit: emit `TicketPlacedEvent`; if `smsOptin` → schedule SMS dispatch.

#### Scenario: Entry with closed draw rejected

- **WHEN** `PlaceTicketCommand` contains an entry referencing a draw with status != OPEN
- **THEN** 422 is returned before persistence

#### Scenario: Idempotent replay returns same ticket

- **WHEN** `PlaceTicketCommand` with the same `idempotencyKey` is received twice
- **THEN** the second call returns the same `ticketId` without inserting a new row

#### Scenario: PENDING_APPROVAL path does not emit TicketPlacedEvent

- **WHEN** ticket is created with status `PENDING_APPROVAL`
- **THEN** `TicketPlacedEvent` is NOT emitted (only emitted after approval)

---

### Requirement: `ApproveBlockedTicketCommandHandler` validates role

The handler SHALL:

1. Load ticket; status must be `PENDING_APPROVAL` (else 409).
2. Verify `ctx.roles` contains the autonomy-required role (else 403 `APPROVAL_INSUFFICIENT_ROLE`).
3. Call `ticket.approve(approver, role, reason, clock)`.
4. After-commit: emit `TicketApprovedEvent` + `TicketPlacedEvent`.

#### Scenario: Wrong role rejected

- **WHEN** `ApproveBlockedTicketCommand` is sent by a caller whose role does not match the required approval role
- **THEN** 403 `APPROVAL_INSUFFICIENT_ROLE` is returned

---

### Requirement: `CancelTicketCommandHandler` enforces time window

The handler SHALL:

1. Status must be `PLACED` or `PENDING_APPROVAL`.
2. If caller has `sales.cancel.any` → allowed.
3. Else if caller has `sales.cancel.self` AND `ticket.appUserId=ctx.appUserId` AND `now - ticket.createdAt < cancelWindow` → allowed.
4. Else → 403 `CANCEL_WINDOW_EXPIRED`.
5. After-commit: emit `TicketCancelledEvent`.

#### Scenario: Cancel outside window rejected

- **WHEN** `CancelTicketCommand` is sent by the agent who placed the ticket AND the cancel window has elapsed
- **THEN** 403 `CANCEL_WINDOW_EXPIRED` is returned

#### Scenario: Admin cancel bypasses window

- **WHEN** `CancelTicketCommand` is sent by a caller with `sales.cancel.any`
- **THEN** cancellation succeeds regardless of time elapsed

---

### Requirement: `TicketPlacedEvent` carries `sessionId`

`TicketPlacedEvent` SHALL include `sessionId` so that `SalesSessionTicketCountersListener` (from `pos-v0-foundation`) can increment counters.

```java
public record TicketPlacedEvent(
    TicketId id, TenantId tenantId, SessionId sessionId,
    OutletId outletId, TerminalId terminalId, AppUserId appUserId,
    BigDecimal totalAmountHtg, List<EntrySnapshot> entries,
    Instant placedAt, UUID eventId
)
```

#### Scenario: TicketPlacedEvent carries sessionId

- **WHEN** `PlaceTicketCommand` is handled successfully
- **THEN** `TicketPlacedEvent.sessionId()` is non-null and matches the session used

---

### Requirement: Audit entries created

- `SALE_PLACED` — STANDARD
- `SALE_PENDING_APPROVAL` — STANDARD
- `SALE_APPROVED` — CRITICAL
- `SALE_CANCELLED` — STANDARD; CRITICAL if `sales.cancel.any` was used

#### Scenario: CRITICAL audit on admin cancel

- **WHEN** a ticket is cancelled by a caller using `sales.cancel.any`
- **THEN** audit entry `SALE_CANCELLED` has audit level CRITICAL
