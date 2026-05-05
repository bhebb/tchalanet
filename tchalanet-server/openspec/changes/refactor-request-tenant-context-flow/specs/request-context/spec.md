# request-context Specification Delta

## ADDED Requirements

### Requirement: HTTP request context SHALL resolve tenant explicitly by API scope

The server SHALL resolve the effective tenant for each HTTP request from an explicit policy that
accounts for API scope, authentication state, JWT tenant claims, and authorized tenant override.

#### Scenario: Public anonymous request

- **GIVEN** an anonymous request targets a `PUBLIC` endpoint
- **WHEN** the request context is created
- **THEN** the effective tenant SHALL be the configured default public tenant
- **AND** the default public tenant code SHALL be `tchalanet` unless configuration explicitly changes it

#### Scenario: Public authenticated request with tenant claim

- **GIVEN** an authenticated request targets a `PUBLIC` endpoint
- **AND** the JWT contains a tenant claim
- **WHEN** the request context is created
- **THEN** the effective tenant SHALL come from the JWT tenant claim
- **AND** the default public tenant SHALL NOT override the authenticated tenant

#### Scenario: Public authenticated request without tenant claim

- **GIVEN** an authenticated request targets a `PUBLIC` endpoint
- **AND** the JWT does not contain a tenant claim
- **WHEN** the request context is created
- **THEN** the effective tenant SHALL fall back to the configured default public tenant

#### Scenario: Tenant or admin request without tenant

- **GIVEN** a request targets a `TENANT` or `ADMIN` endpoint
- **AND** no tenant can be resolved from the authenticated context or an allowed override
- **WHEN** the request context is created
- **THEN** the request SHALL be rejected before application handlers execute

#### Scenario: Platform request without tenant

- **GIVEN** a request targets a `PLATFORM` endpoint
- **WHEN** the request context is created
- **THEN** the context SHALL NOT bind the default public tenant implicitly
- **AND** the request SHALL remain platform-scoped unless an explicit tenant policy applies

#### Scenario: Super-admin tenant override

- **GIVEN** an authenticated super-admin request includes an allowed tenant override header
- **WHEN** the request context is created
- **THEN** the effective tenant SHALL be the override tenant
- **AND** the context SHALL record that the tenant source is an override
- **AND** non-super-admin requests with the same override header SHALL be rejected

### Requirement: Context binding SHALL restore previous context after temporary tenant switches

Temporary context switching SHALL be stack-safe. Any helper that binds a temporary
`TchRequestContext` SHALL restore the previously bound context after the work finishes.

#### Scenario: Temporary switch inside an HTTP request

- **GIVEN** an HTTP request context is already bound
- **WHEN** code runs a temporary tenant-scoped operation
- **THEN** the temporary context SHALL be visible only during that operation
- **AND** the original HTTP request context SHALL be restored afterward
- **AND** the original context SHALL NOT be cleared accidentally

#### Scenario: Temporary switch without previous context

- **GIVEN** no context is currently bound
- **WHEN** code runs a temporary tenant-scoped operation
- **THEN** the temporary context SHALL be visible only during that operation
- **AND** no context SHALL remain bound afterward

### Requirement: Context creation SHALL be explicit at system boundaries

The server SHALL create context at system boundaries using a source-specific policy instead of a
generic tenant runner.

#### Scenario: HTTP request context is created

- **GIVEN** an HTTP request enters the API
- **WHEN** the context pipeline runs
- **THEN** user bootstrap MAY enrich the request with an application user id
- **AND** the canonical HTTP context SHALL be created by the context filter/factory
- **AND** actor enrichment SHALL NOT decide the effective tenant

#### Scenario: PageModel template startup seed runs

- **GIVEN** PageModel templates are seeded from classpath resources
- **WHEN** the catalog template seed runner executes
- **THEN** it SHALL run as global catalog startup work
- **AND** it SHALL NOT bind the default public tenant unless a documented persistence requirement needs a platform/startup context

#### Scenario: PageModel tenant startup seed runs

- **GIVEN** tenant PageModels are created from PageModel templates at startup
- **WHEN** the onboarding runner executes for the default tenant
- **THEN** it SHALL bind an explicit startup tenant context
- **AND** the context source SHALL distinguish startup tenant work from HTTP public default tenant work

#### Scenario: Keycloak bootstrap sync runs

- **GIVEN** Keycloak bootstrap sync is enabled
- **WHEN** the application-ready listener runs
- **THEN** it SHALL be treated as startup/platform work
- **AND** it SHALL NOT bind a tenant context unless the operation is explicitly tenant-scoped

#### Scenario: Scheduler launches batch work

- **GIVEN** a scheduler tick needs to run tenant or platform work
- **WHEN** the scheduler launches a batch job
- **THEN** the batch context binder SHALL create the execution context from explicit job parameters
- **AND** the scheduler SHALL NOT rely on an ambient HTTP ThreadLocal

#### Scenario: Event may run outside original request thread

- **GIVEN** an event listener may execute asynchronously, after retry, or outside the original request thread
- **WHEN** the listener needs tenant, actor, or correlation information
- **THEN** the event payload SHALL carry that information explicitly
- **AND** the listener SHALL NOT depend on inherited ThreadLocal context

### Requirement: RLS context SHALL match the effective request context

The RLS datasource bridge SHALL derive database session variables from the canonical current
`TchRequestContext`.

#### Scenario: Public default tenant reaches RLS

- **GIVEN** a public anonymous request binds the default public tenant
- **WHEN** tenant-scoped persistence code opens a connection
- **THEN** RLS session variables SHALL include the effective tenant id
- **AND** RLS SHALL use `PUBLIC` API scope

#### Scenario: Platform request reaches RLS

- **GIVEN** a platform-scoped request has no effective tenant
- **WHEN** persistence code opens a connection
- **THEN** RLS session variables SHALL not contain a tenant id
- **AND** RLS SHALL use `PLATFORM` API scope

#### Scenario: Batch tenant context reaches RLS

- **GIVEN** a tenant-scoped batch job binds a tenant context
- **WHEN** persistence code opens a connection
- **THEN** RLS session variables SHALL include the batch tenant id
- **AND** RLS SHALL use the tenant-scoped batch context
