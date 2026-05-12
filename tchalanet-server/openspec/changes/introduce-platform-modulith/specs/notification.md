# Spec — Notification Placement

## Decision

Default placement is `platform.notification`, not `core.notification`.

## Rationale

Notifications are normally transversal application support capabilities:

- notification inbox,
- user notification preferences,
- push notification routing,
- delivery state,
- notification templates,
- notification read/unread lifecycle.

They are consumed by multiple domains and do not decide game/money/payout/draw outcomes.

## Split with communication

```text
platform.notification
  = what to notify, notification state, inbox, preferences, routing intent

platform.communication
  = how to send via email/SMS/WhatsApp/push/provider gateways
```

## Core interaction

Core modules publish domain events:

```text
core.payout -> PayoutApprovedEvent
core.sales  -> TicketSoldEvent
core.draw   -> DrawResultAppliedEvent
```

`platform.notification` may listen and create/send notification work.

Core modules must not listen to `platform.notification` events.

## Exception

`core.notification` is allowed only if an ADR proves notification owns a core business-critical invariant.
