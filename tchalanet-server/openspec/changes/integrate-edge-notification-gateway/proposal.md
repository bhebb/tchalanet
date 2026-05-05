# Change: integrate-edge-notification-gateway

## Why

`tchalanet-server` needs a clean, secure, reusable integration with `tchalanet-edge-service` for delivery providers such as Slack, Brevo and Twilio.

The edge service is already able to receive notification requests and deliver to Slack and Brevo. The server now needs one canonical gateway adapter instead of ad-hoc HTTP calls or domain-specific delivery code.

This change also finalizes the lightweight batch technical notification path so schedulers can report useful failures through the notification gateway without polluting scheduler methods.

## Decision

Spring Boot remains the source of truth for business decisions, permissions, tenant/RLS, audit, tickets, draws and settlement.

`edge-service` is only a delivery/integration layer.

```text
Spring Boot validates, authorizes, prepares and audits.
Edge-service delivers, formats, routes and integrates.
```

## Scope

This change adds/refines:

- `NotificationGatewayPort` integration to edge-service.
- `EdgeNotificationGatewayAdapter`.
- `EdgeNotificationProperties`.
- `EdgeHmacSigner`.
- Edge HTTP DTO aligned with the already-tested edge endpoint.
- HMAC headers for `/internal/*` edge calls.
- Request ID and idempotency-key propagation.
- Batch technical notification cleanup:
  - `BatchEventNotificationService` as thin orchestrator.
  - `BatchNotificationPolicy` for should-send and cooldown.
  - `BatchNotificationCacheSpecProvider` for dedup TTLs.

## Out of scope

This change does not implement:

- Web/mobile notification use cases.
- Admin notification APIs.
- Draw result fetched email detail flow.
- Sales reports.
- Client ticket SMS/email flows.
- Notification outbox/retry persistence.

Those are handled in separate changes.

## Edge contract

The server adapter must send the same contract already validated manually with `tchalanet-edge-service`:

```json
{
  "eventId": "evt_...",
  "severity": "INFO",
  "title": "Test",
  "message": "ok",
  "recipients": [
    {
      "channel": "SLACK",
      "channelKey": "batch-draws"
    }
  ],
  "context": {}
}
```

The server must not leak internal typed-id objects directly into the edge DTO. Tenant/user IDs must be serialized as strings when needed.

## Config

Target properties:

```yaml
tch:
  notification:
    edge:
      enabled: true
      base-url: 'http://localhost:3000'
      notifications-path: '/internal/notifications/send'
      hmac-secret: '${EDGE_INTERNAL_HMAC_SECRET}'
      connect-timeout: 2s
      read-timeout: 5s
```

Rename existing Node naming:

```text
NodeNotificationGatewayAdapter      -> EdgeNotificationGatewayAdapter
NodeNotificationConfigProperties    -> EdgeNotificationProperties
tch.notification.node.*             -> tch.notification.edge.*
```

## HMAC

All server-to-edge internal calls must include:

```text
X-Request-Id: <requestId>
Idempotency-Key: <idempotencyKey>
X-Tch-Timestamp: <ISO instant>
X-Tch-Signature: sha256=<hex hmac>
Content-Type: application/json
```

Signature payload:

```text
payloadToSign = timestamp + "." + rawJsonBody
signature = HMAC_SHA256(secret, payloadToSign)
```

The adapter must serialize the body to JSON before signing so the signature matches exactly what is sent.

## Batch technical notifications

This change refines existing batch notification integration.

Architecture:

```text
@BatchScheduledJob
  -> BatchScheduledJobAspect
  -> BatchEventNotificationService
  -> BatchNotificationPolicy
  -> NotificationGatewayPort
  -> EdgeNotificationGatewayAdapter
  -> tchalanet-edge-service
```

Target package:

```text
common.batch.notification/
  BatchEventNotificationService
  BatchNotificationPolicy
  BatchNotificationCacheSpecProvider
  BatchNotification
  BatchNotificationStatus
```

Rules:

```text
STARTED   -> never notify
SUCCEEDED -> never notify
SKIPPED   -> notify only when code == gate_disabled, subject to cooldown
FAILED    -> notify, subject to cooldown
```

Normal scheduler ticks must not generate Slack/email noise.

Default cooldown:

```text
30 minutes
```

Cooldown fingerprint:

```text
jobKey + tenantId/GLOBAL + status + code/none
```

If cache is unavailable, the policy should allow sending rather than hiding important failures.

## Acceptance criteria

- `tchalanet-server` can send one Slack notification through edge-service using `NotificationGatewayPort`.
- `tchalanet-server` can send one email notification through edge-service using the same gateway contract.
- Edge calls include HMAC headers.
- The edge DTO matches the tested `/internal/notifications/send` JSON shape.
- `STARTED` and `SUCCEEDED` batch events are silent.
- `FAILED` batch events are sent with cooldown.
- `SKIPPED gate_disabled` batch events are sent with cooldown.
- Missing tenant context does not break global/platform jobs.
- Tests cover adapter payload mapping, HMAC signing and batch policy decisions.
