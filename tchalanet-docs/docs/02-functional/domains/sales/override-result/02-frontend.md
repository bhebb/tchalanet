# OpenSpec — SALES / Override Result — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/71-domain-sales.md
- openspec/specs/sales/override-result/01-backend.md

---

## Purpose

Provide an admin/operator UI to manually override a ticket result with explicit outcome (`WON`/`LOST`), capture justification, and reflect the updated ticket state. Respect permissions and audit requirements.

---

## Views

### Override Result Dialog/Page

- Header: ticket identifiers (`ticket_code`, `public_code`), current statuses (sale/result/settlement), amounts.
- Current result block: show current `result_status` and any prior override metadata (if present).
- Form fields:
  - `outcome` selector: `WON` or `LOST` (required)
  - `reason_code` input/select (required; from a stable catalog)
  - `reason_note` textarea (optional; max length enforced)
- Actions:
  - Confirm (submit override)
  - Cancel/Close
- Confirmation modal: summarize chosen outcome and reason before submission.

---

## Data contract

- Request binds to `OverrideResultRequest`:
  - `outcome` (WON|LOST) — required
  - `reason_code` — required
  - `reason_note` — optional (max length)
- Response binds to `OverrideResultResponse` with updated `result_status = OVERRIDDEN` and `override_metadata`.
- Do not duplicate backend logic; backend is source of truth.
- No `tenant_id` in body; derived from auth.

---

## Interactions

- Submit override: POST `/api/v1/tenant/tickets/{ticket_id}/override-result` with JSON body.
- On success: update UI state from response; show confirmation message; optionally refresh detail view.
- Prevent double-submit; disable submit while pending.
- Log UX-level action (optional) to local telemetry.

---

## Error handling

Map HTTP status and canonical codes to UX messages:

- 404 `sales.override_result.not_found` → "Ticket introuvable"
- 403 `sales.override_result.forbidden` → "Accès refusé"
- 409 `sales.override_result.state_conflict` → "Conflit d'état (ticket invalide pour override)"
- 422 `sales.override_result.invalid` → "Requête invalide (vérifiez le formulaire)"

Display ProblemDetail fields when appropriate:

- title/detail for general context
- violations[] for field-level hints (e.g., `reason_code` required, `outcome` enum)

---

## Privacy & permissions

- Only show override UI to authorized roles.
- Do not expose sensitive PII; show minimal identifiers.

---

## Performance

- Lightweight post; optimistic UI optional but must reconcile with server response.

---

## Accessibility

- Clear radio/select for outcome with accessible labels.
- Validation errors announced and associated with fields.

---

## Examples (UI data bindings)

Request sample:

```json
{
  "outcome": "WON",
  "reason_code": "ADMIN_CORRECTION",
  "reason_note": "Correction suite à décision officielle"
}
```

Response sample (200):

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "SOLD",
  "result_status": "OVERRIDDEN",
  "override_outcome": "WON",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "override_metadata": {
    "reason_code": "ADMIN_CORRECTION",
    "reason_note": "Correction suite à décision officielle",
    "actor_id": "user_01J2...",
    "occurred_at": "2026-01-18T10:15:00Z"
  },
  "updated_at": "2026-01-18T10:15:00Z"
}
```
