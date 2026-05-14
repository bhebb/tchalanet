# Spec: Tchalanet Common Organization

## ADDED Requirements

### Requirement: Common SHALL be a Technical Shared Kernel

`tchalanet-common` SHALL contain only technical primitives, stable contracts, and low-level helpers shared by other modules.

#### Scenario: A class has business lifecycle or workflow

- **WHEN** a class owns a business workflow, lifecycle, business policy, table-backed application state, external gateway, or application runtime configuration
- **THEN** it SHALL NOT be placed in `tchalanet-common`.

#### Scenario: A class is a pure primitive or contract

- **WHEN** a class is a pure Java primitive/contract used by multiple layers and has no business policy
- **THEN** it MAY be placed in `tchalanet-common`.

### Requirement: Common SHALL NOT own Spring Batch runtime

`tchalanet-common` SHALL NOT depend on Spring Batch, configure Spring Batch, launch jobs, bind Spring Batch `JobParameters`, or own job registries/gates.

#### Scenario: A class imports Spring Batch

- **WHEN** a class imports `org.springframework.batch..`
- **THEN** it SHALL be moved to `tchalanet-app` or a future `platform.job` capability.

#### Scenario: Job parameters must be read in common

- **WHEN** common needs a job parameter reader
- **THEN** the reader SHALL operate on `Map<String,String>` or another pure Java contract, not Spring Batch `JobParameters`.

### Requirement: Common SHALL permit only a generic job annotation

`common.job.annotation.TchJob` SHALL remain declarative and SHALL NOT own launch, locking, scheduling, or notification behavior.

#### Scenario: Job behavior is needed around `@TchJob`

- **WHEN** runtime behavior is needed around `@TchJob` such as monitoring, notification, locking, or launch orchestration
- **THEN** the aspect/service SHALL live outside common.

### Requirement: Common SHALL NOT own external HTTP client runtime config

`tchalanet-common` SHALL NOT provide global `WebClient`, `RestClient`, provider-specific HTTP clients, or external-client runtime configuration.

#### Scenario: A shared HTTP builder is required

- **WHEN** a shared HTTP client builder is required
- **THEN** it SHALL be configured in `tchalanet-app` or in the owning platform/core infra package.

### Requirement: Common SHALL separate cache contracts from runtime implementation

`common.cache` MAY contain cache names, specs, and key builders, but SHALL NOT own Redis/Lettuce runtime configuration.

#### Scenario: Redis or Caffeine managers are configured

- **WHEN** a class configures Redis, Lettuce, Caffeine managers, or composite cache managers
- **THEN** it SHALL live in app/platform runtime configuration.

### Requirement: Common SHALL keep web contracts but not Data REST runtime

`common.web` SHALL contain only shared web contracts/helpers such as `ApiResponse`, `ProblemRest`, paging types/resolvers, response advice, and typed ID converters; it SHALL NOT contain Spring Data REST runtime configuration or repository exposure policy.

#### Scenario: Spring Data REST configuration is present

- **WHEN** a class configures Spring Data REST
- **THEN** it SHALL live in app/catalog/platform runtime configuration, not common.

### Requirement: Common SHALL keep typed IDs simple

`common.types.id` SHALL contain only simple typed ID wrappers for construction, parsing, and nullability; it SHALL NOT contain business behavior.

#### Scenario: A typed ID contains business logic

- **WHEN** an `XxxId` wrapper contains business logic beyond construction/parsing/nullability
- **THEN** the logic SHALL be removed or moved to the owning domain.

### Requirement: Common SHALL NOT own business enums

Business enums SHALL live in their owning `core`, `catalog`, or `platform` API package.

#### Scenario: An enum describes sales, draw, payout, audit, notification, idempotency, tenant lifecycle, or game rules

- **WHEN** the enum represents a business concept
- **THEN** it SHALL be moved out of `common.types.enums` to the owning module.

### Requirement: Common context SHALL not validate operational domain state

`common.context` MAY attach request context and operational context claims/hints, but SHALL NOT validate terminal/outlet/session eligibility.

#### Scenario: A sell/payout/offline action needs operational validation

- **WHEN** an action requires terminal/outlet/session/seller validation
- **THEN** the handler SHALL call the owning core queries/validators and SHALL NOT rely on common context alone.

### Requirement: Common POM SHALL stay minimal

`tchalanet-common/pom.xml` SHALL only include dependencies required by remaining common code.

#### Scenario: No remaining common class imports a heavy framework

- **WHEN** no remaining common class imports Spring Batch, ShedLock, WebFlux, Redis, Data REST, QueryDSL, OAuth JOSE, or json-schema-validator
- **THEN** the corresponding dependency SHALL be removed from the common POM.
