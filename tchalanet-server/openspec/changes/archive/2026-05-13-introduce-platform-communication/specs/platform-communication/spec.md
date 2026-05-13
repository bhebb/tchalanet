# platform-communication Spec

## ADDED Requirements

### Requirement: Communication capability

The system SHALL provide `platform.communication` for external message delivery.

Supported initial channels:

- EMAIL
- SMS
- SLACK_INTERNAL
- SLACK_TENANT_WEBHOOK

Future channel:

- PUSH

#### Scenario: External message capability is available through platform

- **GIVEN** a backend module needs to deliver an external message
- **WHEN** it uses `platform.communication.api.CommunicationApi`
- **THEN** it can target email, SMS, internal Slack or tenant Slack without importing provider adapters

### Requirement: Enqueue by default

The system SHALL use `CommunicationApi.enqueue` for automatic/event-driven messages.

#### Scenario: Event-driven payout paid SMS

- **GIVEN** `PayoutPaidEvent` is consumed
- **WHEN** policy requires customer SMS receipt
- **THEN** `platform.communication` creates an `outbound_message`
- **AND** the provider call occurs later through dispatcher/retry

### Requirement: sendNow exception

The system SHALL use `sendNow` only for controlled ops tests or diagnostics.

#### Scenario: Slack test endpoint

- **GIVEN** a SUPER_ADMIN calls `/platform/ops/slack-test`
- **WHEN** the request is valid
- **THEN** `sendNow` may call the Slack provider and return immediate provider result

### Requirement: No fallback from enqueue to sendNow

The system SHALL NOT call `sendNow` as a fallback when `enqueue` fails.

#### Scenario: Enqueue persistence failure

- **GIVEN** enqueue fails due to DB or validation error
- **WHEN** the failure is raised
- **THEN** the system does not attempt direct provider delivery

### Requirement: Delivery attempts

Every provider delivery attempt SHALL be recorded.

#### Scenario: Slack provider fails temporarily

- **GIVEN** an outbound Slack message is dispatched
- **WHEN** provider returns retryable failure
- **THEN** a delivery attempt is recorded
- **AND** the outbound message is scheduled for retry

### Requirement: Correlation key idempotency

Event-driven outbound messages SHALL include a stable `correlationKey`.

#### Scenario: Duplicate PayoutPaidEvent replay

- **GIVEN** an outbound message already exists for `payout:{id}:paid:sms`
- **WHEN** the same event is consumed again
- **THEN** no duplicate SMS message is created

### Requirement: Provider adapters are internal

Provider classes for Slack, email, SMS and push SHALL live under `platform.communication.internal.adapter` and SHALL NOT be imported from other modules.

#### Scenario: Batch wants Slack alert

- **GIVEN** a batch failure occurs
- **WHEN** alerting is requested
- **THEN** the batch/common layer publishes an event or calls an allowed API depending on ownership
- **AND** it never imports `SlackProviderAdapter` or gateway classes

### Requirement: Tenant Slack opt-in

Tenant Slack delivery SHALL be disabled unless tenant communication settings explicitly enable it.

#### Scenario: Offline sync suspicious event

- **GIVEN** tenant Slack is disabled
- **WHEN** communication rule evaluates the event
- **THEN** no tenant Slack outbound message is created
- **AND** internal Slack may still be created for platform ops if policy requires it

## Constraints

- Message templates live in `platform.communication`.
- Templates do not live in `common`.
- PageTemplates remain UI/page model templates only.
- `enqueue` returns after local persistence, not provider delivery.
