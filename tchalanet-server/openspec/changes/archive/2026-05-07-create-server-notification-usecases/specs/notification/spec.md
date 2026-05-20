# notification use cases spec delta

## ADDED Requirements

### Requirement: Controlled notification command

The server SHALL provide a controlled command for sending notifications through the configured gateway.

#### Scenario: Valid Slack command

- **GIVEN** a command with recipient channel `SLACK` and `channelKey=batch-draws`
- **WHEN** the command is handled
- **THEN** policy validation SHALL pass
- **AND** the gateway SHALL be called

#### Scenario: Invalid Slack command

- **GIVEN** a command with recipient channel `SLACK` and no `channelKey`
- **WHEN** the command is handled
- **THEN** policy validation SHALL fail
- **AND** the gateway SHALL not be called

#### Scenario: Invalid Email command

- **GIVEN** a command with recipient channel `EMAIL` and no `to`
- **WHEN** the command is handled
- **THEN** policy validation SHALL fail
- **AND** the gateway SHALL not be called

### Requirement: Ops notification test endpoint

The server SHALL expose a superadmin-only endpoint for testing notification delivery.

#### Scenario: Superadmin sends test Slack notification

- **GIVEN** the caller is SUPER_ADMIN
- **AND** edge notification integration is enabled
- **WHEN** the caller posts a Slack test notification
- **THEN** the server SHALL call `SendNotificationCommand`
- **AND** the command SHALL send through edge-service

#### Scenario: Non-superadmin cannot use ops test endpoint

- **GIVEN** the caller is not SUPER_ADMIN
- **WHEN** the caller posts to `/api/v1/ops/notifications/test`
- **THEN** access SHALL be denied

### Requirement: No generic client send endpoint

The server SHALL NOT expose a free-form notification endpoint to regular web/mobile users.

#### Scenario: Client needs ticket delivery

- **GIVEN** a web/mobile user wants to send a ticket
- **WHEN** the client calls the server
- **THEN** the client SHALL use a business endpoint such as `/tickets/{ticketId}/send`
- **AND** Spring Boot SHALL decide the notification details
