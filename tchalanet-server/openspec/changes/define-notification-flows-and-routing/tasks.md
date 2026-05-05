# Tasks: define-notification-flows-and-routing

## 1. Properties

- [ ] Add `NotificationFlowProperties` or equivalent under `tch.notification.flows`.
- [ ] Add draw lifecycle properties.
- [ ] Add draw results properties with watched providers/slots.
- [ ] Add apply properties.
- [ ] Add settlement properties.
- [ ] Add sales report properties disabled by default.
- [ ] Add client delivery properties disabled by default.
- [ ] Ensure dev/local profile can enable verbose Slack INFO and watched email detail.
- [ ] Ensure prod defaults are quiet for successful frequent events.

## 2. Notification taxonomy

- [ ] Add/refine notification types:
  - [ ] `BATCH_MESSAGE`
  - [ ] `DRAW_GENERATION_COMPLETED`
  - [ ] `DRAW_GENERATION_FAILED`
  - [ ] `DRAW_OPEN_COMPLETED`
  - [ ] `DRAW_OPEN_FAILED`
  - [ ] `DRAW_CLOSE_COMPLETED`
  - [ ] `DRAW_CLOSE_FAILED`
  - [ ] `DRAW_RESULT_FETCHED`
  - [ ] `DRAW_RESULT_FETCH_FAILED`
  - [ ] `DRAW_RESULT_APPLIED`
  - [ ] `DRAW_RESULT_APPLY_FAILED`
  - [ ] `DRAW_RESULT_APPLY_NO_CANDIDATE`
  - [ ] `DRAW_SETTLEMENT_COMPLETED`
  - [ ] `DRAW_SETTLEMENT_FAILED`
  - [ ] `SALES_SESSION_CLOSED`
  - [ ] `SALES_DAILY_REPORT`
  - [ ] `SALES_ANOMALY`
  - [ ] `DELIVERY_FAILED`
  - [ ] `TICKET_SOLD` future
  - [ ] `TICKET_WON` future
- [ ] Add/refine severity mapping: `INFO`, `WARN`, `ERROR`, `CRITICAL`.

## 3. Router

- [ ] Add `NotificationFlowRouter` in `core.notification.application.flow`.
- [ ] Router handles supported domain events and builds `SendNotificationCommand`.
- [ ] Router checks global enabled flag.
- [ ] Router checks flow-specific enabled flags.
- [ ] Router applies watched provider/slot filters for draw results.
- [ ] Router avoids INFO sends unless enabled.
- [ ] Router sends WARN/ERROR according to config.

## 4. Listeners

- [ ] Add listeners in notification module, not in producer domains.
- [ ] Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for business events.
- [ ] Add draw result listener for fetched/applied events when events exist.
- [ ] Add draw lifecycle listener for generation/open/close/settlement events when events exist.
- [ ] Add sales listener skeleton for session/daily/anomaly events if events exist.
- [ ] If some producer events do not exist yet, document TODOs instead of adding fake coupling.

## 5. Draw result fetched notifications

- [ ] On watched `DRAW_RESULT_FETCHED`, send Slack summary to `batch-draws` when enabled.
- [ ] On watched `DRAW_RESULT_FETCHED`, send detailed email when enabled.
- [ ] Include source details:
  - [ ] provider
  - [ ] slotKey
  - [ ] drawDate
  - [ ] occurredAt
  - [ ] pick3
  - [ ] pick4
  - [ ] sourceUrl if available
  - [ ] sourceHash
- [ ] Include Haiti projection details:
  - [ ] rule
  - [ ] projected values
  - [ ] status
- [ ] Include debug details:
  - [ ] requestId
  - [ ] eventId
  - [ ] duration if available

## 6. Draw lifecycle notifications

- [ ] Generation completed sends Slack INFO only if `draw-lifecycle.slack-info-enabled=true`.
- [ ] Open completed sends Slack INFO only if enabled.
- [ ] Close completed sends Slack INFO only if enabled.
- [ ] Failures send Slack ERROR according to config.
- [ ] Do not send email for normal lifecycle completed events by default.

## 7. Apply notifications

- [ ] Apply completed sends Slack INFO only if enabled.
- [ ] Apply no-candidate sends Slack WARN when enabled.
- [ ] Apply failed sends Slack ERROR.
- [ ] Optional email on warning/failure controlled by config.

## 8. Settlement notifications

- [ ] Settlement completed sends Slack INFO only if enabled.
- [ ] Settlement failed sends Slack ERROR.
- [ ] Prepare admin email summary model but keep disabled by default.

## 9. Sales report skeleton

- [ ] Define sales daily report notification context shape.
- [ ] Define sales session closed notification context shape.
- [ ] Keep sales report sending disabled by default.
- [ ] Do not implement client notification sending in this change.

## 10. Tests

- [ ] Unit test watched provider/slot filtering.
- [ ] Unit test draw result fetched produces Slack command.
- [ ] Unit test draw result fetched produces email command only when enabled.
- [ ] Unit test draw lifecycle completed is silent when info disabled.
- [ ] Unit test apply no-candidate routes WARN.
- [ ] Unit test prod-like config does not spam successful frequent events.

## 11. Documentation

- [ ] Document routing matrix.
- [ ] Document dev config vs prod config.
- [ ] Document no automation in `#général` by default.
- [ ] Document notification flow ownership: producer owns event, notification module owns listener.
