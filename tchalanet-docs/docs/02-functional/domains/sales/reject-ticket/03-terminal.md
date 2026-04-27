# OpenSpec — SALES / Reject Ticket — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/reject-ticket/01-backend.md

---

## Purpose

Provide terminal/POS flows for authorized operators to reject a ticket that is `PENDING_APPROVAL`, with justification, reflecting the updated ticket state. Respect permissions.

---

## Views

### Reject Ticket View (terminal)

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

- Submit reject: POST `/api/v1/tenant/tickets/{ticket_id}/reject` with JSON body.
- Preconditions (ref backend): only when `sale_status = PENDING_APPROVAL`, ticket not `VOID`, `settlement_status = UNSETTLED`.
- On success: update view to `sale_status = REJECTED`; show confirmation toast.
- Prevent double-submission; disable controls while pending.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 404 `sales.reject_ticket.not_found` → "Ticket introuvable"
- 403 `sales.reject_ticket.forbidden` → "Accès refusé"
- 409 `sales.reject_ticket.state_conflict` → "Rejet impossible (état incompatible)"
- 422 `sales.reject_ticket.invalid` → "Requête invalide"

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
  "reason_code": "LIMIT_EXCEEDED",
  "reason_note": "Exceeded risk threshold"
}
```

Response sample:

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "sale_status": "REJECTED",
  "settlement_status": "UNSETTLED",
  "rejection_metadata": {
    "reason_code": "LIMIT_EXCEEDED",
    "reason_note": "Exceeded risk threshold",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:20:00Z"
  }
}
```
