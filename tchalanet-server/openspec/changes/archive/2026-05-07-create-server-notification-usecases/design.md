# Design: create-server-notification-usecases

## Principle

Notifications are transverse, but sending is policy-driven.

The server should not expose raw provider-like behavior to web/mobile clients.

```text
Bad:
  Web/Mobile -> POST /notifications/send { to, message, channel }

Good:
  Web/Mobile -> POST /tickets/{ticketId}/send
  Spring validates ticket/tenant/permissions
  Spring prepares template/recipient/channel
  Spring calls SendNotificationCommand
```

## Flow: ops test

```text
SUPER_ADMIN
  -> POST /api/v1/ops/notifications/test
  -> SendNotificationCommand
  -> NotificationPolicy
  -> NotificationGatewayPort
  -> EdgeNotificationGatewayAdapter
  -> edge-service
```

## Flow: future ticket delivery

```text
Mobile/Web
  -> POST /api/v1/tickets/{ticketId}/send
  -> SendTicketDeliveryCommand
  -> verifies ticket ownership, tenant and permissions
  -> prepares email/SMS payload
  -> SendNotificationCommand
  -> edge-service
```

## Flow: future admin notification

```text
Tenant admin
  -> POST /api/v1/admin/notifications/send
  -> policy verifies tenant scope and allowed target
  -> SendNotificationCommand
```

## Command handler responsibilities

The handler owns:

- Input validation.
- Policy check.
- Idempotency key creation/propagation.
- Mapping to gateway payload.
- Delegation to gateway.

It does not own provider-specific delivery details.

## Policy responsibilities

Policy owns:

- Channel-specific target validation.
- Allowed sender/use-case checks.
- Future tenant preferences.
- Future consent/opt-in checks.

## Outbox note

An outbox/retry mechanism is valuable but should not be mixed into this initial use-case change unless implementation already exists.

Future design:

```text
SendNotificationCommand
  -> persist notification_attempt PENDING
  -> after commit / worker sends through gateway
  -> update status SENT/FAILED/RETRY
```

For now, direct gateway call is acceptable for dev/ops test and low-risk notifications.
