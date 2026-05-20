# Design: integrate-edge-notification-gateway

## Context

`tchalanet-edge-service` is a delivery service. It already supports sending notifications to Slack and Brevo through `/internal/notifications/send`.

`tchalanet-server` needs one canonical gateway so domains do not call Slack, Brevo or Twilio directly.

## Component boundary

```text
common.notification
  NotificationGatewayPort
  model/
    SendNotificationPayload
    NotificationRecipient / target model if already present

core.notification.infra.edge
  EdgeNotificationGatewayAdapter
  EdgeNotificationProperties
  EdgeHmacSigner
  EdgeNotificationRequest
  EdgeNotificationRecipient

common.batch.notification
  BatchEventNotificationService
  BatchNotificationPolicy
  BatchNotificationCacheSpecProvider
```

`common.notification` may contain generic contracts only. Business use cases belong in `core.notification`, not `common`.

## Adapter flow

```text
SendNotificationPayload
  -> EdgeNotificationGatewayAdapter
  -> EdgeNotificationRequest JSON
  -> EdgeHmacSigner
  -> RestClient POST /internal/notifications/send
```

## Edge request shape

```java
record EdgeNotificationRequest(
    String eventId,
    String severity,
    String title,
    String message,
    List<EdgeNotificationRecipient> recipients,
    Map<String, Object> context
) {}

record EdgeNotificationRecipient(
    String channel,
    String to,
    String channelKey
) {}
```

## Typed IDs

Do not pass `TenantId` or `UserId` records directly to the edge JSON contract.

Use:

```java
tenantId == null ? null : tenantId.value().toString()
```

Same for user IDs.

## HMAC design

The exact JSON string must be signed.

```text
rawJsonBody = objectMapper.writeValueAsString(request)
timestamp = clock.instant().toString()
payloadToSign = timestamp + "." + rawJsonBody
signature = HMAC_SHA256(secret, payloadToSign)
```

HTTP body must be `rawJsonBody`, not a separately serialized object.

## Batch notification design

Batch notifications are technical operational notifications. They are not business notifications.

They are used to detect important scheduler and batch issues without producing noise.

### Rules

```text
STARTED   -> never notify
SUCCEEDED -> never notify
SKIPPED   -> notify only when code == gate_disabled
FAILED    -> notify
```

All sent notifications are subject to cooldown.

Default cooldown:

```text
30 minutes
```

The cooldown fingerprint is:

```text
jobKey + tenantId/GLOBAL + status + code/none
```

This avoids repeating the same Slack/email notification every minute when a scheduler keeps failing or a gate remains disabled.

### Components

```text
BatchScheduledJobAspect
  -> catches lifecycle of annotated scheduled jobs

BatchEventNotificationService
  -> builds BatchNotification
  -> resolves context
  -> delegates decision to policy
  -> sends through NotificationGatewayPort

BatchNotificationPolicy
  -> owns shouldSend()
  -> owns cooldown
  -> owns fingerprint

BatchNotificationCacheSpecProvider
  -> declares infra.notification.batch_dedup cache
```

### Context handling

Tenant context may not exist for global/platform jobs.

Rules:

```text
tenantId missing -> GLOBAL
requestId missing -> generated UUID
```

The service must not fail if `TchContext.current()` is unavailable.

### Cache behavior

If the cache is unavailable or not registered, the policy allows sending.

This keeps notification delivery best-effort and avoids hiding important failures because of cache misconfiguration.

### Multi-instance note

The MVP cache-based cooldown is acceptable.

In multi-instance production, duplicate sends may occur if two nodes process the same failure concurrently. A future improvement may use Redis atomic `SET NX EX` semantics for stronger deduplication.

## Non-goals

- No business routing logic here.
- No draw/sales notification flow decisions here.
- No web/mobile endpoint here.
- No notification database/outbox here.
