# common-communication Specification

## ADDED Requirements

### Requirement: Generic outbound communication primitives

`common.communication` SHALL provide generic external communication primitives that are independent from notification-center business concepts.

#### Scenario: External channels only

- **WHEN** `CommunicationChannel` is used
- **THEN** it SHALL include external transport channels such as `EMAIL`, `SMS`, `WHATSAPP`, and `SLACK`
- **AND** it SHALL NOT include `IN_APP`.

#### Scenario: Common communication imports

- **WHEN** classes under `common.communication` are compiled
- **THEN** they SHALL NOT import `core.notification`, `core.sales`, `features.*`, or catalog internals.

### Requirement: Edge communication gateway

`common.communication.edge` SHALL contain the Spring-side HTTP adapter used to call tchalanet-edge-service for external communications.

#### Scenario: HMAC signed send

- **GIVEN** an `OutboundMessageRequest`
- **WHEN** `OutboundMessageGateway.send(...)` is invoked
- **THEN** the adapter SHALL map it to an edge request
- **AND** sign the mapped request using `EdgeHmacSigner`
- **AND** POST `signed.rawJsonBody()` to the edge-service
- **AND** include `X-Tch-Timestamp` and `X-Tch-Signature`.

#### Scenario: Idempotency and request ID headers

- **WHEN** an outbound message is sent
- **THEN** the adapter SHALL include `X-Request-Id`
- **AND** include `Idempotency-Key`.

### Requirement: Edge path target

The canonical edge path for communication SHALL be `/internal/messages/send`.

#### Scenario: New canonical path

- **WHEN** server configuration is updated
- **THEN** it SHALL be possible to set `tch.communication.edge.messages-path=/internal/messages/send`.

#### Scenario: Legacy notification path is not used

- **WHEN** server common communication is configured
- **THEN** it SHALL target `messagesPath`
- **AND** it SHALL NOT fall back to `/internal/notifications/send`.
