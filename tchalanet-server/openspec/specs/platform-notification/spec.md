# platform-notification Specification

## Purpose

`platform.notification` owns persistent in-app notifications for the web and mobile notification centers.

Core and system components publish facts. `platform.notification` maps supported facts to
notification-center records. External provider delivery remains owned by `platform.communication`.

## Requirements

### Requirement: Platform notification capability

The system SHALL provide a `platform.notification` capability for persistent in-app notifications.

#### Scenario: Create notification from event

- **GIVEN** a supported domain or system event is consumed
- **WHEN** a `NotificationRule` maps it to a `NotificationIntent`
- **THEN** the system persists a notification with target, severity, title, message, metadata and source event id

### Requirement: Notification audience model

Notifications SHALL support audience types:

- SPECIFIC_ACTORS
- PLATFORM_ADMINS
- ALL_APP_USERS
- TENANT_ADMINS
- TENANT_APP_USERS
- TENANT_SELLER_TERMINALS

#### Scenario: Tenant admin notification

- **GIVEN** a payout request event
- **WHEN** the notification rule targets `TENANT_ADMINS`
- **THEN** the notification is visible to authorized tenant admin users

### Requirement: Notification translations

The system SHALL resolve notification wording from system i18n keys or manual notification translations.

#### Scenario: Render localized notification

- **GIVEN** a notification with system keys or manual translations
- **WHEN** the current actor lists notifications
- **THEN** the platform returns title/body using actor locale, then `fr`, then the first available translation

#### Scenario: Manual announcement translation requirement

- **GIVEN** an admin creates a manual announcement
- **WHEN** the request is validated
- **THEN** `fr`, `en` and `ht` translations are required

### Requirement: Notification idempotency

Event-driven notification listeners SHALL be idempotent.

#### Scenario: Duplicate event replay

- **GIVEN** `notification_trigger_log` already contains `(trigger_key, source_type, source_id)`
- **WHEN** the same trigger is evaluated again
- **THEN** no duplicate notification is created

### Requirement: No external delivery

`platform.notification` SHALL NOT send email, SMS, Slack or push provider messages directly.

#### Scenario: Notification that also needs email

- **GIVEN** an event requires in-app notification and external email
- **WHEN** the event is consumed
- **THEN** `platform.notification` creates only the in-app notification
- **AND** `platform.communication` is responsible for email delivery through its own rule/listener

### Requirement: Tenant notification APIs

The system SHALL expose tenant admin and platform APIs for listing, counting unread, marking read and dismissing notifications.

#### Scenario: Mark notification read

- **GIVEN** a user has an unread notification
- **WHEN** the user calls mark-read endpoint
- **THEN** `read_at` is set and unread count decreases

#### Scenario: Dismiss notification

- **GIVEN** a user has a visible notification
- **WHEN** the user calls dismiss endpoint
- **THEN** `dismissed_at` is set for that actor
- **AND** other actors still see their own state

### Requirement: Notification lifecycle controls

The system SHALL support publish, republish, replay recipients, cancel and purge lifecycle actions for authorized platform operators.

#### Scenario: Republish notification

- **GIVEN** a published notification has existing read/dismiss states
- **WHEN** an operator republishes it
- **THEN** a new publication is created
- **AND** old read/dismiss states remain attached to the old publication

#### Scenario: Cancel notification

- **GIVEN** an operator cancels a notification
- **WHEN** a valid reason is supplied
- **THEN** the notification is no longer active
- **AND** the action is audited

### Requirement: Retention and purge

The system SHALL soft-purge stale inbox rows for read, dismissed, expired and cancelled notification records according to retention policy.

#### Scenario: Purge dry run

- **GIVEN** eligible notification rows exist
- **WHEN** a superadmin runs purge with `dryRun=true`
- **THEN** the system reports counts without marking rows purged

### Requirement: Notification publication event

The system SHALL emit an after-commit publication event when a notification publication requests external delivery channels.

#### Scenario: Notification requests email

- **GIVEN** a notification is published with `IN_APP` and `EMAIL`
- **WHEN** the transaction commits
- **THEN** the notification center state is available in-app
- **AND** `platform.communication` receives the publication event for external delivery

## Constraints

- No `platform.notification.internal` import outside the capability.
- No direct repository access from controllers.
- 2xx JSON responses use `ApiResponse<T>`.
- Errors use `ProblemDetail`.
