# Tasks

## Slice 0 — Discovery & Migration Plan

- [x] Inspect current notification and communication schema/entities.
- [x] Document dev-reset migration strategy with no compatibility/backfill path.
- [x] Confirm Flyway strategy: absorb changes into existing pre-go-live migrations, no new `V*.sql`.
- [x] Identify app-user, seller-terminal, tenant-admin, and platform-admin lookup APIs for audience resolution.

## Slice 1 — DB + Inbox In-App

- [x] Add notification tables, indexes, and RLS policies.
- [x] Remove old `notification_delivery`/`notification_preference` schema and obsolete archive provider references.
- [x] Add typed IDs for notification, publication, and recipient IDs.
- [x] Implement in-app list, unread count, mark read, dismiss, and mark all read.
- [x] Support dynamic broadcast user state.
- [x] Support explicit app-user recipients through `notification_recipient`.
- [x] Add explicit seller-terminal recipient support to the notification recipient contract.
- [x] Add purge/retention rules for read, dismissed, expired, and cancelled notification rows.
- [ ] Add focused backend tests for inbox state and tenant isolation.
- [x] Add minimal frontend bell, unread badge, latest dropdown, and dismiss/read actions.

## Slice 2 — Creation + Translations

- [x] Implement tenant admin announcement creation with tenant from context only.
- [x] Implement superadmin platform/global creation.
- [x] Implement superadmin tenant-targeted creation through route tenant.
- [x] Resolve system notification keys through `i18n_override`.
- [x] Require FR/EN/HT translations for manual announcements.
- [ ] Implement translation fallback resolution.
- [x] Add admin/platform notification list pages.
- [ ] Add backend tests for creation rules, translations, and RLS.

## Slice 3 — Publication Lifecycle

- [x] Implement publish.
- [x] Implement republish as a new publication that resets unread state only for the new publication.
- [x] Implement replay recipients idempotently.
- [x] Implement cancel with required reason and audit.
  - [x] Require reason and cancel active publications.
  - [x] Add audit event/hook for cancel.
- [x] Implement purge expired with dry-run and soft-purge V0.
- [ ] Add lifecycle action tests.

## Slice 4 — Communication Bridge

- [x] Add notification delivery policy for `IN_APP`, `EMAIL`, `SMS`, `WHATSAPP`, and `SLACK`.
- [x] Emit `NotificationPublishedEvent` after commit.
- [x] Reuse existing `platform.communication` after-commit router/rule pipeline for external channels.
- [x] Add communication rule/listener that ignores `IN_APP`.
- [x] Create communication messages/outbox entries for external channels.
- [x] Resolve external recipients automatically for platform admins, tenant admins, tenant users, and specific app-user notifications.
- [x] Queue job lifecycle Slack alerts through `platform.communication` for failed/non-quiet skipped jobs.
- [x] Expose superadmin ops view for queued messages, retry attempts, and manual dispatch of due messages.
- [x] Expose superadmin provider test actions for Slack, email, SMS, and WhatsApp through edge-service.
- [x] Enforce external delivery idempotence by publication, channel, and destination.
- [ ] Add seller-terminal external recipient lookup through a public core API before broad seller-terminal SMS/WhatsApp fanout.
- [ ] Add rollback and duplicate-event tests.

## Slice 5 — Automatic Triggers

- [x] Add `notification_trigger_log`.
- [x] Implement trigger service with stable `trigger_key`, `source_type`, and `source_id`.
- [x] Add tenant onboarding triggers.
- [x] Add seller terminal triggers.
- [x] Add ops job/gate/resource triggers.
  - [x] Add ops job failed/non-quiet skipped system notification trigger.
- [x] Add draw result and settlement triggers.
- [x] Add support/contact triggers.
- [x] Add cache and maintenance triggers.
- [ ] Add idempotence tests for each P0 trigger family.

## Slice 6 — Full UI

- [x] Add admin notification center page with filters and pagination.
- [x] Expose admin notification center under tenant admin `Mon entreprise`.
- [x] Add platform notification center page with filters and pagination.
- [x] Add tenant admin creation form.
- [x] Add superadmin creation form.
- [x] Add lifecycle controls where authorized.
- [x] Add severity badges, empty states, mobile layout, and language-aware rendering.
- [x] For system notifications, show keys/resolved previews and route text edits to referential translations (`i18n_override`).

## Acceptance

- [x] Notification and communication responsibilities are separated.
- [x] `notification_recipient` remains in-app only.
- [x] External channels are processed by `platform.communication`.
- [x] Tenant admin can send tenant notifications only.
- [x] Superadmin can send platform/global and tenant-targeted notifications.
- [ ] FR/EN/HT are supported and resolved by current locale.
- [x] Users can read and dismiss without deleting for others.
- [x] Republish, replay recipients, cancel, and soft purge work.
- [x] Sensitive lifecycle actions are audited.
- [x] Automatic triggers are idempotent.
- [ ] No external delivery is sent on rollback.
- [ ] API responses use standard pagination, ProblemDetail errors, and `ApiResponse<T>` for 2xx responses.
