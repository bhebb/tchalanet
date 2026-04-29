# notification-ui Specification

## Purpose

TBD - created by archiving change add-notification-ui-patterns. Update Purpose after archive.

## Requirements

### Requirement: Distinguish transient feedback from persisted notifications

The system SHALL distinguish transient UI feedback from persisted notifications.

Persisted notifications SHALL be created only for information that must remain visible, actionable, auditable, or recoverable after navigation/reload.

#### Scenario: Field validation error

- **WHEN** a form field is invalid
- **THEN** the frontend displays an inline field error
- **AND** no persisted notification is created

#### Scenario: Save success toast

- **WHEN** a user saves a simple configuration successfully
- **THEN** the frontend may show a transient toast/snackbar
- **AND** no persisted notification is required

#### Scenario: Tenant admin action required

- **WHEN** a page template update requires tenant-admin review
- **THEN** the backend creates a persisted actionable notification
- **AND** the frontend may show it in the drawer and notification center

### Requirement: PageModel carries summary only

The PageModel SHALL carry only a lightweight notification summary when needed.

The PageModel SHALL NOT embed the full notification list.

#### Scenario: Dashboard PageModel load

- **WHEN** a user loads a private dashboard PageModel
- **THEN** the PageModel MAY include unread, critical, and action-required counts
- **AND** the frontend SHALL call the notification API separately to load the full drawer/list

### Requirement: Notification list is loaded through dedicated API

The frontend SHALL load detailed notifications through dedicated notification APIs.

#### Scenario: Open notification drawer

- **WHEN** the user opens the notification drawer
- **THEN** the frontend calls a notification list endpoint
- **AND** the endpoint returns a paginated list

### Requirement: Severity maps to consistent UI presentation

The frontend SHALL map notification severity consistently.

Suggested mapping:

- `INFO`: neutral drawer item or subtle toast
- `WARNING`: emphasized drawer item or warning notice
- `ERROR`: error notice or drawer item
- `CRITICAL`: persistent banner and high-priority drawer item

#### Scenario: Critical notification exists

- **WHEN** a user has unread critical notifications
- **THEN** the header summary can indicate critical state
- **AND** the dashboard may show a critical banner

### Requirement: Kind maps to behavior

The frontend SHALL map notification kind to behavior.

Suggested mapping:

- `INFO`: read-only informational item
- `WARNING`: read-only or review item
- `ACTION_REQUIRED`: item with action CTA
- `SYSTEM_ERROR`: item with diagnostics/retry/admin action if allowed

#### Scenario: Action required notification

- **WHEN** notification kind is `ACTION_REQUIRED`
- **THEN** the UI SHALL display a clear action affordance
- **AND** clicking the action SHALL route to or invoke the intended review flow

### Requirement: ApiNotice remains non-persisted

`ApiResponse.notices` SHALL be treated as request-scoped feedback unless explicitly converted into a persisted notification by a backend use case.

#### Scenario: Partial service degradation

- **WHEN** an API response returns service status `DEGRADED`
- **THEN** the frontend may display an `ApiNotice`
- **AND** it SHALL NOT automatically create or assume a persisted notification
