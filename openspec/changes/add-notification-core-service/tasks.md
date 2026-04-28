# Tasks: Add Notification Core Service

## 1. Domain and persistence

- [ ] Review existing `core.notification` package and preserve compatible code where possible.
- [ ] Add/confirm typed IDs: `NotificationId`, `NotificationDeliveryId`, `NotificationPreferenceId`.
- [ ] Add enums:
  - [ ] `NotificationSeverity`
  - [ ] `NotificationKind`
  - [ ] `NotificationCategory`
  - [ ] `NotificationAudienceType`
  - [ ] `NotificationStatus`
  - [ ] `NotificationChannel`
  - [ ] `NotificationDeliveryStatus`
- [ ] Add Flyway migration for `notification`.
- [ ] Add Flyway migration for `notification_delivery`.
- [ ] Add Flyway migration for `notification_preference` if preferences are in scope for MVP.
- [ ] Add RLS policies for tenant-scoped rows.
- [ ] Add indexes:
  - [ ] `(tenant_id, audience_type, audience_value, status, created_at desc)`
  - [ ] `(tenant_id, dedupe_key)` unique where dedupe_key is not null
  - [ ] `(notification_id, channel)`
  - [ ] `(status, next_attempt_at)` for delivery workers

## 2. Commands

- [ ] Add `CreateNotificationCommand`.
- [ ] Add `CreateNotificationHandler` with idempotency/dedupe handling.
- [ ] Add `MarkNotificationReadCommand`.
- [ ] Add `ArchiveNotificationCommand`.
- [ ] Add `ExpireNotificationsCommand`.
- [ ] Add `ScheduleNotificationDeliveryCommand` if delivery rows are not created in `CreateNotificationCommand`.
- [ ] Ensure write handlers use `@UseCase` and `@TchTx`.

## 3. Queries

- [ ] Add `GetNotificationSummaryQuery`.
- [ ] Add `ListNotificationsQuery` returning `TchPage<NotificationItemView>`.
- [ ] Add `ListNotificationDeliveriesQuery` for admin/platform diagnostics.
- [ ] Ensure queries are read-only and RLS-aware.

## 4. Ports and adapters

- [ ] Add `NotificationWriterPort`.
- [ ] Add `NotificationReaderPort`.
- [ ] Add `NotificationDeliveryWriterPort`.
- [ ] Add JPA entities and repositories under infra/persistence.
- [ ] Add mapper(s), using `CommonIdMapper` for typed IDs.

## 5. Controllers

- [ ] Add tenant notification controller.
- [ ] Add admin/platform notification controller if needed.
- [ ] Use `@CurrentContext TchRequestContext`.
- [ ] Use `@TchPaging` for list endpoints.
- [ ] Return `ApiResponse<T>` for new endpoints.
- [ ] Return `ProblemDetail` for errors.

## 6. Delivery channels

- [ ] Create delivery abstraction for WEB/SMS/WHATSAPP/EMAIL.
- [ ] Persist delivery status transitions.
- [ ] Add retry metadata.
- [ ] Feature-flag external transport workers if not enabled for MVP.

## 7. Audit and cleanup

- [ ] Audit critical notification creation and admin actions.
- [ ] Add cleanup/expiration job for old notifications.
- [ ] Add tests for dedupe and audience filtering.
- [ ] Add tests for read/archive lifecycle.
