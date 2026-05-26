# Spec — auth-context

## ADDED Requirements

### Requirement: HTTP context is canonical

All authenticated HTTP requests SHALL create a canonical `TchRequestContext` via `TchContextFilter`.

#### Scenario: Tenant scoped request

- **GIVEN** an authenticated user with a tenant claim
- **WHEN** the user calls a `/tenant/**` endpoint
- **THEN** the request context contains the effective tenant and actor
- **AND** the tenant is not read from the request body

### Requirement: Operational context is attached early and validated late

The system SHALL attach an `OperationalRequestContext` during request context creation when operational headers are present, but SHALL validate it per sensitive action.

#### Scenario: Client claim only

- **GIVEN** a request with `X-Terminal-Id` and no valid binding
- **WHEN** the context is built
- **THEN** operational context source is `CLIENT_CLAIM`
- **AND** `trustedOperationalContextRequired()` fails for a sale

#### Scenario: Signed binding

- **GIVEN** a request with a valid terminal binding
- **WHEN** the context is built
- **THEN** operational context source is `SIGNED_DEVICE_BINDING`
- **AND** sensitive operations may proceed to action-specific validation

### Requirement: Local biometric auth is never server identity

The backend SHALL NOT accept biometrics as identity or permission proof.

#### Scenario: Flutter local auth success

- **GIVEN** Face ID succeeds locally
- **WHEN** Flutter calls the API
- **THEN** the API still requires a valid Keycloak bearer token
- **AND** terminal/session/permission validation still applies

