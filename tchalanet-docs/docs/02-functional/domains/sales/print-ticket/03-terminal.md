# OpenSpec — SALES / Print Ticket — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/print-ticket/01-backend.md

---

## Purpose

Provide terminal/POS flows to print tickets using the normative endpoints: ESC/POS (primary) and PDF (fallback). Respect permissions, privacy, and caching rules; exclude debug-only endpoints.

---

## Views

### Terminal Print Flow

- Entry points:
  - After successful sale
  - On-demand reprint from ticket detail
- Actions:
  - Print ESC/POS → GET `/api/v1/tenant/tickets/{ticket_id}/print.escpos` and stream to printer
  - Print PDF (fallback) → GET `/api/v1/tenant/tickets/{ticket_id}/print.pdf` and open print dialog
- Status indicators: show printing progress, success/failure.

---

## Interactions

- ESC/POS:
  - Fetch bytes; send to printer driver/interface.
  - If printer error: retry (bounded attempts, e.g., 3) and present recovery options (reprint later, save).
- PDF fallback:
  - Use when ESC/POS device unavailable.
- Do not cache prints; respect `Cache-Control: no-store`.
- Reprint allowed for authorized roles only.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 404 `sales.print_ticket.not_found` → "Ticket introuvable"
- 403 `sales.print_ticket.forbidden` → "Accès refusé"
- 409 `sales.print_ticket.state_conflict` → "Impression non autorisée"
- 422 `sales.print_ticket.invalid` → "Requête invalide"

Display ProblemDetail fields when useful:

- `detail` as message body; show specific guidance.

---

## Privacy & masking

- Ensure printed content does not include sensitive PII beyond policy.
- Mask outlet details partially if required.

---

## Performance & UX constraints

- Keep network payload minimal; stream ESC/POS efficiently.
- Provide clear progress and retry options; avoid infinite retries.

---

## Accessibility

- Large, clear buttons for print actions; readable status messages.

---

## Exclusions

- Do not use or expose the debug-only endpoint `/api/v1/tenant/tickets/{ticket_id}/print` in terminal flows.

---

## Examples

Actions wiring:

- ESC/POS: GET `/api/v1/tenant/tickets/tick_01J.../print.escpos`
- PDF: GET `/api/v1/tenant/tickets/tick_01J.../print.pdf`
