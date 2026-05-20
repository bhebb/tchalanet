# Design: create-edge-communication-services

## Overview

This change introduces the first communication capability for `tchalanet-edge-service`.

The design must stay simple, modular, and provider-agnostic.

Initial supported channels:

```text
SLACK
EMAIL
SMS
```

Future channel:

```text
WHATSAPP
```

WhatsApp appears in the type model only so the contract does not need a major rewrite later, but no WhatsApp sender is implemented in this change.

## Responsibility split

### Spring Boot

Spring Boot decides:

- whether a notification should be sent
- which tenant/user/event it belongs to
- whether action is authorized
- whether it should be audited
- which recipients/channels should be used

### Edge Service

Edge-service does:

- request validation
- provider selection
- message formatting
- provider calls
- provider-level error mapping
- technical delivery result

Edge-service does not:

- read Tchalanet business tables
- decide permissions
- settle tickets
- calculate official outcomes
- own audit métier

## Module architecture

```text
modules/notifications/
  domain/
    notification-channel.ts
    notification-severity.ts
    notification-message.ts
  application/
    send-notification.service.ts
  ports/
    notification-sender.port.ts
  adapters/
    slack/
    email/
    sms/
  http/
    notification.routes.ts
    notification.schemas.ts
```

### domain

Contains TypeScript types for the notification model.

### application

Contains orchestration logic. It does not know provider details.

### ports

Contains interfaces implemented by provider adapters.

### adapters

Contains provider-specific code:

- Slack Incoming Webhooks
- Brevo email
- Twilio SMS

### http

Contains Fastify route and JSON Schema.

## Request contract

```ts
export type NotificationChannel = 'SLACK' | 'EMAIL' | 'SMS' | 'WHATSAPP';
export type NotificationSeverity = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';

export interface NotificationRecipient {
  channel: NotificationChannel;
  to?: string;
  channelKey?: string;
}

export interface SendNotificationRequest {
  eventId: string;
  tenantCode?: string;
  severity: NotificationSeverity;
  title: string;
  message: string;
  recipients: NotificationRecipient[];
  context?: Record<string, unknown>;
}

export interface SendNotificationResponse {
  accepted: boolean;
  eventId: string;
  deliveries: NotificationDeliveryResult[];
}
```

## Delivery result

Each recipient receives an independent result.

```ts
export interface NotificationDeliveryResult {
  channel: NotificationChannel;
  to?: string;
  channelKey?: string;
  accepted: boolean;
  reason?: string;
}
```

The service should not fail the entire notification because one recipient failed.

Example:

```json
{
  "accepted": true,
  "eventId": "evt_001",
  "deliveries": [
    { "channel": "SLACK", "channelKey": "batch-draws", "accepted": true },
    {
      "channel": "EMAIL",
      "to": "admin@example.com",
      "accepted": false,
      "reason": "EMAIL_PROVIDER_NOT_CONFIGURED"
    }
  ]
}
```

## Slack design

MVP uses Slack Incoming Webhooks.

Channel key mapping:

```text
tchalanet       -> SLACK_WEBHOOK_TCHALANET
batch-draws     -> SLACK_WEBHOOK_BATCH_DRAWS
delivery        -> SLACK_WEBHOOK_DELIVERY
ops-alerts      -> SLACK_WEBHOOK_OPS_ALERTS
security-audit  -> SLACK_WEBHOOK_SECURITY_AUDIT
```

Recommended routing later:

```text
draw jobs       -> batch-draws
delivery issues -> delivery
infra issues    -> ops-alerts
security/audit  -> security-audit
release summary -> tchalanet
```

Slack message format should be readable and compact.

Example:

```text
[ERROR] draw:results:fetch failed

Tenant: GLOBAL
Job: draw:results:fetch
Slot: NY_MID
Reason: Provider timeout
Event: evt_001
```

Do not log webhook URLs.

## Email design

MVP uses Brevo.

Email content:

- subject: `[SEVERITY] title`
- HTML body with title, message, eventId, tenantCode
- simple text fallback if easy

No attachments in this change.

Attachments belong to the future `delivery` module, not basic notifications.

## SMS design

MVP supports Twilio adapter shape.

SMS content must be short.

Recommended max:

```text
320 characters
```

SMS should be used only for urgent or explicit recipients.

No attachments in SMS.

## Disabled provider behavior

Provider disabled or missing config must not crash the service.

Examples:

```text
SLACK_DISABLED
SLACK_WEBHOOK_NOT_CONFIGURED:batch-draws
EMAIL_DISABLED
EMAIL_PROVIDER_NOT_CONFIGURED
SMS_DISABLED
SMS_PROVIDER_NOT_CONFIGURED
```

These are returned as delivery results.

## Route design

Route:

```text
POST /internal/notifications/send
```

Status:

```text
202 Accepted
```

Reason:

The request may be accepted even if one delivery fails. The response body tells which deliveries succeeded or failed.

HMAC auth is not implemented in this change unless already available, but the route path is internal and must be ready to be protected later.

## Validation

Use Fastify JSON Schema.

TypeScript types are compile-time only. JSON Schema validates runtime payloads from Spring Boot or local curl tests.

## Testing strategy

Automated tests must not call real Slack/Brevo/Twilio.

Use:

- fake sender implementing `NotificationSender`
- Fastify `inject()` for route tests
- invalid payload tests

Manual tests can call real providers using `.env.local`.

## Future changes

Follow-up changes:

1. Add HMAC auth to `/internal/*`
2. Add Redis idempotency and anti-spam
3. Add `delivery` module for tickets and attachments
4. Add templates module
5. Add rules/routing module
6. Add feature management helper/cache
7. Add WhatsApp sender
8. Add delivery status webhooks
