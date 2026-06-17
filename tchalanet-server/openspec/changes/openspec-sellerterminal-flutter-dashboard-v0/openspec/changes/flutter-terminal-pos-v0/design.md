# Design: Flutter Terminal POS V0

## Client Strategy

```text
Admin = Angular web / SSR-compatible admin
Seller = Flutter Android POS
Backend = shared, client-neutral APIs
```

Flutter is not allowed to own business rules. It only collects input, displays server decisions, and prints receipts.

## Authentication

SellerTerminal login uses Firebase technical user credentials.

User-facing form:

```text
tenant code
terminal code
PIN/password
```

App maps terminal code/PIN to Firebase login convention or calls a backend helper if required.

After Firebase login:

```text
Firebase ID token -> backend -> resolves SellerTerminal actor
```

## Screens

Minimal V0:

```text
Login
Terminal home/status
Sale entry
Sale preview
Sale confirmation
Receipt/print
Recent tickets
Reprint
Blocked/disabled terminal notice
```

No V0 screens for:

```text
admin stats
odds config
limits config
results
payout management
billing
```

## Sale Flow

```text
1. Seller selects draw.
2. Seller selects game/bet type.
3. Seller enters selection and stake.
4. App asks backend for preview.
5. Backend validates odds/limits/cutoff/terminal status.
6. Seller confirms.
7. Backend creates ticket.
8. App prints receipt payload.
```

## Receipt Payload

Backend returns a client-neutral receipt model:

```json
{
  "ticketId": "...",
  "publicCode": "...",
  "soldAt": "...",
  "terminalCode": "...",
  "sellerDisplayName": "...",
  "lines": [],
  "totalStake": "100.00",
  "receiptText": "...",
  "qrPayload": "...",
  "htmlReceipt": "..."
}
```

Flutter uses `receiptText` or structured fields for thermal printing.

## Printing

V0 printing strategy:

- implement Android printer adapter behind a Flutter abstraction;
- keep backend payload neutral;
- support reprint for own recent tickets;
- mark receipt as duplicate on reprint if required.

## Local Storage

Use secure storage for Firebase/session token only.

Do not store offline sale drafts as valid sales in V0.

## Error UX

Must clearly show:

```text
terminal blocked
terminal disabled
token expired
draw closed
cutoff passed
limit exceeded
odds unavailable
network error
```

## API Contracts

```http
GET  /tenant/terminal/me
POST /tenant/sales/preview
POST /tenant/sales/confirm
GET  /tenant/terminal/tickets/recent
POST /tenant/terminal/tickets/{id}/reprint
```

The exact route names may align to existing `/tenant/tickets` conventions, but the backend use cases must remain client-neutral.
