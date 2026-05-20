# CLAUDE — Tchalanet Common Reorganization Guide

## Mission

Reorganize `tchalanet-common` so it becomes a strict Technical Shared Kernel. Do not treat `common` as a dumping ground for shared business concepts.

## Baseline observed in uploaded zip

The uploaded module currently contains these top-level areas:

```text
common.batch
common.bus
common.cache
common.client.http
common.constant
common.context
common.event
common.job
common.json
common.mapper
common.persistence
common.security
common.selection
common.stereotype
common.time
common.tx
common.types
common.util
common.web
```

The module also contains a heavy POM with Spring Batch, ShedLock, WebFlux, Redis, Data REST, JPA starter, QueryDSL, OAuth JOSE, and json-schema-validator. These dependencies are not acceptable in the final `common` unless explicitly justified by remaining classes.

## Non-negotiable rule

Before keeping any class in common, ask:

> If this class were published as a technical library for another Tchalanet-like app, would it still make sense without knowing sales, draw, payout, tickets, tenants, notifications, audit, batch runtime, or platform operations?

If no, move it out.

## Keep / Move summary

### Keep in common

```text
common.bus                         command/query bus contracts + in-process impl
common.event                       DomainEvent + publisher contracts + MVP Spring publisher
common.tx                          AfterCommit
common.stereotype                  @UseCase, @TchTx if annotation-only
common.types.id                    simple typed ID wrappers only
common.types.money                 Money, CurrencyCode, Percent
common.types.time                  DateRange, TimeWindow
common.time                        Clock/timezone/date helpers
common.context                     request context contracts/binder/thread-local
common.context.web                 canonical HTTP context filter/resolver if retained as common web infra
common.web.api                     ApiResponse contract
common.web.advice                  auto-wrap 2xx advice/context
common.web.error                   ProblemRest and generic HTTP exceptions
common.web.paging                  TchPage/TchPaging
common.web.converter               typed ID converters
common.json                        typed ID Jackson support only
common.mapper                      CommonIdMapper only
common.job.annotation              TchJob
common.job.key                     JobKey; review constants
common.job.exception               generic job exceptions
common.job.params                  pure Java param keys/reader only
```

### Move out of common

```text
common.batch.*                     app.job/app.batch or future platform.job
common.client.http.*               app.config.http or owning platform/core infra clients
common.cache.RedisConfig           app/platform cache runtime
common.web.config.DataRestConfig   app/catalog/platform config
common.persistence.config.*        app.config.persistence
common.security.Permissions        platform.accesscontrol / owning domains
common.selection.*                 owning game/sales/drawresult module
common.types.enums.* business      owner APIs
common.util.JsonSchemaValidatorUtil platform.document/schema or owner
common.util.RoleUtils              platform.accesscontrol/identity
```

## Work method

Do not move everything at once.

1. Pick one package group.
2. Move classes and update packages/imports.
3. Compile `tchalanet-common`.
4. Compile affected module with `-am`.
5. Remove only dependencies proven unused by common.
6. Continue.

## Compilation loop

Use this after each small group:

```bash
./mvnw -pl tchalanet-common test
./mvnw -pl tchalanet-app -am test
```

Final:

```bash
./mvnw clean verify
```

## Package-specific instructions

### Job/batch

Common keeps only generic job language. Runtime goes to app:

```text
common.job.annotation.TchJob                    KEEP
common.job.key.JobKey                           KEEP
common.job.params.JobParamKeys                  KEEP if pure/minimal
common.job.params.JobParamReader                KEEP only after refactor to Map<String,String>
common.job.params.JobParamsValidator            MOVE app.job.params
common.batch.*                                  MOVE app.batch/app.job
```

Spring Batch classes must not remain in common.

### Cache

Keep specs and key builder. Move concrete manager/runtime config if it imports Redis, Lettuce, or Caffeine manager wiring.

### Web

Keep API response and pagination. Move Data REST config.

### Persistence

RLS bridge may remain if accepted as low-level infra. DataSource/runtime config moves to app. Shared base entities need an explicit ADR or documented project decision.

### Enums

Move business enums. Keep only technical enums required by context/web contracts.

### IDs

Keep all simple `XxxId(UUID value)` wrappers in common for now to preserve converters and mappers. Do not add business methods to IDs.

## POM cleanup target

Remove from `tchalanet-common` once code is moved:

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

Also remove any hardcoded dependency version from child modules; versions belong in parent/VERSIONS.

## Do not break these rules

- Do not make `common` depend on `platform`, `core`, `catalog`, or `features`.
- Do not leave Spring Batch in common.
- Do not leave external gateway config in common.
- Do not leave business enums in common.
- Do not move typed IDs during this pass unless there is a specific compile reason.
- Do not put runtime assembly config in common.
