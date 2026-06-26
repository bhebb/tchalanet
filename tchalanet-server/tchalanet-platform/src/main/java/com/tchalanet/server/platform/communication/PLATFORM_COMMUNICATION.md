# PLATFORM_COMMUNICATION — External delivery: email, SMS, WhatsApp, Slack, push

## Status

**NORMATIVE — V0 target**


## Rôle

`platform.communication` gère la livraison externe :
- email
- SMS
- WhatsApp
- alertes Slack internes
- webhooks Slack tenant (optionnel)
- push provider (à venir)
- templates de messages
- gestion des tentatives, retry/backoff, adaptateurs providers

Ce module ne prend pas de décision métier : il délivre les intentions produites par events ou API explicite.

enqueue is the normal path.
sendNow is the controlled exception.

`platform.communication` is also the only owner of external delivery requested by notification publications. `platform.notification` owns the inbox; communication owns provider-facing delivery.

## Surface API

- `CommunicationApi` (Java) :
  - `enqueue(SendOutboundMessageRequest)` — chemin normal (asynchrone, traçable)
  - `sendNow(SendOutboundMessageRequest)` — exception contrôlée (synchrone, à éviter)

## Intégration

- Les modules platform/core publient des intentions de communication via `enqueue`
- `NotificationPublishedEvent` peut produire des messages externes quand une politique demande `EMAIL`, `SMS`, `WHATSAPP` ou `SLACK`
- `sendNow` réservé aux cas d’exception (ne pas bloquer les transactions métier)
- Les tentatives, statuts et erreurs sont tracés

## Règles et limitations

- Jamais de logique métier dans ce module
- Les providers peuvent être lents/indisponibles : retry/backoff obligatoire
- Les livraisons sont asynchrones par défaut
- Les templates sont gérés côté platform
- `IN_APP` est ignoré par communication et reste dans `platform.notification`
- Les lignes `notification_recipient` et `notification_user_state` ne sont jamais des statuts de livraison provider

## Module organization

```text
platform/communication/
  api/
    CommunicationApi.java
    model/
      SendMessageRequest.java
      MessageId.java
      MessageChannel.java
      MessageRecipient.java
      MessageRecipientType.java
      MessageTemplateKey.java
      MessagePriority.java
      MessageSendResult.java
      DeliveryStatus.java
      CommunicationIntent.java

  internal/
    service/
      CommunicationService.java
      MessageRenderingService.java
      DeliveryPolicyResolver.java
      OutboundMessageDispatcher.java
      DeliveryRetryPlanner.java
    rule/
      CommunicationRule.java
      NotificationPublishedCommunicationRule.java
      BatchAlertCommunicationRule.java
      OpsCommunicationRule.java
    event/
      CommunicationDomainEventRouter.java
      BatchAlertCommunicationListener.java
    adapter/
      email/
        EmailProviderAdapter.java
      sms/
        SmsProviderAdapter.java
      slack/
        SlackProviderAdapter.java
      whatsapp/
        WhatsappProviderAdapter.java
      push/
        PushProviderAdapter.java
    persistence/
      OutboundMessageJpaEntity.java
      MessageDeliveryAttemptJpaEntity.java
      MessageTemplateJpaEntity.java
      CommunicationSettingsJpaEntity.java
    scheduler/
      OutboundMessageRetryScheduler.java
    web/
      PlatformCommunicationOpsController.java
      AdminCommunicationSettingsController.java
      AdminMessageTemplateController.java
    mapper/
    config/
```

## Public API

```java
package com.tchalanet.server.platform.communication.api;

public interface CommunicationApi {
    MessageId enqueue(SendMessageRequest request);
    MessageSendResult sendNow(SendMessageRequest request);
}
```

Optional specialized API for ops may be added if needed:

```java
public interface OpsCommunicationApi {
    MessageId enqueueOpsAlert(OpsAlertRequest request);
}
```

If an ops alert must survive a rollback, the implementation must use a transaction independent from the failing business transaction.

## Data model

### `outbound_message`

```sql
CREATE TABLE outbound_message (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  source_event_id uuid NULL,
  channel varchar(32) NOT NULL,
  recipient_type varchar(32) NOT NULL,
  recipient_value varchar(255) NOT NULL,
  template_key varchar(120) NOT NULL,
  locale varchar(20) NULL,
  subject varchar(255) NULL,
  body text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  priority varchar(32) NOT NULL,
  status varchar(32) NOT NULL,
  correlation_key varchar(180) NULL,
  next_attempt_at timestamptz NULL,
  sent_at timestamptz NULL,
  failed_at timestamptz NULL,
  failure_reason text NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  version bigint NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_outbound_message_correlation
  ON outbound_message (tenant_id, correlation_key)
  WHERE correlation_key IS NOT NULL;

CREATE INDEX idx_outbound_message_pending
  ON outbound_message (status, next_attempt_at, priority);
```

### `message_delivery_attempt`

```sql
CREATE TABLE message_delivery_attempt (
  id uuid PRIMARY KEY,
  message_id uuid NOT NULL REFERENCES outbound_message(id),
  attempted_at timestamptz NOT NULL,
  status varchar(32) NOT NULL,
  provider varchar(80) NOT NULL,
  provider_message_id varchar(255) NULL,
  error_code varchar(120) NULL,
  error_message text NULL
);
```

### `message_template`

```sql
CREATE TABLE message_template (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  template_key varchar(120) NOT NULL,
  channel varchar(32) NOT NULL,
  locale varchar(20) NOT NULL,
  subject_template text NULL,
  body_template text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NULL,
  UNIQUE (tenant_id, template_key, channel, locale)
);
```

### `tenant_communication_settings`

```sql
CREATE TABLE tenant_communication_settings (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  email_enabled boolean NOT NULL DEFAULT true,
  sms_enabled boolean NOT NULL DEFAULT false,
  tenant_slack_enabled boolean NOT NULL DEFAULT false,
  tenant_slack_webhook_secret_ref varchar(255) NULL,
  critical_alert_email varchar(255) NULL,
  ops_alert_email varchar(255) NULL,
  default_locale varchar(20) NOT NULL DEFAULT 'fr',
  quiet_hours_start time NULL,
  quiet_hours_end time NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NULL,
  UNIQUE (tenant_id)
);
```

## Event contract

`platform.communication` consumes explicit communication requests and supported events, including notification publication events for external channels.

Every rule produces normalized `CommunicationIntent`:

```java
public record CommunicationIntent(
    EventId sourceEventId,
    Instant occurredAt,
    TenantId tenantId,
    MessageChannel channel,
    MessageRecipient recipient,
    MessageTemplateKey templateKey,
    MessagePriority priority,
    Map<String, Object> variables,
    String correlationKey
) {}
```

## Channel rules

```text
EMAIL:
  invitation, onboarding, action required, security, important receipt/summary.

SMS:
  customer-facing short receipts, OTP, urgent payout/ticket messages, opt-in required.

WHATSAPP:
  tenant/customer opt-in only. Usually receipt/help flows. Never default for broad seller-terminal fanout until recipient lookup and consent are explicit.

SLACK_INTERNAL:
  Tchalanet/platform ops only: failures, fraud, provider down, provisioning failed, cache clear all.

SLACK_TENANT_WEBHOOK:
  tenant opt-in only. Never default.

PUSH:
  later, usually notification wake-up. Source of truth remains notification center.
```

## Notification publication bridge

`platform.notification` emits `NotificationPublishedEvent` after commit when a notification publication is created or republished.

The communication bridge rule:

- ignores `IN_APP`;
- maps `EMAIL`, `SMS`, `WHATSAPP` and `SLACK` to outbound messages;
- resolves destination addresses, phone numbers, Slack targets and future provider destinations inside the communication boundary;
- batches large audiences when necessary;
- writes `outbound_message` through `CommunicationApi.enqueue(...)`;
- uses stable correlation keys: `notification:{publicationId}:{channel}:{destination}`;
- never writes notification-owned inbox state;
- never blocks the committed in-app notification when dispatch or provider delivery later fails.

No external message is created if the notification transaction rolls back, because the bridge runs after commit.

Seller-terminal SMS/WhatsApp fanout requires a public recipient lookup and opt-in model before broad activation. Until then, seller-terminal notifications stay in-app unless an explicit destination is available.

## Batch alert refactor

The old batch/Slack integration must be replaced.

### Wrong

```text
common.batch or core scheduler -> SlackGateway.send(...)
```

### Correct

```text
common.batch.alert
  -> publishes BatchFailedEvent provider-neutral

platform.communication
  -> listens to BatchFailedEvent
  -> creates CommunicationIntent
  -> enqueue outbound_message
  -> dispatcher sends through SlackAdapter internal
```

If the annotation/aspect lives in `common`, it must not import `platform.communication.api`, `MessageChannel`, `MessageTemplateKey`, or any Slack class.

### Common annotation shape

```java
package com.tchalanet.server.common.batch.alert;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotifyOnBatchFailure {
    String alertKey() default "batch.failed";
    BatchAlertSeverity severity() default BatchAlertSeverity.ERROR;
    boolean includeStackTrace() default false;
}
```

### Common event shape

```java
package com.tchalanet.server.common.batch.alert;

public record BatchFailedEvent(
    EventId eventId,
    Instant occurredAt,
    String jobName,
    String stepName,
    String executionId,
    BatchAlertSeverity severity,
    String reason,
    Map<String, Object> facts,
    String correlationKey
) implements SystemEvent {}
```

### Communication mapping

```text
BatchFailedEvent(alertKey=batch.failed)
  -> SLACK_INTERNAL
  -> template ops.batch.failed
  -> priority HIGH
  -> correlationKey batch:{jobName}:{executionId}:failed
```

## HTTP error and notices

`platform.communication` may add `ApiResponse.notices` only for immediate HTTP feedback, for example:

```text
- test Slack succeeded with warnings;
- provider test degraded;
- message accepted but channel disabled;
- template fallback used.
```

Errors remain `ProblemDetail` and are not wrapped.

Communication failures during async delivery are stored in `outbound_message` and `message_delivery_attempt`; they are not returned as HTTP notices unless the request was a `sendNow`/test endpoint.

## Idempotency

Every outbound message that comes from an event must have a stable `correlationKey`.

Examples:

```text
notification:{publicationId}:email:{email}
notification:{publicationId}:sms:{phone}
notification:{publicationId}:whatsapp:{phone}
notification:{publicationId}:slack:{workspaceOrChannel}
ticket:{ticketId}:receipt:sms
payout:{payoutId}:paid:email
payout:{payoutId}:paid:sms
offline-sync:{batchId}:rejected:tenant-email
batch:{jobName}:{executionId}:failed
cache:{operationId}:clear-all:ops-slack
tenant:{tenantId}:admin:{userId}:invited:email
```

## Rules

- Provider adapters are internal only.
- No module outside `platform.communication.internal` may import Slack/email/SMS/WhatsApp provider classes.
- `enqueue` is the default for all event-driven and automatic communication.
- `sendNow` is only for ops tests/diagnostics.
- Tenant Slack is opt-in.
- Internal Slack is for Tchalanet/platform ops.
- Message templates are not PageTemplates.
- `common` never contains message templates, channels, providers or communication delivery logic.
- Notification publication bridge code must live in `platform.communication`, not in notification services/controllers.
