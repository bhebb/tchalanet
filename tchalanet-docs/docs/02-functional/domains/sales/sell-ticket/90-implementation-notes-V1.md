---

# 📄 `openspec/specs/sales/sell-ticket/90-implementation-notes.md`
*(NON-NORMATIF — guidance / refactor)*

```md
# SALES — sell-ticket — Implementation Notes (NON-NORMATIVE)

This document provides **implementation guidance only**.
It does NOT define behavior and MUST NOT override `01-backend.md`.

---

## Purpose

Help align the existing SALES codebase with the Sell Ticket OpenSpec
while allowing refactoring without breaking contracts.

---

## Pipeline realization (guideline)

The sell-ticket flow naturally decomposes into the following steps:

1. Draft validation (pure)
2. Canonicalization & merge (pure)
3. Session & cutoff validation (IO)
4. Limits & autonomy decision (pure, via read ports)
5. Idempotency acquisition (IO)
6. Ticket persistence (IO)
7. AfterCommit event publication (IO)

A thin command handler SHOULD orchestrate these steps.

---

## Suggested responsibilities

### Domain (pure)

- Draft canonicalization
- Line merge
- Sale decision mapping (limits × autonomy)
- Ticket invariants & state transitions

### Application

- Transaction boundaries
- Pipeline orchestration
- Calls to ports
- Mapping decision → HTTP result

### Ports

- Terminal session lookup
- Draw cutoff lookup
- Limits policy evaluation
- Autonomy policy evaluation
- Ticket persistence
- Idempotency storage
- Event publication

### Integration

- Cross-domain listeners (ledger, payout, results)
- No business rules here

---

## Mapping (example)

| OpenSpec concept   | Existing code                | Target direction      |
| ------------------ | ---------------------------- | --------------------- |
| Canonicalization   | TicketLinePreparationService | DraftCanonicalizer    |
| Merge duplicates   | TicketLinePreparationService | LineMerger            |
| Limits decision    | TicketSalePolicy             | SaleDecisionPolicy    |
| Cutoff check       | DrawCutoffRule               | DrawCutoffPort        |
| Sell orchestration | SellTicketCommandHandler     | Thin pipeline handler |

---

## Idempotency storage (guideline)

Recommended characteristics:

- tenant-scoped (RLS)
- unique `(tenant_id, scope, key)`
- payload hash
- status: `IN_PROGRESS | COMPLETED | FAILED`
- stored result reference for replay

---

## Events

Guideline:

- Emit SALES events **after commit only**
- Do not perform ledger/payout/result side effects inside SALES transactions
- Use integration listeners for cross-domain reactions

---

## Code placement rule (guideline)

- `domain/**`  
  No Spring, no JPA, no ports

- `application/**`  
  Orchestration & transactions

- `infra/**`  
  Adapters (web, persistence, generators)

- `integration/**`  
  Cross-domain listeners only

---

## Final note

OpenSpec defines **what must happen**.  
This document explains **how it can be implemented today**.

Refactoring is allowed as long as:

- behavior remains compliant with `01-backend.md`
- error codes, statuses, and invariants are preserved
