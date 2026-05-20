# Change: define-notification-flows-and-routing

## Why

Tchalanet needs useful notifications for development, tests, operations and later tenant business reports without creating Slack/email spam.

Seeing notifications everywhere is not manageable. Scheduler ticks can run every minute or every few minutes, and a notification on every normal run would hide the important events.

This change defines intelligent notification flows and routing rules.

## Decision

Notifications are centralized and policy-driven.

Business domains publish events or execute business commands. The notification module listens, decides routing, applies cooldown, formats payloads and sends through the gateway.

```text
Domain event métier
  -> AFTER_COMMIT listener in core.notification
  -> NotificationFlowRouter
  -> SendNotificationCommand
  -> EdgeNotificationGatewayAdapter
  -> edge-service
  -> Slack / Brevo / Twilio
```

For technical batch/scheduler statuses:

```text
@BatchScheduledJob
  -> BatchScheduledJobAspect
  -> BatchEventNotificationService
  -> BatchNotificationPolicy
```

## Scope

This change defines/implements:

- Notification flow taxonomy.
- Routing properties.
- `NotificationFlowRouter`.
- AFTER_COMMIT listeners in `core.notification` for draw/drawresult/sales events.
- Dev-focused draw result notifications for watched providers/slots.
- Slack INFO for generation/open/close only when enabled.
- WARN/ERROR notifications with cooldown.
- Sales report notification model prepared but disabled by default.

## Out of scope

This change does not implement:

- Edge gateway integration details already covered by `integrate-edge-notification-gateway`.
- Ops/admin command APIs covered by `create-server-notification-usecases`.
- Full client delivery flows.
- Web notification inbox persistence.
- Notification outbox/retry persistence.

## Core rules

```text
1. No notification for every successful scheduler tick.
2. No email for frequent normal events.
3. Slack INFO only if explicitly enabled.
4. Slack WARN/ERROR enabled by default.
5. Email detail only for critical flows or watched providers/slots.
6. SMS only for critical/business/client opt-in later.
7. Cooldown is mandatory for repeated FAILED/SKIPPED events.
8. Business event notifications run AFTER_COMMIT.
9. Client notifications use business commands/endpoints, not generic send.
10. Notification routing lives in notification module, not scattered in handlers.
```

## Channels

Recommended Slack routing:

```text
#batch-draws
  draw lifecycle, draw results, settlement

#delivery
  email/SMS/WhatsApp delivery failures

#ops-alerts
  infra/service down, Redis, DB, edge-service, Meili, Unleash

#security-audit
  force actions, overrides, suspicious actions

#tchalanet
  occasional global summaries

#général
  no automation by default
```

## Notification families

```text
1. DEV/OPS notifications
   Slack + rare emails for debugging and operations.

2. BUSINESS ADMIN notifications
   Email/dashboard notification for tenant admins.

3. CLIENT notifications
   Ticket SMS/email/WhatsApp later, opt-in aware.
```

## Draw lifecycle flow

Events/flows:

```text
draw:generation.completed
draw:generation.failed
draw:open.completed
draw:open.failed
draw:close.completed
draw:close.failed
```

Slack INFO may be enabled in dev/staging.

Production default should be quiet for successful lifecycle events.

## Draw result fetch flow

Important for development/testing.

Start with watched providers/slots only:

```text
providers: NY, FL
slots: NY_MID, NY_EVE, FL_MID, FL_EVE
```

On watched successful fetch:

- Slack short summary to `batch-draws`.
- Email detailed source/projection report if `email-detail-enabled=true`.

Email content should include:

```text
Provider
Slot
Draw date
Occurred at
Source Pick3/Pick4
Source URL if available
Source hash
Haiti projection
Projection rule
Status
RequestId/EventId
Fetch duration if available
```

## Apply flow

Events/flows:

```text
draw:results:apply.completed
draw:results:apply.no_candidate
draw:results:apply.failed
```

Routing:

```text
completed -> Slack INFO only if enabled
no_candidate -> Slack WARN + optional email in dev
failed -> Slack ERROR + optional email
```

## Settlement flow

Events/flows:

```text
draw:settlement.completed
draw:settlement.failed
```

Routing:

```text
completed -> Slack INFO in dev/staging if enabled
failed -> Slack ERROR, optional email
```

Future business admin email can include ticket/winner/payout summary.

## Sales reports

Prepare but disable by default:

```text
sales:session-closed
sales:daily-report
sales:anomaly
```

Routing later:

```text
session closed -> dashboard/admin email optional
daily report -> tenant admin email/dashboard
anomaly -> Slack ops/security + tenant admin depending severity
```

## Config

Target properties:

```yaml
tch:
  notification:
    enabled: true

    batch:
      enabled: true
      cooldown: 30m
      notify-started: false
      notify-succeeded: false
      notify-failed: true
      notify-skipped-gate-disabled: true
      slack-channel-key: 'batch-draws'

    flows:
      draw-lifecycle:
        enabled: true
        slack-info-enabled: true
        email-enabled: false

      draw-results:
        enabled: true
        slack-enabled: true
        email-detail-enabled: true
        watched-providers:
          - NY
          - FL
        watched-slots:
          - NY_MID
          - NY_EVE
          - FL_MID
          - FL_EVE

      apply:
        enabled: true
        slack-info-enabled: true
        email-on-warning-enabled: true
        email-on-failure-enabled: true

      settlement:
        enabled: true
        slack-info-enabled: true
        email-admin-enabled: false

      sales-reports:
        enabled: false
        daily-email-enabled: false

      client-delivery:
        enabled: false
        ticket-sold-enabled: false
        ticket-won-enabled: false
```

## Acceptance criteria

- Normal successful scheduler ticks do not send notifications.
- Failures go to Slack with cooldown.
- `gate_disabled` skipped jobs go to Slack with cooldown.
- Watched NY/FL draw result fetch sends Slack summary in dev config.
- Watched NY/FL draw result fetch sends detailed email when enabled.
- Draw generation/open/close successful events send Slack INFO only when enabled.
- Apply `no_candidate` sends WARN when enabled.
- Sales report flow is defined/prepared but disabled by default.
- Notification listeners live in notification module; business handlers are not polluted with provider-specific send calls.
