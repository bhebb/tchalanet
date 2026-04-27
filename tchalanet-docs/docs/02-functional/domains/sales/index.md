# OpenSpec — SALES (Index) — v1 (ACTED)

## Scope

This index defines all SALES-related use cases for the platform.
It is the **single source of truth** for ticket sales, lifecycle, and public verification.

---

## Features

- sell-ticket/00-index.md
- print-ticket/00-index.md
- approve-ticket/00-index.md
- reject-ticket/00-index.md
- list-tickets/00-index.md
- get-ticket-detail/00-index.md
- public-verify-ticket/00-index.md
- override-result/00-index.md

> Rule: Any new SALES capability requires:
>
> - a new feature folder
> - an explicit update to this index

---

## Shared invariants (single source here)

### Ticket lifecycle

- `saleStatus`: `SOLD` | `PENDING_APPROVAL` | `REJECTED` | `VOID`
- `resultStatus`: `NOT_RESULTED` | `WON` | `LOST` | `OVERRIDDEN`
- `settlementStatus`: `UNSETTLED` | `SETTLED`

State transitions are enforced by the domain model.
Illegal transitions MUST result in `ticket.state_conflict`.

---

## Business invariants

### Idempotency (SELL)

- Header `Idempotency-Key` is **REQUIRED** on `POST /api/v1/tenant/tickets`
- Read: `openspec/context/25-idempotency.md`
- Client MUST generate **one key per sale intent**
- Client MUST reuse the same key for retries of the same intent
- Server MUST deduplicate on `(tenantId, scope, idempotencyKey)`
- Same key + same payload:
  - If already completed → server MUST replay the same ticket
- Same key + different payload:
  - `409` `ProblemDetail` → `idempotency.payload_mismatch`
- Missing key:
  - `400` `ProblemDetail` → `idempotency.missing`
- Request still processing:
  - `409` `ProblemDetail` → `idempotency.in_progress`

### Pattern resolution

- Pattern resolution rule: `(game + bet_type + option) -> pattern_key`
- `option` is chosen by the seller
- Seller NEVER selects a pattern directly

### Limits & autonomy

- Autonomy levels: `none` | `partial` | `full`
- `partial` autonomy:
  - Ticket is created with `saleStatus=PENDING_APPROVAL`
- Limits are evaluated at sale time
- Blocking limits MUST fail the sale

### Cancel semantics

- Action: `cancel`
- Resulting state: `saleStatus=VOID`
- Cancel is irreversible

### Audit

- All critical actions MUST emit an audit event:
  - sell
  - approve
  - reject
  - cancel
  - override-result

### Access scope (high-level)

- Seller:
  - own tickets (or outlet-scoped, per tenant policy)
- Operator / Admin:
  - outlet or tenant scope

---

## Contracts (shared)

- Success responses: `ApiResponse<T>`
  - Optional `notices[]` for warnings
- Errors:
  - `ProblemDetail` only (never wrapped)
- IDs:
  - Typed IDs everywhere outside persistence
- Events:
  - Published **AfterCommit** for all critical actions
- RLS:
  - Tenant isolation enforced via request context
  - Client-provided tenant identifiers MUST NEVER be trusted

---

## ProblemDetail error codes (canonical)

> Rule:
>
> - One concept = one code
> - Codes are stable
> - Controllers MUST reuse these codes

### Ticket (tenant)

- `ticket.not_found`
- `ticket.invalid`
- `ticket.state_conflict`
- `idempotency.missing`
- `idempotency.payload_mismatch`
- `idempotency.in_progress`

### Public verify

- `ticket.expired`
- `verify.invalid_payload`
- `verify.too_many_codes`

### Limits / Draw

- `limit.blocked`
- `draw.closed`

_(Optional later)_

- `auth.forbidden`
- `auth.unauthorized`
- `rate_limited`

---

## Ticket codes

- `ticket_code`:
  - Internal, human-friendly identifier for ops/support/search
  - Unique per tenant
  - Always present for tenant endpoints
  - Stable (never changes)
  - Example: `TCK-260113-214501-9K3W2H-7`
- `public_code`:
  - Public shareable code for verification (QR/SMS/URL)
  - Non-guessable (random)
  - Always present in MVP
  - Used by `GET /api/v1/public/tickets/verify/{publicCode}`
  - Example: `9Q2H7M4K1PZX`

---

## API Map — Sales / Tickets (v1)

### PUBLIC

**Base path**: `/api/v1/public/tickets`

#### Verify (single)

- **GET** `/verify/{publicCode}`
- Purpose:
  - Public ticket verification
- Responses:
  - `200` `ApiResponse<TicketPublicView>`
  - `404` `ticket.not_found`
  - `410` `ticket.expired`
- Security:
  - Masked data
  - HTTPS only
  - Noindex + rate-limited

#### Verify (batch)

- **POST** `/verify`
- Body:
  - `{ codes: string[] }` (max 20)
- Responses:
  - `200` `ApiResponse<TicketPublicBatchView>`
  - `422` `verify.invalid_payload`
  - `422` `verify.too_many_codes`

---

### TENANT

**Base path**: `/api/v1/tenant/tickets`

#### Sell (create ticket)

- **POST** `/`
- Purpose:
  - Create a ticket with one or more lines
- Headers:
  - `Idempotency-Key` (REQUIRED)
- Responses:
  - `201` → `saleStatus=SOLD`
  - `202` → `saleStatus=PENDING_APPROVAL`
  - `409` → `limit.blocked`, `draw.closed`, `ticket.state_conflict`
  - `422` → `ticket.invalid`

#### List tickets

- **GET** `/`
- Purpose:
  - Paged list of tickets
- Pagination:
  - `@TchPaging(...)`
- Responses:
  - `200` `ApiResponse<TchPage<TicketItemResponse>>`

#### Get ticket detail

- **GET** `/{ticketId}`
- Responses:
  - `200` `ApiResponse<TicketDetailResponse>`
  - `404` `ticket.not_found`

#### Cancel (void)

- **PATCH** `/{ticketId}/cancel`
- Responses:
  - `200` `ApiResponse<TicketDetailResponse>`
  - `404` `ticket.not_found`
  - `409` `ticket.state_conflict`

#### Approve

- **POST** `/{ticketId}/approve`
- Responses:
  - `200`
  - `404`
  - `409`

#### Reject

- **POST** `/{ticketId}/reject`
- Responses:
  - `200`
  - `404`
  - `409`

#### Override result

- **PATCH** `/{ticketId}/result/override`
- Responses:
  - `200`
  - `404`
  - `409`

#### Print (ESC/POS)

- **GET** `/{ticketId}/print.escpos`
- Produces:
  - `application/octet-stream`
- Responses:
  - `200`
  - `404`
  - `409`

#### Print (PDF)

- **GET** `/{ticketId}/print.pdf`
- Produces:
  - `application/pdf`
- Responses:
  - `200`
  - `404`
  - `409`

---

## Status

- SALES API Map is **ACTED (v1)** for MVP
