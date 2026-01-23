# OpenSpec — SALES / Print Ticket — Index

## Purpose

Provide printable representations of a ticket for authorized operators.

The system supports:

- PDF for general-purpose printing and sharing
- ESC/POS byte streams for POS printers

Printing reflects the **current ticket state** and is intended for
operations, customer copies, reprints, and audit purposes.

---

## Scope

- Backend (normative, timeless): `01-backend.md`
- Terminal (informative): `02-terminal.md` and `03-terminal.md`

---

## Endpoint(s) — NORMATIVE

- GET `/api/v1/tenant/tickets/{ticket_id}/print.pdf`
  - Content-Type: `application/pdf`
  - Cache-Control: `no-store`
- GET `/api/v1/tenant/tickets/{ticket_id}/print.escpos`
  - Content-Type: `application/octet-stream`
  - Cache-Control: `no-store`
  - Content-Disposition: inline filename recommended

---

## Non-normative (debug-only)

- GET `/api/v1/tenant/tickets/{ticket_id}/print`
  - Returns Base64-encoded PDF
  - Debug-only; MUST NOT be used by UI/POS
  - MAY be removed at any time; not part of the normative contract

---

## Functional rules (summary)

- Only authorized actors may print tickets.
- Tickets in `VOID` state **ARE printable** and MUST be clearly marked as `VOID`.
- Printable content MUST reflect the current ticket state.
- Printing is read-only and does not modify ticket state.

---

## References

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/71-domain-sales.md`
