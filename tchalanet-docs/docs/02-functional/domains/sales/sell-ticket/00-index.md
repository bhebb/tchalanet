# OpenSpec — SALES / Sell Ticket — Index (v1)

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/25-idempotency.md
- openspec/context/26-ticket-codes.md

## Goal

Sell a ticket for a given draw and operational POS context using a two-step flow:

- Validate sale window (draw cutoff)
- Normalize and merge lines
- Evaluate limits and autonomy
- Create a server-side sale preparation
- Confirm exactly that preparation with idempotency
- Persist ticket as `SOLD` or `PENDING_APPROVAL`
- Return identifiers (`ticket_code`, `public_code`) and status

## Non-goals (v1)

- Payment processing / refunds
- Bulk sell
- Settlement/payout workflow (separate domain)
- Approval workflow details beyond ticket state transitions (separate domain later)

---

## Actors

- Seller terminal: creates the sale
- Tenant admin or super admin in Admin POS mode: creates the sale after selecting a terminal context
- Operator/Admin: approves/rejects (separate features)

---

## User stories

1. As a seller, I can prepare a sale for a draw before committing it.
2. As a seller, I can confirm a prepared sale and receive a `ticket_code` and `public_code`.
3. As a seller, if confirmation fails over the network, I can retry safely without creating duplicates (idempotency).
4. As a seller with partial autonomy, my ticket is created as `PENDING_APPROVAL`.

---

## Use case summary

Prepare input:

- Tenant context + terminal context
- Draw id
- Lines[]: selections, stake, bet_type, bet_option

Prepare pipeline (normative):

1. Validate at least one line
2. Resolve operational POS context:
   - `SELLER_TERMINAL` actor from terminal session, or
   - `APP_USER` admin actor with explicit Admin POS terminal selection
3. Validate draw is before cutoff
4. Normalize selections (server canonicalization)
5. Merge duplicates (server sums stakes)
6. Evaluate promotions and generate prepared promotional lines
7. Persist sale preparation and return `preparationId`

Confirm input:

- `preparationId`
- `Idempotency-Key`

Confirm pipeline (normative):

1. Load the server-side preparation
2. Re-check cutoff and sale context
3. Re-check limits/autonomy on prepared final lines
4. Create ticket:
   - If approval required -> `PENDING_APPROVAL`
   - Else -> `SOLD`
5. Emit audit + AfterCommit event(s)
6. Return response

---

## Decision table — limits/autonomy outcome (normative)

| Limit overall outcome | Autonomy policy allows approval on block? | Result                                       |
| --------------------- | ----------------------------------------: | -------------------------------------------- |
| OK / WARN             |                                       n/a | Ticket created `SOLD` (HTTP 201)             |
| BLOCK                 |                                       YES | Ticket created `PENDING_APPROVAL` (HTTP 202) |
| BLOCK                 |                                        NO | Sale rejected: `409 limit.blocked`           |

Notes:

- WARN does not block creation; server returns SOLD with `notices[]`.

---

## Documents

- Backend: `01-backend.md`
- Frontend: `02-frontend.md`
- Terminal: `03-terminal.md`

---

## Definition of Done (spec)

- Request/response schemas defined (with examples)
- Status codes and ProblemDetail codes are canonical and mapped
- Idempotency behavior specified (replay/mismatch/in_progress)
- Normalization + merge rules specified
- Limits/autonomy decision table included
