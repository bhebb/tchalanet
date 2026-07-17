## ADDED Requirements

### Requirement: Action-required notifications enqueue internal Slack

The communication capability SHALL map `DRAW_RESULT_ACTION_REQUIRED` notification publications to
internal Slack through the existing notification-to-communication bridge.

The flow SHALL remain:

`platform.notification -> platform.communication -> SlackProviderAdapter -> edge-service -> configured Slack channel`

The scheduler and `core.drawresult` SHALL NOT call Slack directly and SHALL NOT implement a second
drawresult-specific Slack mechanism.

#### Scenario: Manual result reminder Slack

- **GIVEN** a `DRAW_RESULT_ACTION_REQUIRED` notification is published with reason
  `MANUAL_ENTRY_REQUIRED`
- **WHEN** the notification/communication bridge processes it
- **THEN** `platform.communication` enqueues one `SLACK_INTERNAL` outbound message
- **AND** the message clearly indicates normal manual entry is required
- **AND** the message includes provider, slot, draw date, expected local draw time, timezone, elapsed
  duration, reason, ops link and correlation/request id when available
- **AND** the Slack correlation key is derived from the notification correlation key.

#### Scenario: Automatic provider overdue Slack

- **GIVEN** a `DRAW_RESULT_ACTION_REQUIRED` notification is published with reason
  `AUTOMATIC_FETCH_OVERDUE`
- **WHEN** the notification/communication bridge processes it
- **THEN** `platform.communication` enqueues one `SLACK_INTERNAL` outbound message
- **AND** the message clearly indicates an automatic provider is overdue, not reclassified as manual
- **AND** the Slack correlation key is derived from the notification correlation key.

#### Scenario: Slack bridge retry is idempotent

- **GIVEN** a Slack outbound message already exists for the notification correlation key
- **WHEN** the bridge retries or the notification publication event is replayed
- **THEN** no second Slack outbound message is created.

#### Scenario: Slack failure does not rollback notification

- **GIVEN** a `DRAW_RESULT_ACTION_REQUIRED` notification was persisted
- **WHEN** Slack delivery fails
- **THEN** the notification remains persisted
- **AND** the communication delivery attempt records the failure and follows retry policy.

### Requirement: Result-arrived external delivery is policy driven

Result-available notifications SHALL request only the external channels configured by product policy.

#### Scenario: In-app only by default

- **GIVEN** a result-available notification is published
- **WHEN** no tenant external delivery policy is enabled
- **THEN** no email, SMS, WhatsApp or tenant Slack message is enqueued
- **AND** in-app notifications remain available.

#### Scenario: Tenant Slack opt-in

- **GIVEN** tenant Slack result alerts are enabled for a tenant
- **WHEN** a result-available notification is published for that tenant
- **THEN** `platform.communication` enqueues one tenant Slack message with a stable correlation key
- **AND** it does not use internal Slack unless platform policy also requires it.

### Requirement: Slack is not notification source of truth

Slack delivery SHALL be an external projection of notification state, not a second source of truth.

#### Scenario: In-app TTL does not delete Slack history

- **GIVEN** an action-required notification expires or is resolved in-app
- **WHEN** its active notification visibility changes
- **THEN** the system does not attempt to delete a Slack message that was already published
- **AND** any optional resolution Slack message is a separate outbound message with a separate
  correlation key.
