# Change: create-server-notification-usecases

## Why

After the server can call edge-service, Tchalanet needs controlled server-side notification use cases for ops, admin, web and mobile flows.

Web/mobile must not be allowed to send arbitrary notifications to arbitrary recipients. They should call business endpoints such as ticket delivery or admin announcement. Spring Boot decides permissions, template, channel, recipient, idempotency and audit.

## Decision

Notifications are a transverse capability, but not a free-form public API.

```text
Mobile/Web requests a business action.
Spring Boot validates and decides.
Notification module sends through Edge.
```

## Scope

This change creates/refines the server-side notification application layer:

- `SendNotificationCommand`.
- `SendNotificationCommandHandler`.
- `NotificationPolicy`.
- Notification recipient/target model.
- Ops test endpoint for Slack/email/SMS.
- Admin-controlled notification endpoint skeleton if needed.
- No free client “send anything” endpoint.

## Out of scope

This change does not implement:

- Full ticket delivery.
- Client ticket sold/won notification flows.
- Sales reports.
- Draw result fetched routing.
- Notification outbox/retry table.
- UI notification inbox persistence.

Those can be added later.

## Target architecture

```text
core.notification
  application/
    command/
      SendNotificationCommand
      SendNotificationCommandHandler
    policy/
      NotificationPolicy
    mapper/
      NotificationPayloadMapper
  domain/
    NotificationType
    NotificationSeverity
    NotificationChannel
    NotificationRecipient
    NotificationTarget
  infra/
    edge/
      EdgeNotificationGatewayAdapter
    web/
      OpsNotificationController
      AdminNotificationController
```

`common.notification` may contain only gateway contracts used by transverse infrastructure.

## Command shape

Target command:

```java
public record SendNotificationCommand(
    NotificationType type,
    NotificationSeverity severity,
    List<NotificationRecipient> recipients,
    Locale locale,
    String title,
    String message,
    Map<String, Object> context,
    String idempotencyKey,
    String reason
) {}
```

## Recipient model

The model must support different channel addressing styles:

```text
SLACK -> channelKey
EMAIL -> to email
SMS   -> to phone
WEB   -> tenant/user target later
```

A flexible model is acceptable:

```java
public record NotificationRecipient(
    NotificationChannel channel,
    String to,
    String channelKey,
    TenantId tenantId,
    UserId userId
) {}
```

The edge adapter must map only what edge needs.

## Ops test endpoint

Add a superadmin-only endpoint for manual testing:

```http
POST /api/v1/ops/notifications/test
```

Example payload:

```json
{
  "channel": "SLACK",
  "channelKey": "batch-draws",
  "severity": "INFO",
  "title": "Tchalanet server -> edge test",
  "message": "Spring Boot successfully called edge-service."
}
```

Email:

```json
{
  "channel": "EMAIL",
  "to": "admin@example.com",
  "severity": "INFO",
  "title": "Tchalanet email test",
  "message": "Email sent from Spring Boot through edge-service."
}
```

SMS:

```json
{
  "channel": "SMS",
  "to": "+15145550000",
  "severity": "INFO",
  "title": "Tchalanet SMS test",
  "message": "SMS sent from Spring Boot through edge-service."
}
```

Protect with:

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

## No generic client send endpoint

Do not expose a broad endpoint like:

```http
POST /api/v1/notifications/send
```

for web/mobile users.

Instead, future web/mobile flows must use business-specific endpoints:

```http
POST /api/v1/tickets/{ticketId}/send
POST /api/v1/tickets/{ticketId}/resend
POST /api/v1/sales/sessions/{sessionId}/report/send
```

These endpoints validate business ownership and then call notification commands.

## Backup/fallback

For MVP, if edge-service is unavailable:

- Log WARN/ERROR with requestId/idempotencyKey.
- Return a clear failure result for direct/manual operations.
- Do not add a DB outbox in this change unless already available.

Future change:

```text
notification-outbox-retry
```

## Acceptance criteria

- Superadmin can test Slack from Spring Boot through edge-service.
- Superadmin can test email from Spring Boot through edge-service.
- Superadmin can test SMS from Spring Boot through edge-service if Twilio config exists.
- Web/mobile users do not get a generic arbitrary send endpoint.
- Notification command validates recipients by channel.
- Notification command produces an idempotency key.
- Handler delegates to `NotificationGatewayPort` only after policy validation.
