# OpenSpec — SALES / Approve Ticket — Index

## Purpose

Approve a ticket currently `PENDING_APPROVAL` to transition it to `SOLD`.

Approval does not:

- settle the ticket
- result the ticket
- modify lines or totals

Approval is an operational action controlled by permissions and fully audited.

---

## Scope

- Backend (normative, timeless): `01-backend.md`
- Frontend (informative): `02-frontend.md`
- Terminal (informative, optional): `03-terminal.md`
- Implementation notes (optional, non-normative): `90-implementation-notes.md`

---

## Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/71-domain-sales.md`
- `openspec/context/20-backend-rules.md`

---

## Cross-references

- `openspec/specs/sales/sell-ticket/01-backend.md` (idempotency rules apply only to SELL)
- `openspec/context/26-ticket-codes.md` (ticket_code and public_code invariants)
