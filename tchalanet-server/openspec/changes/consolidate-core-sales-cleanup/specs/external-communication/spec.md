# external-communication Specification

## ADDED Requirements

### Requirement: External sending belongs to common.communication

SMS, Slack, email, and WhatsApp sending MUST be extracted from notification into `common.communication`.

#### Scenario: External message is sent

- **WHEN** a Spring feature needs to send SMS, Slack, email, or WhatsApp
- **THEN** it uses `common.communication`
- **AND** it does not call edge-service directly from web/mobile clients.

### Requirement: Notification is in-app only

`core.notification` MUST own in-app notification center behavior and MUST NOT own global external transport.

#### Scenario: In-app notification is created

- **WHEN** an in-app notification is requested
- **THEN** `core.notification` handles the notification center state
- **AND** no edge-service call is made.

#### Scenario: External notification channel is requested

- **WHEN** a flow asks for SMS, Slack, email, or WhatsApp
- **THEN** the request is mapped to `common.communication`
- **AND** `IN_APP` is never sent to edge-service.

### Requirement: No ticketdelivery feature

The sales cleanup MUST NOT introduce or keep `features.ticketdelivery` as a target feature.

#### Scenario: Ticket-related external message is needed

- **WHEN** a cashier or ticket flow needs to send a receipt link or external message
- **THEN** it uses `common.communication`
- **AND** any receipt artifact or link comes from the sales receipt/read model plus `common.document`
- **AND** it does not call `features.receipt` or an internal Spring HTTP endpoint.

### Requirement: Canonical edge messages route

Spring-side external communication MUST target the edge-service canonical messages route.

#### Scenario: Outbound message is sent to edge

- **WHEN** the Spring communication gateway sends an external message
- **THEN** the gateway targets `/internal/messages/send`
- **AND** `/internal/notifications/send` is not used
- **AND** HMAC signing preserves raw-body semantics.
