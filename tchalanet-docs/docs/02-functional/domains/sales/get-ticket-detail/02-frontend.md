# OpenSpec — SALES / Get Ticket Detail — Frontend (v1) — NON-NORMATIVE

References:

- openspec/context/10-non-negotiables.md
- openspec/context/30-frontend-rules.md
- openspec/context/71-domain-sales.md
- openspec/specs/sales/get-ticket-detail/01-backend.md

---

## Purpose

Provide UI to view full ticket details (tenant scope) and a masked public verify snapshot by `public_code`. Respect privacy and avoid sensitive PII exposure.

---

## Views

### Tenant — Ticket Detail Screen

- Header: `ticket_code`, `public_code`, sale/result/settlement statuses, `created_at`/`updated_at`.
- Amounts: currency, total_amount.
- Lines list: selection, odds, stake_amount, line result_status.
- Settlement summary (if settled): settlement_status and brief summary.
- Audit summary (if permitted): high-level entries.
- Actions:
  - Print ticket (navigates to print view)
  - Close / Back
  - Request override (visible only if authorized)

### Public — Verify Snapshot

- Minimal header: `public_code`, statuses.
- Amounts: currency, total_amount.
- No outlet PII; outlet masked/partial where applicable.
- No privileged actions.

---

## Data contract

- Tenant screen binds to `TicketDetailResponse` from backend.
- Public screen binds to `PublicTicketDetailResponse` (masked fields).
- Do not duplicate backend logic; backend is source of truth.
- Typed IDs displayed or used as needed; `tenant_id` not shown/used from body.

---

## Interactions

- Fetch on load: GET `/api/v1/tenant/tickets/{ticket_id}` (authorized) or GET `/api/v1/public/tickets/{public_code}`.
- Retry with backoff for transient failures; surface notices[] if provided.
- Respect rate limits on public verify: handle `429` with user-friendly message.

---

## Error handling

Map HTTP status and canonical codes to UX messages:

- 404 `sales.get_ticket_detail.not_found` → "Ticket introuvable"
- 403 `sales.get_ticket_detail.forbidden` → "Accès refusé"
- 429 `sales.get_ticket_detail.rate_limited` (public) → "Trop de requêtes, réessayez plus tard"
- 422 `sales.get_ticket_detail.invalid` (rare) → "Requête invalide"

Display ProblemDetail fields when appropriate:

- title/detail for general context
- violations[] for field-level issues (if any)

---

## Privacy & masking (public verify)

- Do not show sensitive PII.
- Mask outlet details (partial), avoid tenant-specific internal identifiers.
- Ensure pages are marked noindex; avoid exposing via SEO.

---

## Performance

- Lazy-render long lines lists; paginate or virtualize if necessary.
- Cache-safe headers respected; avoid stale sensitive data.

---

## Accessibility

- Status badges with accessible labels; color + text.
- Numeric values readable with locale formatting; currency shown consistently.

---

## Examples (UI data bindings)

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

Public verify binding sample:

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
