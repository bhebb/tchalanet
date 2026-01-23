# OpenSpec — SALES / Get Ticket Detail — Terminal (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/71-domain-sales.md
- openspec/context/40-mobile-rules.md (if terminal UI shares constraints)
- openspec/specs/sales/get-ticket-detail/01-backend.md

---

## Purpose

Provide terminal UI/flows to display ticket details in tenant scope by `ticket_id` and a masked public verify snapshot by `public_code`. Respect rate limiting and privacy constraints for public verify.

---

## Views

### Tenant — Ticket Detail View (terminal)

- Header area: `ticket_code`, `public_code` (if shown), sale/result/settlement badges, `created_at`.
- Amounts: currency, total_amount (prominent, fixed-width for readability).
- Lines list: selection label, odds, stake_amount, line result_status.
- Settlement summary (if settled): concise summary.
- Actions:
  - Print (routes to print flow)
  - Close/Back
  - Request override (visible only if authorized)

### Public — Verify Snapshot (terminal kiosk or operator assisting customer)

- Minimal header: `public_code`, statuses.
- Amounts: currency, total_amount.
- No outlet PII; outlet masked/partial.
- No privileged actions.

---

## Interactions

- Fetch detail (tenant): GET `/api/v1/tenant/tickets/{ticket_id}` with Authorization.
- Fetch public snapshot: GET `/api/v1/public/tickets/{public_code}` (unauthenticated; rate-limited; noindex context).
- Retry on transient network errors with small backoff; show a non-blocking toast or inline message.
- Respect rate limit on public verify; avoid rapid re-requests from the terminal.

---

## Error handling

Map HTTP status and canonical error codes to terminal messages:

- 404 `sales.get_ticket_detail.not_found` → "Ticket introuvable"
- 403 `sales.get_ticket_detail.forbidden` → "Accès refusé"
- 429 `sales.get_ticket_detail.rate_limited` (public) → "Trop de requêtes, veuillez réessayer plus tard"
- 422 `sales.get_ticket_detail.invalid` (rare) → "Requête invalide"

Display ProblemDetail fields when useful:

- `detail` as message body; include `code` for support diagnostics.

---

## Privacy & masking (public verify)

- Do not show sensitive PII on public verify.
- Mask outlet details (partial), avoid tenant-internal identifiers.
- Do not cache public snapshots in ways that could leak PII.

---

## Performance & UX constraints

- Optimize rendering for limited screen sizes and low-powered terminals.
- Use monospace or fixed tabular layout for amounts/odds for readability.
- Avoid excessive network calls; debounce repeated lookups.

---

## Accessibility

- High-contrast status badges and clear text labels.
- Large touch targets for actions (Print, Back).

---

## Example payloads (bindings)

Tenant binding sample:

```json
{
  "ticket_id": "tick_01J2Z8WQ...",
  "ticket_code": "A1B2-C3D4-E5",
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "SOLD",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00",
  "lines": [
    {
      "line_id": "line_01...",
      "selection": "Team A vs Team B — WIN A",
      "odds": "1.80",
      "stake_amount": "20.00",
      "result_status": "NOT_RESULTED"
    }
  ]
}
```

Public verify sample:

```json
{
  "public_code": "PUB-9X2Z7Q4N1M",
  "sale_status": "SOLD",
  "result_status": "NOT_RESULTED",
  "settlement_status": "UNSETTLED",
  "currency": "HTG",
  "total_amount": "35.00"
}
```
