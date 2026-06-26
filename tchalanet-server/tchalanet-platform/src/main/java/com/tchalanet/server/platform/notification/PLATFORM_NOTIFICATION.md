
# Platform Capability `platform.notification` — In-app Notification Center

## Status

**NORMATIVE — V0 target**


## Rôle

`platform.notification` gère les notifications applicatives persistantes (in-app) :
- notification center, unread count, mark read, dismiss, read-all
- ciblage par audience explicite ou dynamique
- publications, republications, replay recipients, cancel et purge douce
- traductions in-app et résolution de langue
- création de notifications à partir d’events métier ou système
- idempotence des déclencheurs automatiques via `notification_trigger_log`

Ce module ne livre pas d’email, SMS, Slack, WhatsApp ou push. Les canaux externes sont demandés par politique de livraison et traités par `platform.communication` après publication.


## Surface API

- `NotificationApi` (Java) : création, lecture, gestion des notifications
- Modèles : `CreateNotificationRequest`, `NotificationView`, etc.

## Intégration

- Les modules publient des events métier/système
- `platform.notification` écoute les events et applique les règles de ciblage
- Les publications in-app peuvent émettre `NotificationPublishedEvent` après commit
- `platform.communication` écoute cet événement pour les canaux externes
- Personne ne doit écrire directement dans les tables notification

## Règles et limitations

- `notification_recipient` et `notification_user_state` sont in-app only
- Les tentatives provider et statuts de livraison externes sont dans `platform.communication`
- Les notifications système utilisent des clés i18n stables et des variables
- Les annonces manuelles stockent des traductions `fr`, `en`, `ht`

## Module organization

```text
platform/notification/
  api/
    NotificationApi.java
    model/
      CreateNotificationRequest.java
      NotificationId.java
      NotificationTarget.java
      NotificationTargetType.java
      NotificationType.java
      NotificationSeverity.java
      NotificationView.java
      NotificationTemplateKey.java

  internal/
    service/
      NotificationService.java
      NotificationTriggerService.java
      NotificationTargetResolver.java
      NotificationTemplateRenderer.java
    rule/
      NotificationRule.java
      TenantLifecycleNotificationRule.java
      SellerTerminalLifecycleNotificationRule.java
      OpsResourceNotificationRule.java
      DrawResultSettlementNotificationRule.java
      SupportContactNotificationRule.java
      CacheMaintenanceNotificationRule.java
    event/
      NotificationDomainEventRouter.java
      BatchAlertNotificationListener.java
    persistence/
      NotificationJpaEntity.java
      NotificationRepository.java
      NotificationTemplateJpaEntity.java
    web/
      TenantNotificationController.java
      AdminNotificationController.java
      PlatformNotificationOpsController.java
    mapper/
    config/
```

## Public API

```java
package com.tchalanet.server.platform.notification.api;

public interface NotificationApi {
    NotificationId create(CreateNotificationRequest request);
    void markRead(NotificationId notificationId, UserId userId);
    void dismiss(NotificationId notificationId, UserId userId);
    int unreadCount(NotificationTarget target);
}
```

The API is for other modules that explicitly need to create an in-app notification. The preferred path remains event-driven listeners.

## Data model

V0 separates the logical message from publication and actor state:

- `notification`: logical message, scope, audience, source, severity, action route and lifecycle status.
- `notification_translation`: manual `fr`/`en`/`ht` translations.
- `notification_publication`: each publish or republish instance.
- `notification_recipient`: explicit in-app recipients.
- `notification_user_state`: per-actor state for dynamic broadcasts.
- `notification_delivery_policy`: requested channels such as `IN_APP`, `EMAIL`, `SMS`, `WHATSAPP`, `SLACK`.
- `notification_trigger_log`: idempotence guard for automatic triggers.

`notification_delivery` does not belong to this capability. External delivery attempts are represented by communication tables such as `outbound_message` and `message_delivery_attempt`.

### Conceptual `notification`

```sql
CREATE TABLE notification (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  source_event_id uuid NULL,
  type varchar(96) NOT NULL,
  severity varchar(32) NOT NULL,
  scope varchar(32) NOT NULL,
  audience_type varchar(64) NOT NULL,
  status varchar(32) NOT NULL,
  title_key varchar(180) NULL,
  message_key varchar(180) NULL,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  action_route varchar(512) NULL,
  expires_at timestamptz NULL,
  cancelled_at timestamptz NULL,
  purged_at timestamptz NULL,
  occurred_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL
);
```

### `notification_recipient` / `notification_user_state`

```sql
CREATE TABLE notification_recipient (
  id uuid PRIMARY KEY,
  publication_id uuid NOT NULL,
  actor_id uuid NULL,
  seller_terminal_id uuid NULL,
  read_at timestamptz NULL,
  dismissed_at timestamptz NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (publication_id, actor_id, seller_terminal_id)
);
```

Dynamic broadcasts use `notification_user_state` with the same per-actor `read_at` and `dismissed_at` semantics.

### `notification_trigger_log`

```sql
CREATE TABLE notification_trigger_log (
  id uuid PRIMARY KEY,
  trigger_key varchar(180) NOT NULL,
  source_type varchar(120) NOT NULL,
  source_id varchar(180) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (trigger_key, source_type, source_id)
);
```

## Event contract

`platform.notification` consumes:

1. Domain events from `core.<domain>.api.event` or the agreed public event package.
2. System events from `common.batch.alert`, e.g. `BatchFailedEvent`.
3. Optional platform events, e.g. tenant/user/config events.

Events should expose at least:

```text
eventId
occurredAt
tenantId when tenant-scoped
actorId when actor-sensitive
correlation/request id when useful
stable business IDs
facts needed for notification rendering
```

A domain event MAY implement `NotifiableDomainEvent`, but it is not required for MVP. Typed rules may adapt specific event classes.

## Normalized intent

Every consumed event is converted to one or more `NotificationIntent` records before persistence.

```java
public record NotificationIntent(
    EventId sourceEventId,
    Instant occurredAt,
    TenantId tenantId,
    NotificationTemplateKey templateKey,
    NotificationSeverity severity,
    NotificationTarget target,
    Map<String, Object> variables,
    String correlationKey
) {}
```

## Idempotency

Every event listener must be idempotent.

Automatic triggers must use a stable triplet:

```text
trigger_key
source_type
source_id
```

Examples:

```text
tenant.onboarding_incomplete / tenant / {tenantId}
seller_terminal.pin_reset / seller_terminal / {terminalId}
ops.job_failed / ops_job_execution / {executionId}
draw_result.missing / draw / {drawId}
support.contact_received / contact_request / {requestId}
cache.cleared / cache_operation / {operationId}
```

`NotificationTriggerService` writes `notification_trigger_log` before publication and skips duplicate triplets. Trigger logs are retained longer than inbox rows to preserve the idempotence window.

## HTTP API

### Tenant admin

```http
GET    /admin/notifications
GET    /admin/notifications/unread-count
POST   /admin/notifications/{notificationId}/read
POST   /admin/notifications/{notificationId}/dismiss
POST   /admin/notifications/read-all
POST   /admin/notifications/announcements
```

### Platform

```http
GET    /platform/notifications
GET    /platform/notifications/unread-count
POST   /platform/notifications/{notificationId}/read
POST   /platform/notifications/{notificationId}/dismiss
POST   /platform/notifications/read-all
POST   /platform/notifications/announcements
POST   /platform/tenants/{tenantId}/notifications/announcements
POST   /platform/notifications/{notificationId}/publish
POST   /platform/notifications/{notificationId}/republish
POST   /platform/notifications/{notificationId}/replay-recipients
POST   /platform/notifications/{notificationId}/cancel
POST   /platform/notifications/purge
```

Controllers remain thin: mapping + validation + bus/api call + security/audit.

Lifecycle endpoints are superadmin/system only where sensitive. Cancel and purge require a reason and audit.

## Lifecycle and retention

- Click notification: set `read_at = now()` for the actor, then navigate to `actionRoute` when present.
- Dismiss: set `dismissed_at = now()` and set `read_at = now()` if it is still null.
- User delete: forbidden; an actor can hide only their own inbox state.
- Cancel: superadmin/system only, reason required, audit required.
- Republish: create a new `notification_publication`; old actor states remain untouched.
- Replay recipients: add missing recipients only and never reset read/dismiss state.
- Purge: soft purge V0 with `dryRun`; eligible rows include read, dismissed, expired and cancelled notifications according to retention windows.

Recommended defaults:

- read/dismissed actor state: 30 days.
- expired/cancelled notification records: soft purge after 30 days.
- purged notification records: optional hard delete after 90 days in dev/stage.
- trigger logs: keep at least 90 days.

## Notices vs notifications

`ApiResponse.notices` are not notifications.

```text
ApiNotice:
  - only in the current HTTP response
  - not persisted
  - not pushed
  - for immediate warnings/partial success/service degradation

platform.notification:
  - persisted
  - visible later
  - read/unread/dismiss
  - never sends external provider messages directly
```

## Templates and targets

System notification wording is resolved through `i18n_override`, not edited from the notification center. Manual announcements persist `fr`, `en` and `ht` rows in `notification_translation`.

Resolution order:

1. current actor locale;
2. `fr`;
3. first available translation.

Supported audiences are:

| Audience | Meaning |
|---|---|
| `SPECIFIC_ACTORS` | Explicit app users or terminals |
| `PLATFORM_ADMINS` | Platform operator audience |
| `ALL_APP_USERS` | Dynamic app-user broadcast |
| `TENANT_ADMINS` | Tenant administration audience |
| `TENANT_APP_USERS` | Tenant app-user audience |
| `TENANT_SELLER_TERMINALS` | Tenant terminal audience |

Audiences describe who should see the in-app item. They do not imply email, SMS, Slack, WhatsApp or push delivery.

## Event cases handled by notification

| Event family | Example events | Default targets | Default severity |
|---|---|---|---|
| Payout | `PayoutRequestedEvent`, `PayoutRejectedEvent`, `PayoutPaidEvent`, `PayoutReversedEvent` | tenant admin, payout manager, cashier | INFO/WARN/ERROR |
| Offline sync | `OfflineSubmissionRejectedEvent`, `OfflineSyncBatchPartiallyAcceptedEvent`, `DeviceSequenceMismatchEvent` | seller, tenant admin, platform ops | WARN/ERROR |
| Tenant user | `TenantUserInvitedEvent`, `TenantUserRoleChangedEvent`, `TenantUserDisabledEvent` | user, tenant admin | INFO/WARN |
| Tenant lifecycle | `TenantCreatedEvent`, `TenantActivatedEvent`, `TenantSuspendedEvent`, `TenantProvisioningFailedEvent` | tenant admin, platform ops | INFO/WARN/ERROR |
| Terminal/outlet/session | `TerminalBlockedEvent`, `OutletSuspendedEvent`, `SessionClosedWithAnomaliesEvent` | cashier, tenant admin | WARN |
| Draw/result ops | `DrawResultFetchFailedEvent`, `DrawResultAppliedEvent`, `DrawResultCorrectedEvent` | platform ops, tenant admin when impact visible | INFO/WARN/ERROR |
| Batch/system | `BatchFailedEvent`, `SchedulerTickFailedEvent` | platform ops | ERROR |
| Cache ops | `CacheClearedEvent`, `CacheClearAllRequestedEvent`, `CacheWarmupFailedEvent` | platform ops only by default | INFO/WARN/ERROR |

## Communication bridge

`NotificationPublishedEvent` is emitted after commit and contains notification/publication IDs, scope, audience, delivery channels and occurrence time.

The communication side:

- ignores `IN_APP`;
- resolves external destinations inside `platform.communication`;
- creates `outbound_message` rows for external channels;
- uses stable idempotence keys such as `notification:{publicationId}:{channel}:{destination}`;
- must not block in-app publication when provider delivery later fails;
- must not write notification-owned delivery attempts.

## Rules

- `platform.notification` may depend on `common`, `catalog api` and relevant public event/model contracts.
- `platform.notification` must not depend on `core.internal` or `features`.
- `platform.notification` must not send email/SMS/WhatsApp/Slack directly.
- `platform.notification` must not call provider adapters or write external delivery attempts.
- The notification-to-communication collaboration goes through after-commit publication events and the communication rule pipeline.
- Notification templates are not PageTemplates.
- PageTemplates remain for UI/page model structure.
