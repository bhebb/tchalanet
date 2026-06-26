# Notifications V0 + Communication Bridge

## Why

Tchalanet needs a durable in-app notification capability for platform operators, tenant admins, app users, and seller terminals. The current notification/delivery model is too easy to confuse with external communication delivery: in-app read/dismiss state, publication lifecycle, and email/SMS/Slack retry should not live in the same concept.

## What

- Introduce a cross-project notification model where `platform.notification` owns the logical notification, publication lifecycle, recipients, and in-app actor state.
- Keep external delivery in `platform.communication`; notification only records requested delivery policy and publishes an after-commit event.
- Support tenant, platform, and global notifications with PostgreSQL RLS and tenant-aware controller rules.
- Support FR/EN/HT translations, unread counts, mark-read, dismiss, mark-all-read, publish, republish, replay recipients, cancel, and soft purge.
- Add a minimal Angular notification center: bell, unread badge, latest menu, list page, and creation forms for tenant admin and superadmin.
- Add idempotent automatic triggers for onboarding, seller terminal, ops, draw/result, support/contact, cache, and maintenance events.

## Impact

- Backend: `tchalanet-server` platform notification and communication modules, Flyway migrations, RLS policies, API contracts, tests.
- Web: `tchalanet-web` API contracts, notification store, header bell/menu, admin/platform notification pages.
- Data: additive notification schema with a migration/backfill plan for existing notification rows.

## Non-goals

- Hard delete in V0.
- Storing email/SMS/Slack delivery status inside notification recipients.
- Full push notification/mobile delivery.
- Rich message templates beyond translated title/body/action metadata.
- Replacing platform.communication retry/provider logic.

