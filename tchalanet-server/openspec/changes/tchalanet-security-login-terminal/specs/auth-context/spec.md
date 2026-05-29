# Spec — auth-context

## ADDED Requirements

### Requirement: HTTP context is canonical

All authenticated HTTP requests SHALL create a canonical `TchRequestContext` through the existing request-context pipeline.

#### Scenario: Tenant scoped request

- **GIVEN** an authenticated user with tenant context
- **WHEN** the user calls a `/tenant/**` endpoint
- **THEN** the request context contains the effective tenant and actor
- **AND** the tenant is not trusted from the request body

### Requirement: Operational context is attached early and validated late

The system SHALL attach an `OperationalRequestContext` during request context creation when operational headers are present, while deferring sensitive validation to the action.

#### Scenario: Client claim only

- **GIVEN** a request with `X-Terminal-Id`
- **AND** no valid signed binding or server-side selection exists
- **WHEN** the request context is built
- **THEN** the operational context source is `CLIENT_CLAIM`
- **AND** `trustedOperationalContextRequired()` rejects transaction use

#### Scenario: Signed device binding

- **GIVEN** a request with a valid terminal binding token
- **WHEN** the request context is built
- **THEN** the operational context source is `SIGNED_DEVICE_BINDING`
- **AND** sensitive operations may proceed to action-specific terminal/session validation

### Requirement: Idempotency key is request context data

The request pipeline SHALL expose `Idempotency-Key` to downstream idempotency enforcement without requiring controllers to parse it manually.

#### Scenario: Sale request with key

- **GIVEN** a ticket sale request with `Idempotency-Key`
- **WHEN** the request reaches the sale endpoint
- **THEN** the idempotency layer can read the key from request context or request headers consistently

### Requirement: Local biometric auth is never server identity

The backend SHALL NOT accept local biometrics as identity, permission, terminal trust, or transaction authorization proof.

#### Scenario: Flutter local auth success

- **GIVEN** Face ID or device PIN succeeds locally
- **WHEN** Flutter calls the API
- **THEN** the API still requires a valid Keycloak bearer token
- **AND** terminal, binding, session, permission, entitlement, and idempotency validation still apply
