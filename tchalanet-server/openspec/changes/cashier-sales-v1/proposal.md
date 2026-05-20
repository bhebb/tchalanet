# Proposal — Cashier Features & Sales Ticket Receipt V1

## Status

Proposed for implementation.

This change stabilizes the V1 cashier/POS ticket journey and removes architectural drift between `features.cashier` and `core.sales`.

## Goal

Deliver a working V1 borlette POS flow where a seller can:

1. operate inside a trusted POS context;
2. preview a basket before selling;
3. modify a basket when limits/rules block it;
4. sell a ticket with idempotency protection;
5. deliver proof to the customer by print, system-send, or manual share;
6. cancel a ticket within a short post-sale window;
7. avoid `features.cashier` importing `core.sales.internal.*`.

The goal is a pragmatic V1, not a full future-proof platform.

---

## Architectural decisions

```text
features.cashier         = POS journey / BFF, no business invariants
core.sales               = ticket truth + official ticket receipt formatter
platform.document        = technical PDF / ESC-POS / QR rendering
platform.communication   = technical SMS / WhatsApp / Email delivery
```

### Non-negotiable rules

- The official ticket receipt belongs to `core.sales`.
- `platform.document` must not know about tickets.
- `platform.communication` must not know about ticket business rules.
- `features.cashier` must not import `core.sales.internal.*`.
- `features.cashier` orchestrates through `CommandBus`, `QueryBus`, `DocumentApi`, and `CommunicationApi` only.
- `core.sales.api` exposes the commands, queries, and read models needed by cashier/admin/support.

---

## User flow decisions

### Cashier POS flow

```text
1. Seller has a trusted operational context.
2. Seller selects an available draw.
3. Seller enters numbers and stakes.
4. Seller previews the basket.
5. Backend returns ACCEPTABLE, REQUIRES_CHANGES, or REJECTED_FINAL.
6. Seller edits basket if needed.
7. Seller sells the accepted basket.
8. Backend returns an accepted sale with backup proof.
9. Seller can print, send, copy code, copy link, or copy full message.
```

### Sell and delivery are separate

`POST /tenant/cashier/tickets/sell` creates the official ticket.

After an accepted sale, proof can be delivered through:

- print;
- system SMS / WhatsApp / Email;
- manual share by seller using the canonical `shareableText`;
- verbal/transcribed backup code.

Print/send failures do not cancel or invalidate the sale.

---

## Preview decision

`preview` is read-only and best-effort. It does not lock or reserve exposure.

The preview response must include a warning when acceptable:

```json
{
  "decision": "ACCEPTABLE",
  "warning": "Ce résultat est indicatif. D'autres ventes en cours peuvent modifier les limites disponibles."
}
```

If sell later returns `REJECTED` with `EXPOSURE_CHANGED`, that is valid behavior. The seller adjusts the basket. The operational rule is: cash should not be finalized before the sell response is `ACCEPTED`; if cash was already taken, it must be refunded immediately.

Reservation/hold is deferred to V2.

---

## Decisions and rejection classes

### `REQUIRES_CHANGES`

The basket can be modified and re-previewed to become acceptable.

Examples:

- stake too high;
- exposure remaining is lower than requested;
- invalid selection format;
- approval required for POS but reducible by lowering stake;
- basket line count or total stake constraints.

### `REJECTED_FINAL`

No basket change can make the sale acceptable in the current operational window.

Examples:

- draw cutoff exceeded;
- draw closed;
- session closed;
- terminal blocked;
- outlet suspended;
- tenant disabled;
- untrusted or missing POS operational context.

---

## Pending approval decision

`PENDING_APPROVAL` remains a valid core sales outcome for non-POS channels such as `WEB` or `PARTNER_API`.

However, cashier POS V1 does not expose pending approval to the seller. In cashier POS, any approval-required decision is surfaced as `REQUIRES_CHANGES` with issue code `APPROVAL_REQUIRED`.

Rationale: at a physical counter, holding the customer/cash while waiting for an admin is operationally toxic. The seller should reduce the stake, change the basket, or refuse the sale.

---

## Backup proof decision

Every accepted sale must return a `backup` block:

```json
{
  "displayCode": "PSGV-4AXJ",
  "verificationShortUrl": "app.tchalanet.com/ticket/PSGV-4AXJ",
  "shareableText": "Ticket Tchalanet valide\nCode: PSGV-4AXJ\n...",
  "primaryInstruction": "Votre code est PSGV-4AXJ.",
  "verificationInstruction": "Vérifier sur app.tchalanet.com/ticket/PSGV-4AXJ"
}
```

This is the fallback when printer/system-send fails.

The seller can copy:

- only the code;
- only the verification link;
- the full shareable message.

Manual share and system-send use the same text content. System-send is audited and billed; manual share is not audited in V1.

---

## QR decision

V1 QR payload is the plain customer-facing verification URL:

```text
app.tchalanet.com/ticket/PSGV-4AXJ
```

No HMAC signing in V1. Signed/offline verification QR is deferred to V2.

---

## Money decision

- Store money as `BigDecimal` with scale 2.
- Serialize all money in cashier/receipt API responses as strings: `"5.00 HTG"`.
- Store odds as `BigDecimal` scale 4.
- Potential payout = stake × odds, rounded `HALF_UP` to scale 2.

---

## Communication decision

V1 system-send sends text only. No PDF attachments. No file storage. No expiring signed URLs.

`platform.communication` deduplicates send requests by `(ticketId, channel, recipient)` within 60 seconds. The caller does not provide an `Idempotency-Key` for send.

The send metadata must include enough stable fields for deduplication:

```text
ticketId
publicCode
channel
recipient
dedupKey
```

---

## Cancel decision

Cashier can cancel a ticket within a configurable post-sale window. V1 default: 3 minutes.

Cancel after the window returns `409 CANCEL_WINDOW_EXPIRED`.

Cancel must release/reverse the ticket exposure through the same exposure ledger mechanism used by sales. Cancel must be idempotent and must not leave exposure consumed.

---

## Public code decision

Customer-facing verification accepts both dashed and non-dashed codes.

Examples:

```text
PSGV-4AXJ
PSGV4AXJ
psgv-4axj
```

The backend normalizes by removing separators and comparing case-insensitively.

---

## Non-goals V1

- HMAC-signed QR payloads.
- PDF attachments on SMS/WhatsApp/email.
- Reservation/hold on preview.
- Cashier POS pending approval workflow.
- Redis-backed distributed send dedup.
- Full i18n for seller instructions.
- Cashier-initiated document storage/download endpoints.
- Complete tenant admin receipt/report feature.
