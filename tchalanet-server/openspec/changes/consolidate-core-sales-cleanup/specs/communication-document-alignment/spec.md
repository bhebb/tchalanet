# communication-document-alignment Specification

## ADDED Requirements

### Requirement: Sales cleanup follows common communication/document extraction

The sales cleanup MUST consume the boundaries defined by `p0-extract-common-communication-document` instead of defining parallel common print or communication abstractions.

#### Scenario: Receipt rendering needs generic primitives

- **WHEN** sales receipt rendering needs PDF, QR, ESC/POS, or generic receipt models
- **THEN** it SHALL use `common.document`
- **AND** `core.sales` SHALL keep only ticket-specific receipt/read model and formatter logic.

#### Scenario: External message is requested

- **WHEN** a cashier or ticket flow sends EMAIL, SMS, WHATSAPP, or SLACK-compatible messages
- **THEN** it SHALL use `common.communication`
- **AND** it SHALL NOT send notification-center `IN_APP` messages to edge-service.

### Requirement: Edge-service messages route alignment

Spring-side outbound communication MUST align with the edge-service canonical messages route from `p1-rename-notification-route-to-messages`.

#### Scenario: Canonical edge route is available

- **WHEN** the edge-service exposes `/internal/messages/send`
- **THEN** Spring communication configuration SHALL target that route
- **AND** `/internal/notifications/send` SHALL NOT be used.

#### Scenario: HMAC behavior

- **WHEN** Spring sends an outbound message to edge-service
- **THEN** it SHALL preserve the signed raw JSON body behavior and HMAC headers defined by the common communication change.
