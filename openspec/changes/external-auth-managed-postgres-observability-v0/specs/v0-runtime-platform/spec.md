# V0 Runtime Platform Specification

## ADDED Requirements

### Requirement: Provider-neutral external authentication

The backend SHALL verify external authentication through `platform.identity.api` and SHALL keep
provider-specific implementation details internal to `platform.identity`.

#### Scenario: Firebase-authenticated production request

- **GIVEN** a valid Firebase ID token
- **WHEN** the request enters the API
- **THEN** the Firebase adapter SHALL verify the external identity
- **AND** the external subject SHALL map to an application-owned `AppUser`
- **AND** downstream modules SHALL consume Tchalanet identity/context APIs only

#### Scenario: Provider details remain internal

- **WHEN** a controller, handler, core module, feature module, or other platform module is compiled
- **THEN** it SHALL NOT depend on Firebase-specific or Keycloak-specific internal classes

### Requirement: Tchalanet-owned authorization and context

Tchalanet SHALL remain the authority for memberships, roles, permissions, operational context,
audit, tenant override, and `TchRequestContext`.

#### Scenario: Valid external user lacks application permission

- **GIVEN** an externally authenticated user
- **AND** the mapped `AppUser` lacks the required Tchalanet permission
- **WHEN** the user invokes a protected operation
- **THEN** Tchalanet SHALL deny the operation

#### Scenario: Client supplies tenant context

- **GIVEN** a client-supplied tenant identifier
- **WHEN** trusted context is resolved
- **THEN** the effective tenant SHALL come from validated Tchalanet state
- **AND** the client value SHALL NOT directly bind RLS context

### Requirement: Mandatory PostgreSQL RLS

All tenant-scoped production and test flows SHALL bind validated Tchalanet context to PostgreSQL
and SHALL remain subject to RLS.

#### Scenario: Identity provider changes

- **WHEN** a request is authenticated through Firebase, local JWT, local perf, or transitional
  Keycloak
- **THEN** it SHALL use the same application context and RLS binding path

#### Scenario: Pooled connection is reused

- **WHEN** a database connection is reused across requests or transactions
- **THEN** tenant/user RLS context SHALL NOT leak between them

### Requirement: Production rejects local identity modes

Production SHALL fail closed when a local or test identity mode is configured.

#### Scenario: Production starts with local identity

- **GIVEN** the effective production environment
- **WHEN** `local-jwt`, `local-perf`, or an equivalent test-auth path is configured
- **THEN** application startup SHALL fail

### Requirement: Managed PostgreSQL production source of truth

Production V0 SHALL use managed PostgreSQL while retaining standard PostgreSQL JDBC, Flyway, and
provider-neutral application behavior.

#### Scenario: Production database is accepted

- **WHEN** the managed PostgreSQL service is approved for go-live
- **THEN** backups and PITR SHALL be enabled
- **AND** a restore rehearsal SHALL have succeeded
- **AND** Flyway and RLS acceptance checks SHALL have succeeded

### Requirement: Redis remains non-authoritative

Redis SHALL NOT be a source of truth for money, tickets, payouts, identity authorization, audit, or
RLS context.

#### Scenario: Redis is unavailable

- **WHEN** Redis becomes unavailable
- **THEN** authoritative business and security state SHALL remain recoverable from Tchalanet and
  PostgreSQL sources of truth

### Requirement: OpenTelemetry critical-flow visibility

The runtime SHALL emit sanitized OpenTelemetry traces for critical technical and business flows.

#### Scenario: Representative critical flow executes in development or staging

- **WHEN** identity verification, authorization, RLS binding, ticket sale, payout, or offline sync
  executes
- **THEN** its trace SHALL be exportable through the configured OpenTelemetry path
- **AND** it SHALL NOT expose raw tokens, secrets, or prohibited sensitive data

