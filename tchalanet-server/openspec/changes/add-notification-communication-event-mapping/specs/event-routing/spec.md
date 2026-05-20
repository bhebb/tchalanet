# event-routing Spec

## ADDED Requirements

### Requirement: Event mapping source of truth

The system SHALL maintain a documented mapping of events to notification and communication behavior.

#### Scenario: Implementing new event listener

- **GIVEN** a developer adds a listener for `PayoutRejectedEvent`
- **WHEN** they implement notification or communication behavior
- **THEN** it must match the documented mapping or update the mapping in the same change

### Requirement: Tenant admin default channels

Tenant admin messages SHALL default to in-app notification and email for important action-required/security events.

#### Scenario: Tenant admin invited

- **GIVEN** a tenant admin is invited
- **WHEN** event is consumed
- **THEN** an invitation email is enqueued

### Requirement: Tenant Slack opt-in

Tenant Slack SHALL only be used when tenant settings enable it.

#### Scenario: Terminal blocked event

- **GIVEN** tenant Slack is disabled
- **WHEN** `TerminalBlockedEvent` is consumed
- **THEN** no tenant Slack message is enqueued

### Requirement: Internal Slack for platform ops

Internal Slack SHALL be used for platform ops failures, fraud/security signals and provider incidents.

#### Scenario: Batch failed

- **GIVEN** `BatchFailedEvent`
- **WHEN** communication rules evaluate it
- **THEN** a `SLACK_INTERNAL` outbound message is enqueued

### Requirement: HTTP notices are not push

HTTP notices SHALL NOT be treated as push/in-app notifications.

#### Scenario: ApiResponse contains LIMIT_WARN

- **GIVEN** a response contains `LIMIT_WARN`
- **WHEN** the client receives it
- **THEN** the notice is displayed for the current request only
- **AND** it does not appear in notification center unless a separate event/rule creates one

### Requirement: Errors remain ProblemDetail

HTTP errors SHALL be represented with `ProblemDetail` and SHALL NOT be converted to notifications by default.

#### Scenario: Permission denied

- **GIVEN** user lacks permission
- **WHEN** API returns 403
- **THEN** the response is `ProblemDetail`
- **AND** no notification is created unless a separate security policy/event triggers one
