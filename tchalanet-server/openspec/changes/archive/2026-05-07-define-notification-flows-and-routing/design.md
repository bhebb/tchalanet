# Design: define-notification-flows-and-routing

## Design principle

The notification module is the consumer and router of domain events.

Producer bounded contexts should not know Slack/Brevo/Twilio and should not contain notification routing decisions.

```text
Producer owns event class.
Notification module owns listener and routing.
```

Examples:

```text
core.drawresult.domain.event.DrawResultFetchedEvent
core.notification.application.listener.DrawResultNotificationListener

core.draw.domain.event.DrawSettledEvent
core.notification.application.listener.DrawLifecycleNotificationListener
```

## Entry points

There are two clean entry points:

```text
1. Technical batch statuses
   @BatchScheduledJob -> aspect -> BatchEventNotificationService

2. Business events
   DomainEvent -> AFTER_COMMIT notification listener -> NotificationFlowRouter
```

## Why AFTER_COMMIT

Business notifications must not be sent if the transaction rolls back.

Examples:

```text
DrawResultFetchedEvent -> send after draw_result is committed
DrawResultAppliedEvent -> send after draw is updated
DrawSettledEvent -> send after settlement is committed
TicketSoldEvent -> send after ticket is committed
```

## Router responsibilities

`NotificationFlowRouter` owns:

- Flow enabled checks.
- Provider/slot watch filters.
- Severity choice.
- Channel choice.
- Recipient choice.
- Context building.
- Template key or title/message construction.
- Delegation to `SendNotificationCommand`.

It does not own provider HTTP calls.

## Noise control

Default behavior must be quiet.

```text
Normal tick success -> no notification
Started -> no notification
Succeeded -> no notification
Repeated failures -> cooldown
Repeated gate disabled -> cooldown
Successful lifecycle INFO -> dev/staging only unless explicitly enabled
Detailed email -> watched flows only
```

## Routing matrix

| Flow                                 |           Slack |      Email dev |     Email admin tenant |           Client |
| ------------------------------------ | --------------: | -------------: | ---------------------: | ---------------: |
| draw:generation.completed            | INFO if enabled |             no |                     no |               no |
| draw:generation.failed               |           ERROR |       optional |                     no |               no |
| draw:open.completed                  | INFO if enabled |             no |                     no |               no |
| draw:open.failed                     |           ERROR |       optional |                     no |               no |
| draw:close.completed                 | INFO if enabled |             no |                     no |               no |
| draw:close.failed                    |           ERROR |       optional |                     no |               no |
| draw:results:fetch.completed watched |            INFO | yes if enabled |                     no |               no |
| draw:results:fetch.failed            |           ERROR |   yes optional |                     no |               no |
| draw:results:apply.completed         | INFO if enabled |             no |                     no |               no |
| draw:results:apply.no_candidate      |            WARN |   yes optional |                     no |               no |
| draw:results:apply.failed            |           ERROR |   yes optional |                     no |               no |
| draw:settlement.completed            | INFO if enabled |             no |                 future |               no |
| draw:settlement.failed               |           ERROR |       optional |              if impact |               no |
| sales:daily-report                   |          no/dev |             no |             future yes |               no |
| ticket:sold                          |              no |             no |                     no | future SMS/email |
| ticket:won                           |              no |             no | dashboard/admin future | future SMS/email |
| delivery:failed                      |      WARN/ERROR |       optional |           case-by-case |               no |

## Email for draw result fetched

The detailed email is specifically useful during development to verify source and Haiti projection.

Initial watched scope:

```text
NY_MID
NY_EVE
FL_MID
FL_EVE
```

This limits noise while validating two provider integrations.

## Slack general channel

`#général` must not receive automation by default.

Use:

```text
#batch-draws     -> draw/batch/results/settlement
#delivery        -> delivery failures
#ops-alerts      -> infra/system
#security-audit  -> force/security actions
#tchalanet       -> occasional global summaries
```

## Future client notifications

Client notifications are out of scope here.

They must be opt-in/policy-aware and triggered by business commands, not generic sends.

Examples:

```text
POST /tickets/{ticketId}/send
POST /tickets/{ticketId}/resend
```

## Future sales reports

Sales reports are business admin notifications.

Prepare shape now, keep disabled:

```text
sales:daily-report
sales:session-closed
sales:anomaly
```
