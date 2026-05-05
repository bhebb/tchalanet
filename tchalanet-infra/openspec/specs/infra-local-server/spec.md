# infra-local-server Specification

## Purpose

TBD - created by archiving change infra-refactor-local-server-v0. Update Purpose after archive.

## Requirements

### Requirement: P0 strict services

The infra SHALL define P0 strict as Traefik, PostgreSQL, and Keycloak.

#### Scenario: Start P0

- **WHEN** `make p0-up ENV=dev` is executed
- **THEN** Traefik, PostgreSQL, and Keycloak SHALL start
- **AND** Redis, API, Edge, and Web SHALL NOT be required for P0 success

#### Scenario: Smoke P0

- **WHEN** `make p0-smoke ENV=dev` is executed after P0 startup
- **THEN** PostgreSQL readiness SHALL be checked
- **AND** Keycloak OIDC discovery SHALL be checked
- **AND** Traefik route availability SHALL be checked

### Requirement: Redis P0+

Redis SHALL be part of P0+ and SHALL be internal only.

#### Scenario: Redis networks

- **WHEN** Redis compose is rendered
- **THEN** Redis SHALL be attached only to the `back` Docker network
- **AND** Redis SHALL NOT be attached to `edge`

#### Scenario: Redis password healthcheck

- **WHEN** `REDIS_PASSWORD` is set
- **THEN** the Redis healthcheck SHALL authenticate before pinging

### Requirement: server-v0 services

server-v0 SHALL include Traefik, PostgreSQL, Keycloak, Redis, API, Edge, and Web.

#### Scenario: Start staging v0

- **WHEN** `make up-staging ENV=staging` is executed
- **THEN** the server-v0 services SHALL be started
- **AND** post-v0 services SHALL NOT be required

### Requirement: PostgreSQL isolation

PostgreSQL SHALL be internal-only in staging/prod.

#### Scenario: Staging compose

- **WHEN** staging Compose is validated
- **THEN** PostgreSQL SHALL be attached to `back`
- **AND** it SHALL NOT publish port `5432` to the host

### Requirement: Keycloak realm overlays

Keycloak realm generation SHALL be environment overlay based.

#### Scenario: Generate staging realm

- **WHEN** `get-realm.sh staging` runs
- **THEN** it SHALL generate valid JSON
- **AND** the final realm JSON SHALL NOT contain local users

#### Scenario: Generate dev realm

- **WHEN** `get-realm.sh dev` runs
- **THEN** local users MAY be included
- **AND** localhost/localtest redirect URIs MAY be included

### Requirement: Staging disposable

Non-production staging SHALL be disposable to control cost.

#### Scenario: Destroy staging

- **WHEN** `make staging-destroy` is executed
- **THEN** the command SHALL require explicit confirmation
- **AND** it SHALL perform or require a PostgreSQL backup unless an explicit override is provided
- **AND** it SHALL NOT destroy production resources

#### Scenario: Recreate staging

- **WHEN** `make staging-create` followed by `make staging-restore-latest` and `make staging-up` is executed
- **THEN** staging SHALL become operational from infrastructure scripts and stored backups/secrets

### Requirement: Vercel free usage boundary

Client-facing staging and prod deployments SHALL NOT use Vercel Free.

#### Scenario: Client staging/prod

- **WHEN** a client-facing staging or prod deployment is needed
- **THEN** it SHALL use Hetzner Docker web or a paid Vercel plan
- **AND** it SHALL NOT rely on Vercel Free
