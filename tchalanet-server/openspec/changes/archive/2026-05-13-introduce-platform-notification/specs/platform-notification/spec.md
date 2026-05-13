# platform-notification Spec

## ADDED Requirements

### Requirement: Platform notification capability

The system SHALL provide a `platform.notification` capability for persistent in-app notifications.

#### Scenario: Create notification from event

- **GIVEN** a supported domain or system event is consumed
- **WHEN** a `NotificationRule` maps it to a `NotificationIntent`
- **THEN** the system persists a notification with target, severity, title, message, metadata and source event id

### Requirement: Notification target model

Notifications SHALL support targets:

- USER
- ROLE
- OUTLET
- TENANT
- PLATFORM

#### Scenario: Tenant admin notification

- **GIVEN** a payout request event
- **WHEN** the notification rule targets `TENANT_ADMIN`
- **THEN** the notification is visible to authorized tenant admin users

### Requirement: Notification templates

The system SHALL keep in-app notification templates in `platform.notification`, not in `common` and not in `catalog.pagetemplate`.

#### Scenario: Render localized notification

- **GIVEN** a notification template key and locale
- **WHEN** a notification is created
- **THEN** the platform renders title/body using tenant override then default template fallback

### Requirement: Notification idempotency

Event-driven notification listeners SHALL be idempotent.

#### Scenario: Duplicate event replay

- **GIVEN** an already processed event id for handler key `notification.payout.requested`
- **WHEN** the same event is consumed again
- **THEN** no duplicate notification is created

### Requirement: No external delivery

`platform.notification` SHALL NOT send email, SMS, Slack or push provider messages directly.

#### Scenario: Notification that also needs email

- **GIVEN** an event requires in-app notification and external email
- **WHEN** the event is consumed
- **THEN** `platform.notification` creates only the in-app notification
- **AND** `platform.communication` is responsible for email delivery through its own rule/listener

### Requirement: Tenant notification APIs

The system SHALL expose tenant user APIs for listing, counting unread, marking read and archiving notifications.

#### Scenario: Mark notification read

- **GIVEN** a user has an unread notification
- **WHEN** the user calls mark-read endpoint
- **THEN** `read_at` is set and unread count decreases

## Constraints

- No `platform.notification.internal` import outside the capability.
- No direct repository access from controllers.
- 2xx JSON responses use `ApiResponse<T>`.
- Errors use `ProblemDetail`.
