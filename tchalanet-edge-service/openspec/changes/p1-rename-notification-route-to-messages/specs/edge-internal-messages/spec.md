# edge-internal-messages Specification

## ADDED Requirements

### Requirement: Internal messages endpoint

The edge-service SHALL expose `POST /internal/messages/send` for internal outbound message delivery.

#### Scenario: Valid signed message request

- **GIVEN** a valid HMAC-signed request
- **WHEN** the server posts to `/internal/messages/send`
- **THEN** edge-service SHALL accept the request
- **AND** dispatch it using message delivery logic.

### Requirement: Legacy notification endpoint is removed

The edge-service SHALL NOT expose `POST /internal/notifications/send`.

#### Scenario: Legacy route is unavailable

- **WHEN** the server posts to `/internal/notifications/send`
- **THEN** edge-service SHALL reject it as an unknown route.

### Requirement: HMAC behavior protects internal messages

The internal messages route SHALL use HMAC verification.

#### Scenario: Invalid signature

- **WHEN** `/internal/messages/send` receives an invalid signature
- **THEN** it SHALL reject the request.

#### Scenario: Raw body verification

- **WHEN** `/internal/messages/send` verifies a request
- **THEN** it SHALL verify the signature against the raw request body
- **AND** SHALL NOT verify against a reserialized JSON body.

#### Scenario: Required HMAC headers

- **WHEN** `/internal/messages/send` receives a signed request
- **THEN** it SHALL require `X-Request-Id`
- **AND** it SHALL require `Idempotency-Key`
- **AND** it SHALL require `X-Tch-Timestamp`
- **AND** it SHALL require `X-Tch-Signature`.

#### Scenario: Spring-compatible signature formula

- **WHEN** `/internal/messages/send` verifies `X-Tch-Signature`
- **THEN** it SHALL verify `sha256=<hex>` where the HMAC input is `X-Tch-Timestamp + "." + raw_json_body`.

### Requirement: HMAC curl documentation

The edge-service SHALL document how to call the internal messages route with HMAC headers for local/manual testing.

#### Scenario: Developer runs signed curl

- **WHEN** a developer opens the internal messages curl documentation
- **THEN** it SHALL show a signed `curl` for `/internal/messages/send`
- **AND** it SHALL show how to compute `X-Tch-Signature`
- **AND** it SHALL state that `/internal/notifications/send` is removed.

### Requirement: No public exposure

The messages endpoint SHALL remain internal-only.

#### Scenario: Web/mobile clients

- **WHEN** web or mobile needs to send a message
- **THEN** they SHALL call Spring Boot
- **AND** SHALL NOT call edge-service directly.
