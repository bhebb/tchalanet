# OpenSpec — SALES / Print Ticket — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/71-domain-sales.md
- openspec/specs/sales/print-ticket/01-backend.md

---

## Purpose

Provide UI actions to print a ticket using the normative endpoints: PDF and ESC/POS. Respect permissions, privacy, and caching rules; exclude debug-only endpoints from UI.

---

## Views

### Ticket Detail / Actions

- Buttons:
  - Print PDF → calls `/api/v1/tenant/tickets/{ticket_id}/print.pdf`
  - Print ESC/POS → calls `/api/v1/tenant/tickets/{ticket_id}/print.escpos`
- Status: show sale/result/settlement statuses for context.
- Reprint allowed for authorized roles.

---

## Data contract

- No request body; use `ticket_id` path param.
- PDF response: `application/pdf` (binary).
- ESC/POS response: `application/octet-stream` (binary) with inline filename recommended.

---

## Interactions

- Invoke print endpoints on user action.
- PDF: open/download via browser print dialog; ensure `Cache-Control: no-store` respected.
- ESC/POS: send bytes to connected printer API; handle device-specific printing outside scope of spec.
- Retry on transient network errors; surface notices if provided.

---

## Error handling

Map HTTP status and canonical codes to UX messages:

- 404 `sales.print_ticket.not_found` → "Ticket introuvable"
- 403 `sales.print_ticket.forbidden` → "Accès refusé"
- 409 `sales.print_ticket.state_conflict` → "Impression non autorisée pour ce ticket"
- 422 `sales.print_ticket.invalid` (si applicable) → "Requête invalide"

Display ProblemDetail fields when appropriate:

- title/detail for general context

---

## Privacy & masking

- Do not render sensitive PII in UI print previews.
- Outlet details may be masked/partial per policy.

---

## Performance

- Avoid prefetching print data; fetch on demand.
- Keep payload sizes small; streaming where possible.

---

## Accessibility

- Provide clear labels for print actions.
- Ensure keyboard navigation to print controls.

---

## Exclusions

- Debug-only endpoint `/api/v1/tenant/tickets/{ticket_id}/print` MUST NOT be surfaced in UI.

---

## Examples

Actions wiring:

- Print PDF: GET `/api/v1/tenant/tickets/tick_01J.../print.pdf`
- Print ESC/POS: GET `/api/v1/tenant/tickets/tick_01J.../print.escpos`
