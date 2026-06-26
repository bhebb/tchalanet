# Migration Plan — Dev Reset

## Decision

The database will be recreated in development, so notification V0 can replace the existing notification tables instead of preserving/backfilling old rows.

There is no transitional compatibility layer in the target model:

- `notification.status` is lifecycle only: `DRAFT`, `PUBLISHED`, `EXPIRED`, `CANCELLED`, `PURGED`.
- read/dismiss state does not live on `notification`; it lives on `notification_recipient` or `notification_user_state`.
- legacy audiences such as `USER`, `ROLE`, `TENANT`, `TERMINAL`, and `PLATFORM` are not part of V0.

Do not create a new Flyway version for this slice. Update existing pre-go-live migrations:

- `V100__create_core_tables.sql`: tables and constraints.
- `V103__create_indexes.sql`: indexes.
- `V104__create_triggers.sql`: `updated_at` triggers.
- `V105__configure_rls.sql`: RLS policies.

## Current Schema Inventory

Existing notification tables:

- `notification`: combined logical message, audience, translated keys/text, status, read state, archive state, and dedupe.
- `notification_delivery`: delivery attempt table inside notification; this conflicts with the communication boundary.
- `notification_preference`: channel preferences tied to notification categories/kinds.
- `notification_template`: in-app notification template table.
- `NotificationArchiveDatasetProvider`: currently archives `notification_delivery`; this becomes obsolete because `notification_delivery` is removed.

Existing communication tables:

- `outbound_message`: external message/outbox.
- `message_delivery_attempt`: provider delivery attempt status.
- `message_template`: communication templates.
- `tenant_communication_settings`: tenant communication settings.

Existing communication code:

- `CommunicationDomainEventRouter`: after-commit router over `CommunicationRule` instances.
- `CommunicationApiService.enqueue(...)`: creates `outbound_message` with correlation-key idempotence.
- `OutboundMessageRetryScheduler` / `OutboundMessageDispatcher`: retries queued messages and writes `message_delivery_attempt`.

Notification-to-communication bridge should reuse this pipeline.

## Target Changes

- Replace `notification` with the logical notification table defined by this change.
- Remove `notification_delivery`; external attempts belong to communication.
- Remove `notification_preference` for V0 unless a dedicated preference model is reintroduced later.
- Remove or replace the old `NotificationArchiveDatasetProvider` that points at `notification_delivery`.
- Add:
  - `notification_translation`
  - `notification_publication`
  - `notification_recipient`
  - `notification_user_state`
  - `notification_delivery_policy`
  - `notification_trigger_log`
- Keep communication delivery in `outbound_message` and `message_delivery_attempt`.

## Retention

Notifications should stay small because they are inbox records:

- read/dismissed actor states: purge after a short retention window, default 30 days;
- expired/cancelled notification publications: soft purge after 30 days;
- trigger logs: keep longer, default 90 days, for idempotence;
- external communication records follow communication retention, not notification retention.

## Open Question Before Code

Decide whether to keep `notification_template`.

V0 can work without it because manual notifications require stored `fr`, `en`, and `ht` rows in `notification_translation`, while external communication templates already live in `message_template`.

## Audience Resolver Inputs

Known lookup sources:

- Platform admins: `PlatformUserRoleService.listSuperAdmins()` currently returns active superadmin rows.
- Tenant app users: `AppUserJpaRepository.findByTenantMembership(...)` and `countActiveTenantUsers(...)` already filter active tenant memberships.
- Tenant admins: existing tenant-admin search is available through platform access-control/identity internals; for notification, expose a narrow API/port instead of importing internals directly.
- Seller terminals: owned by `core.sellerterminal`; resolve through `ListSellerTerminalsQuery` via the query bus or add a narrow core API/query for active terminal IDs.

Boundary rule:

- `platform.notification` must not import `core.sellerterminal.internal.*`, `platform.identity.internal.*`, or `platform.accesscontrol.internal.*` from outside an intentional adapter. Use public APIs/query bus/ports.
