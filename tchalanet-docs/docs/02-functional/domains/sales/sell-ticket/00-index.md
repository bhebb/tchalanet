# OpenSpec — SALES / Sell Ticket — Index (v1)

- openspec/context/05-version-guard.md
- openspec/context/10-non-negotiables.md
- openspec/context/25-idempotency.md
- openspec/context/26-ticket-codes.md

## Goal

Sell a ticket for a given draw and terminal session:

- Validate sale window (draw cutoff)
- Normalize and merge lines
- Evaluate limits and autonomy
- Persist ticket as `SOLD` or `PENDING_APPROVAL`
- Return identifiers (`ticket_code`, `public_code`) and status

## Non-goals (v1)

- Payment processing / refunds
- Bulk sell
- Settlement/payout workflow (separate domain)
- Approval workflow details beyond ticket state transitions (separate domain later)

---

## Actors

- Seller (terminal/web): creates the sale
- Operator/Admin: approves/rejects (separate features)

---

## User stories

1. As a seller, I can sell a ticket for a draw and receive a `ticket_code` and `public_code`.
2. As a seller, if the network fails, I can retry safely without creating duplicates (idempotency).
3. As a seller with partial autonomy, my ticket is created as `PENDING_APPROVAL`.

---

## Use case summary

Input:

- Tenant context + terminal context
- Draw id
- Lines[]: selections, stake, bet_type, bet_option

Processing pipeline (normative):

1. Validate at least one line
2. Validate open POS session for terminal
3. Validate draw is before cutoff
4. Normalize selections (server canonicalization)
5. Merge duplicates (server sums stakes)
6. Evaluate limits and autonomy
7. Create ticket:
   - If approval required -> `PENDING_APPROVAL`
   - Else -> `SOLD`
8. Emit audit + AfterCommit event(s)
9. Return response

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
