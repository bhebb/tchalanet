# notification flows spec delta

## ADDED Requirements

### Requirement: Notification routing must be centralized

Business notification routing SHALL be handled by the notification module, not scattered inside producer handlers.

#### Scenario: Draw result fetched event is consumed by notification module

- **GIVEN** a `DrawResultFetchedEvent` is published by drawresult
- **WHEN** the transaction commits
- **THEN** a notification listener in the notification module SHALL handle it
- **AND** routing SHALL be decided by `NotificationFlowRouter`

### Requirement: Normal scheduler success is quiet

The system SHALL NOT send a notification for every successful scheduler tick.

#### Scenario: Scheduler tick succeeds

- **GIVEN** a scheduled batch runs successfully
- **WHEN** the batch completes
- **THEN** no Slack or email notification SHALL be sent by default

### Requirement: Watched draw result fetch sends useful dev notifications

Watched draw result fetches SHALL send useful summaries without notifying for every provider/slot.

#### Scenario: Watched NY draw result fetched

- **GIVEN** draw result notifications are enabled
- **AND** provider `NY` is watched
- **AND** slot `NY_MID` is watched
- **WHEN** a `DrawResultFetchedEvent` is handled
- **THEN** the system SHALL send a Slack summary if Slack is enabled
- **AND** it SHALL send a detailed email if email detail is enabled

#### Scenario: Unwatched provider result fetched

- **GIVEN** draw result notifications are enabled
- **AND** provider `GA` is not watched
- **WHEN** a `DrawResultFetchedEvent` is handled
- **THEN** the system SHALL not send the dev detailed email

### Requirement: Draw lifecycle INFO must be configurable

Successful draw lifecycle notifications SHALL be sent only when explicitly enabled.

#### Scenario: Draw open completed with info disabled

- **GIVEN** `draw-lifecycle.slack-info-enabled=false`
- **WHEN** draw open completed event is handled
- **THEN** no Slack INFO notification SHALL be sent

#### Scenario: Draw close completed with info enabled

- **GIVEN** `draw-lifecycle.slack-info-enabled=true`
- **WHEN** draw close completed event is handled
- **THEN** Slack INFO notification SHALL be sent to the configured channel

### Requirement: Apply no-candidate warning

The system SHALL notify when apply finds no candidate draw for an expected result.

#### Scenario: Apply no candidate

- **GIVEN** apply notifications are enabled
- **WHEN** a no-candidate apply event is handled
- **THEN** the system SHALL send a WARN notification

### Requirement: Sales report flows are disabled by default

Sales report notifications SHALL be defined but disabled by default.

#### Scenario: Sales daily report disabled

- **GIVEN** `sales-reports.enabled=false`
- **WHEN** a sales daily report event is handled
- **THEN** no notification SHALL be sent
