# Notification / Communication Event Mapping

## Status

**NORMATIVE — proposed**

## Purpose

This document maps events to:

- in-app notifications handled by `platform.notification`;
- external communication handled by `platform.communication`;
- immediate HTTP notices handled by `ApiResponseBodyAdvice`.

## Decision summary

```text
Domain/System event -> notification/communication policy -> normalized intent -> persistence/enqueue.
HTTP request warning -> ApiResponse.notices only.
HTTP error -> ProblemDetail only.
```

## Event contract

### Required base event metadata

All events consumed by notification/communication must expose or be adaptable to:

```text
eventId
occurredAt
tenantId when tenant-scoped
actorId when actor-sensitive
business IDs needed for correlation
facts needed for template rendering
```

### Optional notifiable contract

```java
public interface NotifiableDomainEvent extends DomainEvent {
    NotificationEventKey notificationKey();
    NotificationSeverity defaultSeverity();
    NotificationAudienceHint audienceHint();
    Map<String, Object> notificationFacts();
}
```

This interface is optional. Typed rules may adapt events without changing the original domain event.

### System events

System events are not domain events. Example:

```java
public interface SystemEvent {
    EventId eventId();
    Instant occurredAt();
}
```

`BatchFailedEvent` is a system event from `common.batch.alert`.

## Normalized platform objects

### NotificationIntent

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

### CommunicationIntent

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

## Mapping matrix — business events

| Source | Event | Notification | Communication | Notes |
|---|---|---|---|---|
| `core.sales` | `TicketSoldEvent` | optional cashier/admin info | SMS/email receipt if customer opted in | correlation `ticket:{id}:receipt:{channel}` |
| `core.sales` | `TicketVoidedEvent` | cashier + tenant admin | email optional if customer identifiable | reason required |
| `core.sales` | `TicketResultedEvent` | cashier/admin if winning or payout pending | customer SMS/email if opt-in and winning | do not notify before result/settlement state is stable |
| `core.sales` | `TicketLimitWarningEvent` | cashier immediate/in-app | none by default | HTTP warning may also be ApiNotice if same request |
| `core.sales` | `OfflineSaleAcceptedEvent` | seller + tenant admin summary | optional email summary | no Slack by default |
| `core.offlinesync` | `OfflineSubmissionRejectedEvent` | seller + tenant admin | tenant admin email; internal Slack if suspicious | high importance |
| `core.offlinesync` | `OfflineSyncBatchPartiallyAcceptedEvent` | seller + tenant admin | email summary optional | include counts accepted/rejected |
| `core.offlinesync` | `DeviceSequenceMismatchEvent` | tenant admin + platform ops | internal Slack mandatory | fraud/security signal |
| `core.offlinesync` | `OfflineSignatureInvalidEvent` | platform ops + tenant admin | internal Slack mandatory | security signal |
| `core.payout` | `PayoutRequestedEvent` | tenant admin / payout manager | email optional | action required |
| `core.payout` | `PayoutApprovedEvent` | cashier/admin | none by default |  |
| `core.payout` | `PayoutPaidEvent` | cashier/admin | customer SMS/email receipt if opt-in | correlation per payout/channel |
| `core.payout` | `PayoutRejectedEvent` | cashier/admin | email optional; internal Slack if high amount | reason required |
| `core.payout` | `PayoutReversedEvent` | tenant admin + platform ops | internal Slack mandatory | sensitive |
| `core.drawresult` | `DrawResultFetchFailedEvent` | platform ops | internal Slack mandatory after retry threshold | provider ops |
| `core.drawresult` | `DrawResultAppliedEvent` | platform ops / tenant admin if visible | none by default | settlement trigger |
| `core.drawresult` | `DrawResultCorrectedEvent` | platform ops + tenant admin | internal Slack mandatory | correction sensitive |
| `core.terminal` | `TerminalBlockedEvent` | cashier + tenant admin | tenant Slack optional |  |
| `core.outlet` | `OutletSuspendedEvent` | seller/cashier + tenant admin | email tenant admin optional |  |
| `core.session` | `SessionClosedWithAnomaliesEvent` | tenant admin | tenant Slack optional; internal Slack if severe |  |
| `platform.identity/usercontext` | `TenantUserInvitedEvent` | none or admin info | invitation email mandatory |  |
| `platform.identity/usercontext` | `TenantUserRoleChangedEvent` | user + tenant admin | email if admin/critical role | internal Slack if super admin/admin sensitive |
| `platform.identity/usercontext` | `TenantUserDisabledEvent` | tenant admin | email optional |  |
| `platform.tenantconfig` | `TenantConfigChangedEvent` | tenant admin | email optional for critical config | audit mandatory |
| `platform.tenanttheme` | `TenantThemeChangedEvent` | tenant admin optional | none |  |
| `platform.tenant` | `TenantCreatedEvent` | platform ops | internal Slack optional | provisioning start |
| `platform.tenant` | `TenantProvisioningCompletedEvent` | tenant owner/admin | email onboarding mandatory |  |
| `platform.tenant` | `TenantProvisioningFailedEvent` | platform ops | internal Slack mandatory |  |
| `platform.tenant` | `TenantSuspendedEvent` | tenant admin + platform ops | email tenant admin; internal Slack optional |  |

## Mapping matrix — system / ops events

| Source | Event | Notification | Communication | Notes |
|---|---|---|---|---|
| `common.batch.alert` | `BatchFailedEvent` | platform ops in-app | internal Slack via enqueue | provider-neutral event |
| `common.batch.alert` | `SchedulerTickFailedEvent` | platform ops in-app | internal Slack via enqueue | optional separate event |
| `platform.cacheops` | `CacheClearedEvent` | platform ops optional | none by default | audit required |
| `platform.cacheops` | `CacheClearAllRequestedEvent` | platform ops | internal Slack optional/mandatory by env | sensitive ops action |
| `platform.cacheops` | `CacheWarmupFailedEvent` | platform ops | internal Slack | if impacts availability |
| `platform.communication` | `MessageDeliveryFailedFinalEvent` | platform ops | internal Slack if critical channel | avoid loops |
| `platform.provider` | `ExternalProviderDownEvent` | platform ops | internal Slack | result provider/search/etc. |

## HTTP notices mapping

`ApiResponse.notices` are returned only with the current HTTP response.

| Scenario | ApiNotice | Persistent notification | Communication |
|---|---|---|---|
| Sale accepted with limit warning | yes | optional if severe/repeated | no |
| Search fallback because service degraded | yes + service status | no | no |
| Template fallback used in test endpoint | yes | no | no |
| Slack test succeeded | yes info | no | no |
| Slack test failed | ProblemDetail or notice depending status | no | no |
| Offline sync rejected asynchronously | no, unless same request returns summary | yes | email/slack based on policy |
| Batch failed asynchronously | no | yes ops | Slack internal |

## HTTP errors

Errors remain `ProblemDetail` and must not be wrapped in `ApiResponse`.

```text
4xx/5xx = ProblemDetail
2xx = ApiResponse<T> with notices/services when needed
```

Examples:

| Error | HTTP | Body | Notification/Communication |
|---|---:|---|---|
| idempotency missing | 400 | ProblemDetail | none |
| payload mismatch | 409 | ProblemDetail | none |
| permission denied | 403 | ProblemDetail | optional security event if repeated/suspicious |
| provider test failed | 502/503 or 200 PARTIAL depending endpoint | ProblemDetail or ApiResponse services | no async delivery |
| batch job failed | not tied to HTTP | n/a | BatchFailedEvent -> notif/com |

## Default channel policy

```text
Tenant admins:
  in-app + email by default for important actions.

Tenant Slack:
  opt-in only.

Internal Slack:
  platform ops, failures, fraud/security, provider down, provisioning failed, cache clear all.

SMS:
  customer-facing, opt-in, short/urgent/receipt/security.

ApiNotice:
  immediate HTTP feedback only.
```

## Review checklist

- [ ] Event has stable eventId and occurredAt.
- [ ] Tenant-scoped event carries tenantId or is processed in explicit context.
- [ ] Notification rule maps event to NotificationIntent.
- [ ] Communication rule maps event to CommunicationIntent if external delivery is required.
- [ ] CommunicationIntent has stable correlationKey.
- [ ] Listener is idempotent.
- [ ] No provider gateway is called outside `platform.communication.internal.adapter`.
- [ ] ApiResponse notices are not used as persistent notifications.
