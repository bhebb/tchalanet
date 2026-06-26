# Design

## Context Packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/30-frontend-rules.md`

## Responsibility Boundary

`platform.notification` owns:

- what should be notified;
- who can see it;
- in-app publication lifecycle;
- per-actor `seen_at`, `read_at`, and `dismissed_at`;
- translation resolution;
- idempotent notification triggers.

`platform.communication` owns:

- email, SMS, Slack, and future external delivery;
- provider payloads and destinations;
- delivery attempts, retry, failure status, and provider IDs;
- idempotence for external delivery attempts.

Do not store email/SMS/Slack delivery status in `notification_recipient`.

## Core Tables

- `notification`: logical message, scope, audience, severity, source, action route, lifecycle timestamps.
- `notification_translation`: FR/EN/HT content per notification.
- `notification_publication`: each publish/republish instance.
- `notification_recipient`: explicit fanout recipients for in-app notifications.
- `notification_user_state`: per-actor state for dynamic broadcasts.
- `notification_delivery_policy`: requested channels (`IN_APP`, `EMAIL`, `SMS`, `SLACK`).
- `notification_trigger_log`: idempotence guard for automatic triggers.

Existing `notification_delivery` remains a communication/delivery-attempt concept only if needed during migration; it must not become the inbox state table.

## Scopes

- `TENANT`: `tenant_id` required, visible/manageable only inside that tenant.
- `PLATFORM`: `tenant_id` null, platform operator audience.
- `GLOBAL`: `tenant_id` null, broad app audience governed by explicit service rules.

Tenant admins cannot create or manage `tenant_id IS NULL` notifications and cannot provide `tenantId` in request bodies. Their tenant comes from `TchRequestContext`.

## Audience Types

- `SPECIFIC_ACTORS`
- `PLATFORM_ADMINS`
- `ALL_APP_USERS`
- `TENANT_ADMINS`
- `TENANT_APP_USERS`
- `TENANT_SELLER_TERMINALS`

`ALL_APP_USERS` is a dynamic broadcast and does not fan out to seller terminals. Tenant admins cannot target outside their tenant.

## Lifecycle Semantics

- Click notification: set `read_at = now()` for the current actor, then navigate to `actionRoute` when present.
- Close/X: set `dismissed_at = now()` and set `read_at = now()` if null.
- User delete: forbidden; users never globally delete notifications.
- Cancel: superadmin/system only, reason required, audit required.
- Purge: V0 soft purge (`status = PURGED`, `purged_at = now()`), with `dryRun`.
- Reactivate: extend or make visible the same publication.
- Republish: create a new `notification_publication`; old read/dismiss states remain untouched.
- Replay recipients: add missing recipients only, idempotently, and never reset read/dismiss state.

## Retention & Archive

Notifications are operational inbox records, not long-term business truth. The database should not accumulate hundreds or thousands of stale inbox rows per actor.

V0 retention policy:

- unread active notifications remain visible until `expires_at`, cancel, or purge;
- dismissed/read notifications are eligible for purge after a short retention window;
- expired/cancelled notifications are eligible for soft purge;
- `notification_user_state` and `notification_recipient` are purged with their publication/notification lifecycle;
- audit-sensitive actions (`publish`, `republish`, `cancel`, `purge`) are kept in audit logs, not by keeping inbox rows forever.

Recommended defaults:

- user dismissed/read state: purge after 30 days;
- expired/cancelled notification records: soft purge after 30 days;
- purged notification records: optional hard delete after 90 days in dev/stage, later decided before production;
- trigger logs: keep longer than notification rows, e.g. 90 days, to preserve idempotence windows.

Archive integration:

- the old archive provider for `notification_delivery` must be removed or replaced;
- if notification history must participate in platform archive, the provider should cover the new notification dataset, not external delivery attempts;
- external communication retention belongs to `platform.communication` (`outbound_message` and `message_delivery_attempt`).

## Delivery Channels

`NotificationDeliveryChannel`:

- `IN_APP`
- `EMAIL`
- `SMS`
- `SLACK`

`IN_APP` is handled by `platform.notification`.

`EMAIL`, `SMS`, and `SLACK` are handled by `platform.communication` after a `NotificationPublishedEvent` is emitted after commit.

Default policy V0:

- tenant admin: `IN_APP`; `EMAIL` later; no Slack/SMS.
- superadmin: `IN_APP`, optional `EMAIL`, optional `SLACK`; SMS reserved for later critical use.
- ops critical: `IN_APP + SLACK`.
- support/contact: `IN_APP + EMAIL` where configured.

## Communication Bridge

`NotificationPublishedEvent` contains:

- event ID;
- notification ID;
- publication ID;
- nullable tenant ID;
- scope;
- audience type;
- delivery channels;
- occurred at.

The communication listener:

- ignores `IN_APP`;
- creates communication messages/outbox records for external channels;
- resolves destinations in the communication boundary;
- batches large audiences;
- is idempotent by `(publicationId, channel, recipient/channelDestination)`;
- must not block in-app publication when external delivery fails.

No external communication is sent if the notification transaction rolls back.

Existing communication integration:

- `platform.communication` already has `CommunicationDomainEventRouter`, which listens after commit and routes events through `CommunicationRule` instances into `CommunicationApi.enqueue(...)`;
- `CommunicationApiService.enqueue(...)` writes `outbound_message` and uses `correlation_key` for idempotence;
- `OutboundMessageRetryScheduler` and `OutboundMessageDispatcher` already process pending messages and write `message_delivery_attempt`;
- the notification bridge should therefore be implemented as one or more communication rules/listeners inside `platform.communication`, not as a second delivery pipeline.

Bridge implementation rule:

- prefer adding a `NotificationPublishedCommunicationRule` (or a narrow listener that delegates to the same communication API) that maps `NotificationPublishedEvent` to `SendOutboundMessageRequest`;
- use stable correlation keys such as `notification:{publicationId}:{channel}:{recipientOrDestination}`;
- do not write notification-owned delivery attempts.

## Translation

There are two translation sources:

- system notifications use canonical i18n keys resolved through `i18n_override`;
- manual/ad hoc announcements use `notification_translation`.

System notifications should store stable `title_key` / `message_key` plus payload variables. Their FR/EN/HT text belongs in `i18n_override` with the appropriate surface, so operations can update wording without rewriting notification rows.

Manual announcements require `fr`, `en`, and `ht` rows in `notification_translation`.

System notification wording is managed from the referential/translation administration surface, not from the notification center. The notification center may show the keys and resolved previews for system messages, but any text edit must update `i18n_override`.

Resolution order:

1. current actor locale;
2. `fr`;
3. first available translation.

`NotificationView` returns resolved title/body, resolved locale, available locales when applicable, action metadata, lifecycle state, severity, and creation time.

## Controllers

Tenant admin inbox:

- `GET /api/v1/admin/notifications`
- `GET /api/v1/admin/notifications/unread-count`
- `POST /api/v1/admin/notifications/{notificationId}/read`
- `POST /api/v1/admin/notifications/{notificationId}/dismiss`
- `POST /api/v1/admin/notifications/read-all`

Platform inbox:

- `GET /api/v1/platform/notifications`
- `GET /api/v1/platform/notifications/unread-count`
- `POST /api/v1/platform/notifications/{notificationId}/read`
- `POST /api/v1/platform/notifications/{notificationId}/dismiss`
- `POST /api/v1/platform/notifications/read-all`

Creation:

- `POST /api/v1/admin/notifications/announcements`
- `POST /api/v1/platform/notifications/announcements`
- `POST /api/v1/platform/tenants/{tenantId}/notifications/announcements`

Lifecycle:

- `POST /api/v1/platform/notifications/{notificationId}/publish`
- `POST /api/v1/platform/notifications/{notificationId}/republish`
- `POST /api/v1/platform/notifications/{notificationId}/replay-recipients`
- `POST /api/v1/platform/notifications/{notificationId}/cancel`
- `POST /api/v1/platform/notifications/purge-expired`

Lifecycle endpoints are superadmin only, require a reason where sensitive, and must be audited.

## Trigger Matrix P0

Automatic triggers must use domain/application events or `NotificationApi` after commit. Controllers must not send notifications directly.

P0 triggers:

- Tenant created.
- Tenant admin created/invited.
- Tenant onboarding incomplete.
- Seller terminal created.
- Seller terminal PIN reset.
- Seller terminal blocked/disabled.
- Ops job failed.
- Ops job stale/never-run.
- Ops gate disabled.
- Ops resource critical.
- Result provider down/degraded.
- Draw result missing.
- Draw result corrected.
- Settlement failed.
- Public contact received.
- Tenant support request.
- Seller terminal help request.
- Cache cleared.
- Maintenance scheduled.

Each trigger needs a stable `trigger_key`, `source_type`, and `source_id`, and must use `notification_trigger_log` for idempotence.

## Web

Add:

- API contract under `libs/api`;
- notification API service;
- notification store;
- header bell with unread badge;
- latest 5 dropdown;
- click-to-read/navigate;
- dismiss action;
- admin and platform list pages;
- creation forms for tenant admin and superadmin.

Tenant admin navigation exposes the admin notification center under `Mon entreprise` / `My company`, because tenant notifications are part of the tenant workspace configuration and team communication surface.

UI must be mobile-first, token/theme driven, and use existing Tchalanet web conventions.

## Dev Reset Migration Strategy

This project is still pre-go-live and the development database can be recreated.

Therefore this change does not need a compatibility/backfill path for existing notification rows.

Schema changes must be absorbed into the existing pre-go-live migration files:

- `V100__create_core_tables.sql` for tables and constraints;
- `V103__create_indexes.sql` for indexes;
- `V104__create_triggers.sql` for update triggers;
- `V105__configure_rls.sql` for RLS policies.

The existing notification schema will be replaced, not migrated:

- replace `notification` with the new logical-notification table;
- remove `notification_delivery` from `platform.notification`;
- remove `notification_preference` from `platform.notification` unless reintroduced later as a dedicated preference model;
- keep external delivery data in communication tables such as `outbound_message` and `message_delivery_attempt`;
- keep `notification_template` only if it still serves in-app notification templating, otherwise move templating responsibility to translation rows and communication templates.

No new `V*.sql` file is needed for this dev-reset slice.
