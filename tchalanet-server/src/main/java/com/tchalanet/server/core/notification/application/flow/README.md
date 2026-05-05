# Notification Flow Router

## Purpose

Routes domain events to appropriate notification channels (Slack, Email).

## Design

See: `openspec/changes/define-notification-flows-and-routing/IMPLEMENTATION.md`

## Files to Create

1. **NotificationFlowRouter.java** - Main routing logic
2. **DrawNotificationListener.java** - AFTER_COMMIT event listener

## Key Responsibilities

- Check if flow is enabled via config
- Filter by watched providers/slots (NY/FL only)
- Build SendNotificationCommand for each route
- Respect noise control rules (INFO opt-in, WARN/ERROR by default)

## TODO

Manual completion required due to technical file creation issues.
See IMPLEMENTATION.md for complete design and code templates.
