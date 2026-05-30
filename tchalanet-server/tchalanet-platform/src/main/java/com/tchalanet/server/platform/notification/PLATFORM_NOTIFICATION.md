
# Platform Capability `platform.notification` — In-app Notification Center

## Status

**NORMATIVE — proposed**


## Rôle

`platform.notification` gère les notifications applicatives persistantes (in-app) :
- notification center, unread count, mark read/archive
- ciblage user/role/outlet/tenant/platform
- templates in-app, préférences de notification
- création de notifications à partir d’events métier ou système

Ce module ne livre pas d’email, SMS, Slack ou push (voir `platform.communication`).


## Surface API

- `NotificationApi` (Java) : création, lecture, gestion des notifications
- Modèles : `CreateNotificationRequest`, `NotificationView`, etc.

## Intégration

- Les modules publient des events métier/système
- `platform.notification` écoute les events et applique les règles de ciblage
- Personne ne doit écrire directement dans la table notification

## Règles et limitations

- Les notifications externes sont gérées par `platform.communication`
- Les préférences et templates sont internes à ce module

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
      NotificationPreferenceView.java
      NotificationTemplateKey.java

  internal/
    service/
      NotificationService.java
      NotificationTargetResolver.java
      NotificationPreferenceResolver.java
      NotificationTemplateRenderer.java
    rule/
      NotificationRule.java
      PayoutNotificationRule.java
      OfflineSyncNotificationRule.java
      TenantUserNotificationRule.java
      TenantNotificationRule.java
      BatchAlertNotificationRule.java
      OpsNotificationRule.java
    event/
      NotificationDomainEventRouter.java
      BatchAlertNotificationListener.java
    persistence/
      NotificationJpaEntity.java
      NotificationRepository.java
      NotificationPreferenceJpaEntity.java
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
    void archive(NotificationId notificationId, UserId userId);
    int unreadCount(NotificationTarget target);
}
```

The API is for other modules that explicitly need to create an in-app notification. The preferred path remains event-driven listeners.

## Data model

### `notification`

```sql
CREATE TABLE notification (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  source_event_id uuid NULL,
  type varchar(96) NOT NULL,
  severity varchar(32) NOT NULL,
  target_type varchar(32) NOT NULL,
  target_user_id uuid NULL,
  target_role varchar(64) NULL,
  target_outlet_id uuid NULL,
  title varchar(180) NOT NULL,
  message text NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  read_at timestamptz NULL,
  archived_at timestamptz NULL,
  occurred_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL
);

CREATE INDEX idx_notification_tenant_target
  ON notification (tenant_id, target_type, target_user_id, target_role, target_outlet_id);

CREATE INDEX idx_notification_unread
  ON notification (tenant_id, read_at)
  WHERE read_at IS NULL;
```

### `notification_preference`

```sql
CREATE TABLE notification_preference (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  user_id uuid NULL,
  role_key varchar(64) NULL,
  notification_type varchar(96) NOT NULL,
  in_app_enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NULL,
  UNIQUE (tenant_id, user_id, role_key, notification_type)
);
```

### `notification_template`

In-app templates live here. External message templates live in `platform.communication`.

```sql
CREATE TABLE notification_template (
  id uuid PRIMARY KEY,
  tenant_id uuid NULL,
  template_key varchar(120) NOT NULL,
  locale varchar(20) NOT NULL,
  title_template text NOT NULL,
  body_template text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NULL,
  UNIQUE (tenant_id, template_key, locale)
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

Recommended handler key format:

```text
notification.<source>.<event-or-rule>
```

Examples:

```text
notification.payout.requested
notification.payout.rejected
notification.offlinesync.submission_rejected
notification.batch.failed
notification.tenant_user.role_changed
```

Use `processed_event` or a unique `(tenant_id, source_event_id, target_type, target_user_id, type)` constraint when appropriate.

## HTTP API

### Tenant user

```http
GET    /tenant/me/notifications
GET    /tenant/me/notifications/unread-count
POST   /tenant/me/notifications/{notificationId}/read
POST   /tenant/me/notifications/{notificationId}/archive
```

### Tenant admin

```http
GET    /admin/notifications
GET    /admin/notification-preferences
PUT    /admin/notification-preferences/{id}
```

### Platform ops

```http
GET    /platform/ops/notifications
POST   /platform/ops/notifications/test
```

Controllers remain thin: mapping + validation + bus/api call + security/audit.

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
  - read/unread/archive
  - never sends external provider messages directly
```

## Templates and targets

Notification templates are persisted in `notification_template` and are used only for in-app
rendering. A template is selected by `(tenant_id, template_key, locale)`, with tenant-specific
templates preferred over global templates when both exist.

Supported persisted targets are:

| Target | Meaning |
|---|---|
| `USER` | One concrete user notification center |
| `ROLE` | Users currently acting under a role such as tenant admin |
| `OUTLET` | Outlet-scoped operational/audit visibility |
| `TENANT` | Tenant-wide notification center or admin feed |
| `PLATFORM` | Platform ops feed |

Targets describe who should see the in-app item. They do not imply email, SMS, Slack or push
delivery. External delivery belongs to `platform.communication`, either from a separate listener on
the same source event or from a future explicit collaboration rule.

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

## Rules

- `platform.notification` may depend on `common`, `catalog api` and relevant public event/model contracts.
- `platform.notification` must not depend on `core.internal` or `features`.
- `platform.notification` must not send email/SMS/Slack directly.
- `platform.notification` must not call `platform.communication.api` in V1. Preferred: both capabilities listen to the same source event independently.
- Notification templates are not PageTemplates.
- PageTemplates remain for UI/page model structure.
