# Change: add-notification-communication-event-mapping

## Why

The project needs an explicit matrix of events that create notifications and/or external messages so agents do not invent delivery logic in core domains.

## What changes

- Add mapping from domain/system events to notification and communication intents.
- Define default channel choices for tenant admins, platform ops, tenant Slack, SMS and email.
- Define HTTP notice/error distinction.

## Impact

- Documentation only initially.
- Later implementation in platform rules.
