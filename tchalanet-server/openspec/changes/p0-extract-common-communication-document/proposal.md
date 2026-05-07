# Change: P0 — Extract common communication/document boundaries in tchalanet-server

## Priority

P0

## Why

The sales and notification areas started to absorb too many responsibilities:

- `core.sales` was becoming responsible for ticket lifecycle, ticket receipt rendering, printing, and delivery orchestration.
- `core.notification` contains notification-center concepts, but also edge-service outbound transport classes.
- Receipt/document rendering and external communication are reusable by multiple domains, but placing full workflows in `common` would turn `common` into a fourre-tout.
- `features.cashier` and `features.receipt` must not depend directly on each other.

We need a clean boundary that keeps:

- business truth in `core`;
- UI/BFF orchestration in `features`;
- technical reusable primitives in `common`;
- provider transport in `edge-service`.

## What

Introduce and/or normalize:

- `common.communication`

  - generic outbound message primitives;
  - edge-service gateway adapter;
  - HMAC signing for internal edge-service calls;
  - no notification-center concepts.

- `common.document`
  - generic receipt/document rendering primitives;
  - PDF, QR, ESC/POS technical renderers;
  - no ticket/payout/draw business rules.

Refactor:

- `core.notification`

  - remains the in-app notification center;
  - owns `NotificationFlowRouter`, notification command routing, read/unread/action links, approval/limit/system notifications;
  - maps external notification channels to `common.communication`.

- `core.sales`

  - owns the canonical ticket receipt/read model;
  - keeps ticket-specific formatting/model assembly outside `common`.

- `features.cashier`

  - orchestrates sell + optional receipt + optional external delivery;
  - does not call `features.receipt`.

- `features.receipt`
  - owns print/download/preview endpoints;
  - uses `core.sales` receipt query + `common.document`;
  - does not own ticket lifecycle.

## Impact

- Package renames and import updates.
- Edge transport naming changes from notification to communication inside server.
- HMAC signing behavior must be preserved exactly.
- No web/mobile direct call to edge-service.
- Existing Slack/email draw-result notification behavior must remain unchanged.
