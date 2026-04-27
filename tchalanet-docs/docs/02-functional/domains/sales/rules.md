# OpenSpec — SALES — Rules (v1)

## Purpose

This document expands the **shared invariants** referenced by `domains/sales/00-index.md`.
It is normative for all Sales implementations (server/web/mobile/terminal).

---

## 1) Status model (canonical)

### saleStatus

- `SOLD`: ticket is accepted and active
- `PENDING_APPROVAL`: created but requires approval (autonomy partial)
- `REJECTED`: rejected by operator/admin
- `VOID`: cancelled/voided; irreversible

### resultStatus

- `NOT_RESULTED`: no result applied yet
- `WON` / `LOST`: resulted outcomes
- `OVERRIDDEN`: outcome manually overridden (audited)

### settlementStatus

- `UNSETTLED`: not settled/paid out yet
- `SETTLED`: settlement completed

### Transition rule

Any illegal transition MUST return `ProblemDetail` code `ticket.state_conflict`.

---

## 2) SELL idempotency (required)

The SELL endpoint MUST enforce idempotency via header `Idempotency-Key`.

### Rules

- Missing key → `400` `idempotency.missing`
- Same key + same payload:
  - if completed → replay same ticket response
- Same key + different payload → `409` `idempotency.payload_mismatch`
- Still processing → `409` `idempotency.in_progress`

### Dedup scope

Server MUST deduplicate on `(tenantId, scope, idempotencyKey)` where `scope` is at least:

- endpoint family (sell-ticket)
- and optionally outlet/terminal if your idempotency spec requires it

---

## 3) Pattern resolution (seller selects option)

Pattern resolution MUST follow:
`(game + bet_type + option) -> pattern_key`

- Seller MUST choose `option`
- Seller MUST NOT choose `pattern_key` directly
- If resolution fails → `422` `ticket.invalid` (with field-level details)

---

## 4) Limits & autonomy

### Autonomy levels

- `none`: always requires approval? (tenant policy)
- `partial`: SELL creates ticket with `saleStatus=PENDING_APPROVAL`
- `full`: SELL can directly complete with `saleStatus=SOLD`

### Limits evaluation

- Limits MUST be evaluated at sale time
- Blocking limit MUST fail the sale with:
  - `409` `limit.blocked`

---

## 5) Cancel / VOID semantics

Cancel action:

- Endpoint: `PATCH /api/v1/tenant/tickets/{ticketId}/cancel`
- Resulting state: `saleStatus=VOID`
- Cancel is irreversible
- Illegal cancel attempt MUST return:
  - `409` `ticket.state_conflict`

---

## 6) Public verification rules

### Visibility window

Tenant defines a visibility window. When expired:

- `410` `ticket.expired`

### Masking

Public endpoints MUST NOT expose:

- seller identity
- full outlet identity (only masked)
- any internal-only identifiers

### Abuse control

Public verify MUST be rate-limited.
Batch verify MUST enforce max size (20) with:

- `422` `verify.too_many_codes`

---

## 7) Error envelope rules (canonical)

- Success: `ApiResponse<T>` (optional `notices[]`)
- Errors: `ProblemDetail` ONLY (never wrapped)

Error codes MUST be stable and reused consistently by controllers.

---

## 8) Audit & events

Critical actions MUST:

- emit an audit event
- publish domain events **AfterCommit**:
  - sell
  - approve
  - reject
  - cancel
  - override-result

---

## 9) Security / RLS (tenant isolation)

- Tenant isolation MUST be enforced via request context
- Client-provided tenant identifiers MUST NEVER be trusted
- All tenant endpoints MUST behave as tenant-scoped
