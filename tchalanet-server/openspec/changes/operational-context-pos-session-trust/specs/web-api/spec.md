# Specification: web-api

## ADDED Requirements

### Requirement: POS HTTP bodies contain business payload only

The system SHALL keep POS operational frame IDs out of tenant POS HTTP request bodies.

#### Scenario: Sell ticket request body omits POS IDs

- **GIVEN** a tenant POS sell-ticket endpoint
- **WHEN** the client sends a request
- **THEN** terminal, outlet, and session are supplied through headers
- **AND** the body contains only sale payload fields

### Requirement: Protected POS endpoints use context headers

The system SHALL document POS context headers in OpenAPI for protected POS endpoints.

#### Scenario: Swagger shows POS headers

- **GIVEN** a POS endpoint that needs terminal/outlet/session
- **WHEN** OpenAPI is generated
- **THEN** the endpoint documents `X-Tch-Terminal-Id`, `X-Tch-Outlet-Id`, and `X-Tch-Sales-Session-Id` as headers
- **AND** does not document them as body fields
