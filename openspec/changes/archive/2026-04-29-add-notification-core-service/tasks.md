# Tasks: Add Notification Core Service

## 1. Domain and persistence

- [x] Review existing `core.notification` package and preserve compatible code where possible.
- [x] Add/confirm typed IDs: `NotificationId`, `NotificationDeliveryId`, `NotificationPreferenceId`.
- [x] Add enums:
  - [x] `NotificationSeverity`
  - [x] `NotificationKind`
  - [x] `NotificationCategory`
  - [x] `NotificationAudienceType`
  - [x] `NotificationStatus`
  - [x] `NotificationChannel`
  - [x] `NotificationDeliveryStatus`
- [x] Add Flyway migration for `notification`.
- [x] Add Flyway migration for `notification_delivery`.
- [x] Add Flyway migration for `notification_preference` if preferences are in scope for MVP.
- [x] Add RLS policies for tenant-scoped rows.
- [x] Add indexes:
  - [x] `(tenant_id, audience_type, audience_value, status, created_at desc)`
  - [x] `(tenant_id, dedupe_key)` unique where dedupe_key is not null
  - [x] `(notification_id, channel)`
  - [x] `(status, next_attempt_at)` for delivery workers

## 2. Commands

- [x] Add `CreateNotificationCommand`.
- [x] Add `CreateNotificationHandler` with idempotency/dedupe handling.
- [x] Add `MarkNotificationReadCommand`.
- [x] Add `ArchiveNotificationCommand`.
- [x] Add `ExpireNotificationsCommand`.
- [x] Add `ScheduleNotificationDeliveryCommand` if delivery rows are not created in `CreateNotificationCommand`.
- [x] Ensure write handlers use `@UseCase` and `@TchTx`.

## 3. Queries

- [x] Add `GetNotificationSummaryQuery`.
- [x] Add `ListNotificationsQuery` returning `TchPage<NotificationItemView>`.
- [x] Add `ListNotificationDeliveriesQuery` for admin/platform diagnostics.
- [x] Ensure queries are read-only and RLS-aware.

## 4. Ports and adapters

- [x] Add `NotificationWriterPort`.
- [x] Add `NotificationReaderPort`.
- [x] Add `NotificationDeliveryWriterPort`.
- [x] Add JPA entities and repositories under infra/persistence.
- [x] Add mapper(s), using `CommonIdMapper` for typed IDs.

## 5. Controllers

- [x] Add tenant notification controller.
- [x] Add admin/platform notification controller if needed.
- [x] Use `@CurrentContext TchRequestContext`.
- [x] Use `@TchPaging` for list endpoints.
- [x] Return `ApiResponse<T>` for new endpoints.
- [ ] Return `ProblemDetail` for errors.

## 6. Delivery channels

- [x] Create delivery abstraction for WEB/SMS/WHATSAPP/EMAIL.
- [x] Persist delivery status transitions.
- [x] Add retry metadata.
- [x] Feature-flag external transport workers if not enabled for MVP.

## 7. Audit and cleanup

- [ ] Audit critical notification creation and admin actions.
- [ ] Add cleanup/expiration job for old notifications.
- [ ] Add tests for dedupe and audience filtering.
- [x] Add tests for read/archive lifecycle.
