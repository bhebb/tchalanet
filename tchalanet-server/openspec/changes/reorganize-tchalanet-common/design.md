# Design: `tchalanet-common` organization

## Boundary rule

`common` is not "all shared code". It is only the Technical Shared Kernel.

A class can remain in `common` only if all are true:

- It is technical or infrastructural-contract level.
- It has no domain lifecycle.
- It has no business policy.
- It does not own a business table or workflow.
- It does not configure application runtime assembly.
- It does not call external gateways.
- It does not depend on platform/core/catalog/features.
- It can compile without Spring Batch, ShedLock, WebFlux, Data REST, Redis, QueryDSL/JPA starter, or external-client dependencies unless explicitly approved.

## Target package map

### Keep in `common`

```text
common.bus
common.event
common.event.infra.spring                 # MVP Spring event publisher is acceptable for now
common.job.annotation                      # TchJob only
common.job.exception                       # generic job exceptions only
common.job.key                             # JobKey only; domain-specific key constants need review
common.job.params                          # pure Java keys/reader only, no Spring Batch types
common.context                             # TchContext, TchRequestContext, context contracts
common.context.web                         # HTTP context filter/resolver/annotation if kept as canonical web boundary
common.context.tenant                      # tenant lookup interface only
common.context.system                      # system properties only if no platform runtime dependency
common.context.operational                 # attached operational claim/hint only, no domain validation
common.web.api
common.web.advice
common.web.error
common.web.paging
common.web.converter                       # typed ID string converters
common.json                                # typed ID Jackson support only
common.mapper                              # CommonIdMapper only
common.types.id                            # simple UUID wrappers only
common.types.money                         # Money/Currency/Percent primitives
common.types.time                          # DateRange/TimeWindow primitives
common.types.codes                         # only if pure value objects; SelectionKey may need owner review
common.time                                # Clock/timezone/date helpers
common.tx
common.stereotype
```

### Move out of `common`

```text
common.batch.*                             -> app.job/app.batch or platform.job
common.client.http.*                       -> app.config.http or platform.<capability>.internal.client
common.cache.RedisConfig                   -> app/platform cache runtime config
common.cache.CacheConfig                   -> app/platform if it builds concrete managers
common.cache.CombinedCache*                -> app/platform if it depends on Caffeine/Redis runtime
common.web.config.DataRestConfig           -> app/catalog/platform config
common.persistence.config.*                -> app persistence config
common.persistence.BaseEntity/BaseTenantEntity/AuditableEntity -> keep only if project approves shared base entities; otherwise platform/common-persistence ADR required
common.security.Permissions                -> platform.accesscontrol.api/model or owning domains
common.types.enums.* business enums        -> owning module
common.selection.*                         -> catalog.game or core.sales/drawresult/limitpolicy
common.util.JsonSchemaValidatorUtil        -> platform.document/schema or owning capability
common.util.RoleUtils                      -> platform.accesscontrol or identity
common.util.JsonUtils/ObjectMapper holders -> keep only if pure; runtime holders/config should move to app/json config
```

## Batch/job redesign

`common` owns only the language:

```text
common.job.annotation.TchJob
common.job.key.JobKey
common.job.exception.JobSkippedException
common.job.params.JobParamKeys
common.job.params.JobParamReader     # Map<String,String>, pure Java
```

`app` owns Spring Batch and runtime assembly:

```text
app.config.batch.BatchRuntimeConfig
app.config.scheduler.ShedLockRuntimeConfig
app.job.aspect.TchJobAspect
app.job.registry.RegisteredJob
app.job.registry.TchBatchJobRegistry
app.job.params.JobParamsValidator
app.batch.params.SpringBatchJobParams
app.batch.context.BatchTchContextBinder
app.batch.context.BatchJobExecutionListener
app.batch.launch.BatchJobStarter
app.batch.gate.BatchGate
app.batch.gate.BatchGateResolver
app.batch.gate.BatchGateFlagStore
app.batch.gate.BatchGateCache
app.batch.notification.BatchEventNotificationService
```

Later, if this becomes reusable platform functionality, move it from app to `platform.job` with an API/internal split.

## Enums policy

### IDs

For this change, keep all `XxxId` wrappers in `common.types.id`, provided they remain simple UUID records with no business methods.

### Enums

Business enums must move to owner packages:

- sales/ticket/offline enums -> `core.sales.api.model` or `core.offlinesync.api.model`;
- payout enums -> `core.payout.api.model`;
- draw/result enums -> `core.draw.api.model` / `core.drawresult.api.model`;
- catalog/game/pricing enums -> `catalog.game.api.model` / `catalog.pricing.api.model`;
- audit enums -> `platform.audit.api.model`;
- notification enums -> `platform.notification.api.model`;
- idempotency scope -> `platform.idempotence.api.model` or endpoint-specific scopes under owning API;
- roles/user/tenant status -> `platform.identity` or `platform.accesscontrol` unless required by `TchRequestContext`.

Allowed technical enums in common:

- `ApiScope`;
- `OperationalContextSource` / `TrustLevel` if they are context metadata only;
- API response statuses/severities under `common.web.api`.

## Context policy

`common.context` may bind the canonical HTTP request context, but it must not validate terminal/outlet/session ownership. Operational context in common is only an attached claim/hint.

Action validation remains in owners:

```text
core.terminal -> terminal validation
core.outlet   -> outlet validation
core.session  -> session validation
core.sales    -> sell/cancel/offline acceptance validators
core.payout   -> payout validators
core.offlinesync -> grant/sync technical validators
```

## POM target

After migration, `tchalanet-common` should not need:

```text
spring-batch-core
shedlock-core
shedlock-spring
shedlock-provider-jdbc-template
spring-webflux
spring-data-redis
lettuce-core
spring-data-rest-core
spring-data-rest-webmvc
spring-boot-starter-data-jpa
querydsl-jpa
querydsl-apt
spring-security-oauth2-jose
json-schema-validator
```

Potentially allowed:

```text
spring-modulith-api
spring-boot-starter-validation
spring-web
spring-webmvc
spring-data-commons
spring-tx
jakarta.persistence-api      # only if base entities/converters stay
jackson-core / spring-boot-jackson
spring-security-core         # only if ApiScope/auth extraction requires it; otherwise move out
commons-lang3                # only if used by remaining code
lombok
junit/assertj test
```

## ArchUnit guardrails

Add tests after migration:

- `common` must not import `org.springframework.batch..`.
- `common` must not import `net.javacrumbs.shedlock..`.
- `common` must not import `org.springframework.web.reactive..`.
- `common` must not import `org.springframework.data.rest..`.
- `common` must not import `org.springframework.data.redis..`.
- `common` must not import `com.querydsl..`.
- `common` must not contain `@RestController`.
- `common` must not contain concrete domain events except generic `DomainEvent` contracts.
- `common.types.enums` must not contain business enums after migration.
