# notification spec delta

## ADDED Requirements

### Requirement: Edge notification gateway

`tchalanet-server` SHALL provide a single notification gateway adapter for calls to `tchalanet-edge-service`.

#### Scenario: Send Slack notification through edge-service

- **GIVEN** edge notification integration is enabled
- **AND** a payload targets channel `SLACK` with `channelKey=batch-draws`
- **WHEN** the server sends the notification
- **THEN** it SHALL POST to `/internal/notifications/send`
- **AND** the request body SHALL include recipient `{ "channel": "SLACK", "channelKey": "batch-draws" }`
- **AND** the request SHALL include HMAC headers

#### Scenario: Integration disabled

- **GIVEN** `tch.notification.edge.enabled=false`
- **WHEN** a notification is sent
- **THEN** the adapter SHALL not call edge-service
- **AND** it SHALL not throw for normal disabled behavior

### Requirement: HMAC for internal edge calls

All internal calls from `tchalanet-server` to `tchalanet-edge-service` SHALL be signed.

#### Scenario: Signed body

- **GIVEN** a fixed timestamp, secret and JSON body
- **WHEN** the adapter signs the request
- **THEN** the signature SHALL be HMAC-SHA256 of `timestamp + "." + rawJsonBody`

### Requirement: Batch technical notification policy

Batch technical notifications SHALL be centralized and noise-controlled.

#### Scenario: Failed batch sends notification

- **GIVEN** a `BatchNotification` with status `FAILED`
- **WHEN** it is evaluated by `BatchNotificationPolicy`
- **THEN** it SHALL be allowed if not inside cooldown

#### Scenario: Started batch is silent

- **GIVEN** a `BatchNotification` with status `STARTED`
- **WHEN** it is evaluated by `BatchNotificationPolicy`
- **THEN** it SHALL not be sent

#### Scenario: Succeeded batch is silent

- **GIVEN** a `BatchNotification` with status `SUCCEEDED`
- **WHEN** it is evaluated by `BatchNotificationPolicy`
- **THEN** it SHALL not be sent

#### Scenario: Gate disabled skipped batch sends notification

- **GIVEN** a `BatchNotification` with status `SKIPPED` and code `gate_disabled`
- **WHEN** it is evaluated by `BatchNotificationPolicy`
- **THEN** it SHALL be allowed if not inside cooldown

#### Scenario: Other skipped batch is silent

- **GIVEN** a `BatchNotification` with status `SKIPPED` and code other than `gate_disabled`
- **WHEN** it is evaluated by `BatchNotificationPolicy`
- **THEN** it SHALL not be sent
