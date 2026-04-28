# Change: Add Notification Core Service

## Summary

Enrich the existing `core.notification` domain so it becomes the single source of truth for Tchalanet notifications across:

- web/in-app notifications,
- SMS delivery,
- WhatsApp delivery,
- future email/push delivery,
- actionable operational alerts.

This change avoids creating separate notification mechanisms for PageModel, tenant admin, POS/session, draw/result, payout, batch, and system alerts.

## Motivation

Tchalanet needs a unified notification domain because alerts will appear in many flows:

- PageModel template updates requiring tenant-admin review,
- limit approvals and warnings,
- POS/session anomalies,
- terminal/offline sync status,
- draw/result ingestion failures,
- payout approvals and payment issues,
- batch/provider/system health alerts,
- security and audit-sensitive operations.

The current architecture already has strong eventing rules: facts are published after commit and listeners must stay thin and idempotent. Notification creation should follow that model instead of embedding notification logic in feature listeners.

## Goals

- Add persistence for notification messages and channel deliveries.
- Support web/in-app notifications with read/archive lifecycle.
- Support multi-channel delivery planning: WEB, SMS, WHATSAPP, EMAIL later.
- Support target audiences: user, role, tenant, outlet, terminal, platform.
- Support severities: INFO, WARNING, ERROR, CRITICAL.
- Support notification kinds: INFO, WARNING, ACTION_REQUIRED, SYSTEM_ERROR.
- Support idempotency so the same source event does not create duplicates.
- Support retry/error tracking for external delivery channels.
- Provide commands, queries, controllers, and Flyway migrations.

## Non-Goals

- Do not implement a full marketing messaging platform.
- Do not create PageModel-specific notification tables.
- Do not persist transient form validation errors or simple toasts.
- Do not force SMS/WhatsApp delivery for every notification.
- Do not build provider-specific SMS/WhatsApp adapters in this change unless already present.

## Scope

### Backend

- `core.notification`
  - domain model
  - commands
  - queries
  - ports
  - persistence adapters
  - controllers
  - Flyway migration

### Integrations

- Event listeners in other domains/features may call `CreateNotificationCommand`.
- Transport adapters may consume pending `notification_delivery` rows.

## Proposed Data Model

### `notification`

Logical notification visible to users or operational audiences.

Recommended fields:

- `id`
- `tenant_id` nullable for platform/global notifications
- `source_type`
- `source_id`
- `dedupe_key`
- `audience_type`: USER, ROLE, TENANT, OUTLET, TERMINAL, PLATFORM
- `audience_value`
- `severity`: INFO, WARNING, ERROR, CRITICAL
- `kind`: INFO, WARNING, ACTION_REQUIRED, SYSTEM_ERROR
- `category`: PAGE_MODEL, TENANT_CONFIG, USER, OUTLET, TERMINAL, SESSION, SALES, DRAW, RESULT, PAYOUT, BATCH, SYSTEM, SECURITY
- `title_key`
- `message_key`
- `title_text` nullable fallback
- `message_text` nullable fallback
- `payload` jsonb
- `action_type` nullable
- `action_url` nullable
- `status`: UNREAD, READ, ARCHIVED, EXPIRED
- `read_at`
- `archived_at`
- `expires_at`
- `created_at`, `updated_at`, `deleted_at`, `version`

### `notification_delivery`

One row per channel delivery attempt/status.

Recommended fields:

- `id`
- `tenant_id` nullable
- `notification_id`
- `channel`: WEB, SMS, WHATSAPP, EMAIL, PUSH
- `recipient`
- `status`: PENDING, SENT, DELIVERED, FAILED, SKIPPED, CANCELLED
- `attempt_count`
- `next_attempt_at`
- `last_attempt_at`
- `provider`
- `provider_message_id`
- `error_code`
- `error_message`
- `payload` jsonb
- `created_at`, `updated_at`, `deleted_at`, `version`

### `notification_preference`

Tenant/user preference for channels by category/kind/severity.

Recommended fields:

- `id`
- `tenant_id`
- `scope_type`: TENANT, ROLE, USER
- `scope_value`
- `category`
- `kind`
- `channel`
- `enabled`
- `created_at`, `updated_at`, `deleted_at`, `version`

## API Shape

Tenant/user endpoints:

- `GET /api/v1/tenant/notifications/summary`
- `GET /api/v1/tenant/notifications`
- `POST /api/v1/tenant/notifications/{id}/read`
- `POST /api/v1/tenant/notifications/{id}/archive`
- `POST /api/v1/tenant/notifications/read-all`

Admin/platform endpoints:

- `GET /api/v1/admin/notifications`
- `POST /api/v1/admin/notifications`
- `GET /api/v1/platform/notifications`
- `POST /api/v1/platform/notifications`

## Compatibility

- This is additive.
- Existing notification code should be migrated or wrapped progressively.
- Existing SMS/WhatsApp sending logic should become delivery-channel adapters, not a separate source of truth.

## Risks

- Notification spam if dedupe is not enforced.
- Confusion between transient UI notices and persisted notifications.
- External channel retries may become noisy without rate limits and preferences.

## Rollout

1. Add schema and core commands/queries.
2. Add web/in-app notification endpoints.
3. Add delivery rows but keep transport workers disabled/feature-flagged if needed.
4. Integrate PageModelTemplateUpdatedEvent in the PageModel workflow change.
5. Gradually migrate other operational alerts.
