# Specification: Notification Core Service

## ADDED Requirements

### Requirement: Persist logical notifications

The system SHALL persist logical notifications in the `core.notification` domain.

Each notification SHALL include:

- a severity,
- a kind,
- a category,
- an audience,
- a localized title/message key or fallback text,
- optional payload metadata,
- optional action metadata,
- lifecycle status.

#### Scenario: Create tenant-admin action notification

- **WHEN** a template update requires tenant-admin review
- **THEN** the system creates a notification with:
  - audience type `ROLE`
  - audience value `TENANT_ADMIN`
  - kind `ACTION_REQUIRED`
  - category `PAGE_MODEL`
  - severity at least `WARNING`
  - an action pointing to the review flow

### Requirement: Persist delivery attempts separately from logical notification

The system SHALL store channel delivery attempts separately from the logical notification.

A logical notification MAY have zero or more delivery rows.

#### Scenario: Web-only notification

- **WHEN** a notification is only intended for the in-app drawer
- **THEN** it MAY create a `WEB` delivery row or rely on the notification row itself
- **AND** it MUST NOT require SMS/WhatsApp delivery rows

#### Scenario: SMS and WhatsApp notification

- **WHEN** a notification requires external delivery
- **THEN** the system creates delivery rows for the requested channels
- **AND** each delivery row tracks its own status, attempts, provider response, and errors

### Requirement: Support audience targeting

The system SHALL support the following notification audiences:

- `USER`
- `ROLE`
- `TENANT`
- `OUTLET`
- `TERMINAL`
- `PLATFORM`

#### Scenario: Role-targeted notification

- **WHEN** a notification targets `ROLE:TENANT_ADMIN`
- **THEN** all matching tenant admins SHALL be eligible to see it
- **AND** the notification SHALL remain tenant-scoped if a tenant is present

### Requirement: Support notification severities

The system SHALL support these severities:

- `INFO`
- `WARNING`
- `ERROR`
- `CRITICAL`

#### Scenario: Critical alert

- **WHEN** a critical provider/system failure affects operations
- **THEN** the notification SHALL be created with severity `CRITICAL`
- **AND** the frontend MAY surface it as a banner or high-priority drawer item

### Requirement: Support notification kinds

The system SHALL support these kinds:

- `INFO`
- `WARNING`
- `ACTION_REQUIRED`
- `SYSTEM_ERROR`

#### Scenario: Action required

- **WHEN** a user decision is required
- **THEN** the notification SHALL include an action type or action URL

### Requirement: Dedupe source events

The system SHALL support an optional dedupe key to prevent duplicate notifications for the same source event or actionable state.

#### Scenario: Same event replayed

- **GIVEN** a notification exists for the same tenant and dedupe key
- **WHEN** the same source event is replayed
- **THEN** the system SHALL NOT create duplicate unread notifications
- **AND** it MAY update payload or timestamps according to implementation rules

### Requirement: Notification listing

The system SHALL provide APIs for listing and summarizing notifications.

#### Scenario: Dashboard loads notification summary

- **WHEN** a user loads a dashboard or PageModel
- **THEN** the frontend can obtain unread count, critical count, and action-required count

#### Scenario: User opens notification drawer

- **WHEN** a user opens the notification drawer
- **THEN** the frontend can request a paginated list of notifications
- **AND** the response SHALL support filtering by status, category, kind, and severity

### Requirement: Read and archive lifecycle

The system SHALL allow users to mark one or more notifications as read and archive one or more notifications.

#### Scenario: Mark one notification as read

- **WHEN** a user marks a notification as read
- **THEN** the notification status changes from `UNREAD` to `READ`
- **AND** `read_at` is set

#### Scenario: Archive notification

- **WHEN** a user archives a notification
- **THEN** the notification status changes to `ARCHIVED`
- **AND** it is excluded from default active lists

#### Scenario: Mark multiple notifications as read

- **WHEN** a user selects multiple notifications in the notification center and marks them as read
- **THEN** each selected unread notification status changes to `READ`
- **AND** `read_at` is set for each updated notification

#### Scenario: Archive multiple notifications

- **WHEN** a user selects multiple notifications in the notification center and archives them
- **THEN** each selected notification status changes to `ARCHIVED`
- **AND** each archived notification is excluded from default active lists

### Requirement: Notification preferences

The system SHALL support preferences by tenant, role, or user to enable/disable channels per category/kind.

#### Scenario: Tenant disables WhatsApp for system info

- **GIVEN** the tenant disabled WhatsApp for `SYSTEM:INFO`
- **WHEN** a matching notification is created
- **THEN** no WhatsApp delivery row is scheduled
- **AND** web delivery remains allowed unless also disabled
