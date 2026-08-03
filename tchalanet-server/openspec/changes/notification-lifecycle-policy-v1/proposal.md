# Proposal - notification lifecycle policy v1

## Why

The draw-result path already emits a few useful notices, but draw and ticket lifecycle
documentation does not state which transitions are intentionally silent and which ones require
an operational alert. Without that contract, future listeners can create duplicate or noisy
notifications, and tenant admins cannot rely on the notification center for exceptional events.

## What

- Define one notification matrix for draw and ticket lifecycles.
- Separate tenant operational audiences (`TENANT_OWNER`, `TENANT_ADMIN`) from platform
  supervision (`SUPER_ADMIN`).
- Make normal high-volume transitions silent; their state remains visible in lists, detail pages,
  dashboard and reports.
- Define actionable result and settlement exceptions, with stable deduplication keys and
  after-commit delivery rules.

## Impact

- Affects `core.draw`, `core.drawresult`, `core.sales`, `core.payout`,
  `platform.notification`, and their communication bridge rules.
- This change first establishes the functional contract and records existing behavior. It does
  not add new notification listeners in this slice.

## Non-goals

- Customer, public ticket, seller-terminal push/SMS notifications.
- A notification for every ticket sale, result calculation or payment.
- Changing ticket, payout or draw business state transitions.
