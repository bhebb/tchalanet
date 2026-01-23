# OpenSpec — SALES / Override Result — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/override-result/01-backend.md

---

## Purpose

Provide terminal UI/flows for authorized operators to override a ticket result with explicit outcome and justification. Respect permissions and audit expectations.

---

## Views

### Override Result View (terminal)

- Header: `ticket_code`, `public_code` (if shown), sale/result/settlement badges, amounts.
- Current status: show `result_status` and any existing override metadata.
- Form:
  - `outcome` selector: `WON` or `LOST` (required)
  - `reason_code` input/select (required)
  - `reason_note` textarea (optional; max length)
- Actions:
  - Confirm (submit)
  - Cancel/Back
- Confirmation step: short summary and require explicit confirmation.

---

## Interactions

- Submit override: POST `/api/v1/tenant/tickets/{ticket_id}/override-result` with JSON body.
- On success: update view with `result_status = OVERRIDDEN`, show confirmation toast.
- Prevent double-submission; disable controls while pending.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 404 `sales.override_result.not_found` → "Ticket introuvable"
- 403 `sales.override_result.forbidden` → "Accès refusé"
- 409 `sales.override_result.state_conflict` → "Conflit d'état"
- 422 `sales.override_result.invalid` → "Requête invalide"

Display ProblemDetail fields when useful:

- `detail` as message body, highlight `violations[]` near offending field.

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
  "outcome": "LOST",
  "reason_code": "MANUAL_EXCEPTION",
  "reason_note": "Operator override due to miscomputed result"
}
```

Response sample:

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "result_status": "OVERRIDDEN",
  "override_outcome": "LOST",
  "override_metadata": {
    "reason_code": "MANUAL_EXCEPTION",
    "reason_note": "Operator override due to miscomputed result",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:15:00Z"
  }
}
```
