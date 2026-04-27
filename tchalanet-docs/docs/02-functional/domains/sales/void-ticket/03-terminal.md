# OpenSpec — SALES / Void Ticket — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/void-ticket/01-backend.md

---

## Purpose

Provide terminal/POS flows for authorized operators to void (cancel) a ticket, with justification, reflecting the updated ticket state. Respect permissions and v1 constraint: void is not allowed when `settlement_status = SETTLED`.

---

## Views

### Void Ticket View (terminal)

- Header: `ticket_code`, optional `public_code`, sale/result/settlement badges, amounts.
- Current status: show `sale_status` and `settlement_status`.
- Form:
  - `reason_code` input/select (required)
  - `reason_note` textarea (optional; max length 500)
- Actions:
  - Confirm (submit)
  - Cancel/Back
- Confirmation step: short summary and explicit confirmation.

---

## Interactions

- Submit void: POST `/api/v1/tenant/tickets/{ticket_id}/void` with JSON body.
- On success: update view to `sale_status = VOID`; show confirmation toast.
- Prevent double-submission; disable controls while pending.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 404 `sales.void_ticket.not_found` → "Ticket introuvable"
- 403 `sales.void_ticket.forbidden` → "Accès refusé"
- 409 `sales.void_ticket.state_conflict` → "Annulation impossible (ticket déjà réglé)"
- 422 `sales.void_ticket.invalid` → "Requête invalide"

Display ProblemDetail fields when useful:

- `detail` as message body; highlight `violations[]` near offending field.

---

## Performance & UX constraints

- Keep form minimal; enforce client-side validation to reduce retries.
- Optimize for small screens and limited input methods.

---

## Accessibility

- Clear labels and error messages associated with inputs.
- Large touch targets; hardware-key navigation compatible.

---

## Example payloads

Request sample:

```json
{
  "reason_code": "CUSTOMER_REQUEST",
  "reason_note": "Customer asked to cancel immediately"
}
```

Response sample:

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "sale_status": "VOID",
  "settlement_status": "UNSETTLED",
  "void_metadata": {
    "reason_code": "CUSTOMER_REQUEST",
    "reason_note": "Customer asked to cancel immediately",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:25:00Z"
  }
}
```
