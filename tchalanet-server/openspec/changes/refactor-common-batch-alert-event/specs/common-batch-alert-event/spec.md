# common-batch-alert-event Spec

## ADDED Requirements

### Requirement: Common batch alert is provider-neutral

`common.batch.alert` SHALL define only provider-neutral alert concepts.

It SHALL NOT import:

- `platform.communication.api`;
- `MessageChannel`;
- `MessageTemplateKey`;
- Slack/email/SMS provider classes.

#### Scenario: Batch annotation is used

- **GIVEN** a method annotated with `@NotifyOnBatchFailure`
- **WHEN** the method throws
- **THEN** common publishes `BatchFailedEvent`
- **AND** it does not call Slack, email, SMS or CommunicationApi directly

### Requirement: Batch failure event

The system SHALL publish a `BatchFailedEvent` containing:

- event id;
- occurred at;
- job name;
- optional step name;
- execution id or fallback correlation component;
- severity;
- reason;
- facts;
- correlation key.

#### Scenario: Spring Batch job execution fails

- **GIVEN** a Spring Batch job execution fails
- **WHEN** the alert aspect/listener handles failure
- **THEN** the event correlation key is `batch:{jobName}:{executionId}:failed`

### Requirement: Platform notification listens to batch event

`platform.notification` SHALL listen to `BatchFailedEvent` and create an ops in-app notification when policy requires it.

#### Scenario: Batch failure event consumed by notification

- **GIVEN** a `BatchFailedEvent`
- **WHEN** notification policy maps it
- **THEN** a PLATFORM target notification is created

### Requirement: Platform communication listens to batch event

`platform.communication` SHALL listen to `BatchFailedEvent` and enqueue internal Slack/email based on policy.

#### Scenario: Batch failure event consumed by communication

- **GIVEN** a `BatchFailedEvent`
- **WHEN** communication policy maps it
- **THEN** a `SLACK_INTERNAL` outbound message is enqueued
- **AND** provider delivery occurs later through the dispatcher

### Requirement: No direct provider gateway imports

No class outside `platform.communication.internal.adapter` SHALL import direct provider gateway classes.

#### Scenario: Architecture test runs

- **GIVEN** a scheduler imports `SlackGateway`
- **WHEN** ArchUnit tests run
- **THEN** the build fails
