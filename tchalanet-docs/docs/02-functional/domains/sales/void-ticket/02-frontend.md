# OpenSpec — SALES / Void Ticket — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/71-domain-sales.md
- openspec/specs/sales/void-ticket/01-backend.md

---

## Purpose

Provide an admin/operator UI to void (cancel) a ticket, capturing justification and reflecting the updated ticket state. Respect permissions and the v1 constraint: void is not allowed when `settlement_status = SETTLED`.

---

## Views

### Void Ticket Dialog/Page

- Header: ticket identifiers (`ticket_code`, `public_code`), current statuses (sale/result/settlement), amounts.
- Current status block: display current `sale_status` and `settlement_status`.
- Form fields:
  - `reason_code` input/select (required; from a stable catalog)
  - `reason_note` textarea (optional; max length 500)
- Actions:
  - Confirm (submit void)
  - Cancel/Close
- Confirmation modal: summarize reason before submission.

---

## Data contract

- Request binds to `VoidTicketRequest`:
  - `reason_code` — required
  - `reason_note` — optional (max length 500)
- Response binds to `VoidTicketResponse` with updated `sale_status = VOID` and `void_metadata`.
- Do not duplicate backend logic; backend is source of truth.
- No `tenant_id` in body; derived from auth.

---

## Interactions

- Submit void: POST `/api/v1/tenant/tickets/{ticket_id}/void` with JSON body.
- On success: update UI state from response; show confirmation message; optionally refresh detail/list.
- Prevent double-submit; disable submit while pending.

---

## Error handling

Map HTTP status and canonical codes to UX messages:

- 404 `sales.void_ticket.not_found` → "Ticket introuvable"
- 403 `sales.void_ticket.forbidden` → "Accès refusé"
- 409 `sales.void_ticket.state_conflict` → "Annulation impossible (ticket déjà réglé)"
- 422 `sales.void_ticket.invalid` → "Requête invalide (vérifiez le formulaire)"

Display ProblemDetail fields when appropriate:

- title/detail for general context
- violations[] for field-level hints (e.g., `reason_code` required, `reason_note` length)

---

## Privacy & permissions

- Only show void UI to authorized roles.
- Do not expose sensitive PII; show minimal identifiers.

---

## Performance

- Lightweight post; optimistic UI optional but must reconcile with server response.

---

## Accessibility

- Clear labels for `reason_code` and `reason_note`; associate errors with fields.

---

## Examples (UI data bindings)

Request sample:

```json
{
  "reason_code": "CUSTOMER_REQUEST",
  "reason_note": "Customer asked to cancel immediately"
}
```

Response sample (200):

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "VOID",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "void_metadata": {
    "reason_code": "CUSTOMER_REQUEST",
    "reason_note": "Customer asked to cancel immediately",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:25:00Z"
  },
  "updated_at": "2026-01-18T10:25:00Z"
}
```
