# Tasks: Reorganize `tchalanet-common`

## Phase 0 — Safety

- [ ] Create branch `reorg/common-technical-kernel`.
- [ ] Run baseline: `./mvnw -pl tchalanet-common test`.
- [ ] Run baseline with dependents: `./mvnw -pl tchalanet-app -am test` if available.
- [ ] Do not perform large blind moves; move by package group and compile after each group.

## Phase 1 — Job/batch extraction

- [x] Rename/keep annotation: `common.job.annotation.TchJob`.
- [x] Keep `common.job.key.JobKey`; remove shared `BatchJobKeys` business constants from common and replace consumers with app/runtime or local domain keys.
- [x] Keep generic job exceptions only in `common.job.exception`.
- [x] Convert `common.job.params.JobParamReader` to a pure `Map<String,String>` reader.
- [x] Move any Spring Batch `JobParameters` adapter to `app.batch.params.SpringBatchJobParams`.
- [x] Move `JobParamsValidator` to `app.job.params`. *(Deleted — zero callers; minimal required-params check inlined in `SpringBatchJobStarter`.)*
- [x] Move `RegisteredJob`, `TchBatchJobRegistry`, and registry metadata to `app.job.registry`. *(Already done on a previous in-progress branch state; no-op for Phase 1.)*
- [x] Move `common.batch.config.*` to `app.config.batch` / `app.config.scheduler`. *(No `common.batch.config` package found — already handled.)*
- [x] Move `common.batch.context.*` to `app.batch.context`. *(Already done on a previous in-progress branch state.)*
- [x] Move `common.batch.launch.BatchJobStarter` to `app.batch.launch.SpringBatchJobStarter` (impl); public interface kept as `common.job.launch.BatchJobStarter` per design decision (interfaces in common, impls in app).
- [x] Move `common.batch.gate.*` to `app.batch.gate` (impls); public interface `BatchGate`, `BatchGateFlagStore`, `BatchDisabledException` kept as `common.job.gate.*`.
- [x] Move `common.batch.service.*` out of common. `TchJobAspect` lives in `app.job.aspect`, publishes `JobLifecycleEvent`, and `platform.communication` listens to enqueue Slack/internal alerts for technical batch failures/skips.
- [x] Keep Spring Batch runtime metadata in app: `SpringTchRegisteredJob` owns `springJobBeanName`, while `RegisteredJob` exposes only public metadata.
- [x] Keep feature ops on common contracts only: `OpsBatchService` uses `TchJobRegistry`, `BatchJobStarter`, `RegisteredJob`, and `JobStartResult`; Spring Batch execution history is not exposed through the public contract yet.
- [x] Remove Spring Batch params from simple core schedulers and use `JobContextBinder.bindTenant` / `bindPlatform` with exactly one clear path.
- [x] Remove Spring Batch and ShedLock dependencies from `tchalanet-common/pom.xml`.
- [x] Compile: `./mvnw -pl tchalanet-common test`. *(Passed: 15 tests.)*

## Phase 2 — HTTP client extraction

- [x] Move `common.client.http.HttpClientConfig` to `app.config.http`.
- [x] Move `RestClientFactory` and `HttpClientProperties` to `platform.<capability>.internal.client`.
- [x] Remove `spring-webflux` from `tchalanet-common/pom.xml` if no remaining common class uses it.
- [x] Compile common and app. *(`./mvnw -pl tchalanet-app -am -DskipTests compile` passed across common/catalog/platform/core/features/app. Full `test` reaches app but still fails on broader ArchUnit/Flyway alignment gates unrelated to HTTP extraction.)*

## Phase 3 — Cache extraction

- [x] Keep `CacheSpec`, `CacheSpecProvider`, and `CacheKeyBuilder` in `common.cache`.
- [x] Move Redis/Caffeine concrete runtime managers/config to `app.config.cache`. *(Renamed: `CacheConfig` → `CacheRuntimeConfig`, `RedisConfig` → `RedisCacheRuntimeConfig`. Moved verbatim: `CacheSpecAwareCaffeineCacheManager`, `CombinedCache`, `CombinedCacheManager`. Deleted `TchCacheProperties` — zero callers.)*
- [x] Keep no Redis/Lettuce dependency in `common`.
- [x] Remove `spring-data-redis`, `lettuce-core`, and `caffeine` from `tchalanet-common/pom.xml`. Added `spring-data-redis` + `lettuce-core` to `tchalanet-app/pom.xml` (caffeine was already there).
- [x] Compile common and app. *(`./mvnw -pl tchalanet-app -am -DskipTests compile` passed across common/catalog/platform/core/features/app. Full `test` reaches app but still fails on broader ArchUnit/Flyway alignment gates unrelated to Phase 3. All Phase 3 grep guards are clean: 0 imports of caffeine/spring-data-redis/lettuce in `tchalanet-common/src`, 0 stale references to moved classes, 12 callers of `common.cache.CacheSpec` intact.)*

## Phase 4 — Web/Data REST cleanup

- [x] Keep `common.web.api`, `common.web.advice`, `common.web.error`, `common.web.paging`, `common.web.converter`.
- [x] Move `common.web.config.DataRestConfig` to `app.config.datarest` or catalog/platform web config.
- [x] Remove Spring Data REST dependencies from `tchalanet-common/pom.xml`.
- [x] Compile common and app. *(`./mvnw -pl tchalanet-common test` passed: 15 tests. `./mvnw -pl tchalanet-app -am -DskipTests compile` passed across common/catalog/platform/core/features/app.)*

## Phase 5 — Persistence cleanup

- [x] Move `common.persistence.config.DataSourceConfig` and `PersistenceConfig` to `app.config.persistence`. *(Both moved verbatim. `DataSourceConfig` keeps the `RlsAwareDataSource` wiring; `PersistenceConfig` keeps `@EnableJpaAuditing`/`@EnableJpaRepositories`/`@EntityScan` rooted at `com.tchalanet.server`. App's `@SpringBootApplication(scanBasePackages = "com.tchalanet.server")` picks up the new package automatically.)*
- [x] Keep RLS bridge in `common.persistence.rls` only if it remains a generic low-level bridge from `TchContext` to DB session variables. *(KEPT — `RlsAwareDataSource` and `ResetOnCloseConnection` are pure spring-jdbc + JDK proxy; no app assembly logic. Sole importer outside common is the moved `DataSourceConfig`.)*
- [x] Review `BaseEntity`, `BaseTenantEntity`, `AuditableEntity`, and `TenantEntityListener`. Decide with ADR whether shared base entities stay in `common.persistence`. *(Decision: KEEP in common. Rationale: 49 JPA entities across `core`/`catalog`/`platform` extend these mapped superclasses; moving them is a 49-file caller migration with no architectural win. They use only `jakarta.persistence-api` and `spring-data-jpa` (`AuditingEntityListener`/`@CreatedDate`/etc.) — light, framework-agnostic surface. Matches the user's Phase 1 rule "keep abstractions in common, move runtime to app".)*
- [x] Keep generic converters (`Currency`, `Locale`, `ZoneId`, JSON map/list) if they are used across modules and do not force heavy runtime dependencies. *(KEPT in `common.persistence.converter` — jakarta-only, used across modules.)*
- [x] Remove JPA starter/QueryDSL from common; use `jakarta.persistence-api` only if necessary. *(QueryDSL removed: `querydsl-jpa` and `querydsl-apt` dropped from `tchalanet-common/pom.xml` — zero `com.querydsl` imports in common src. `spring-boot-starter-data-jpa` KEPT in common because `AuditableEntity` references `AuditingEntityListener` from `spring-data-jpa`. `jakarta.persistence-api` is also explicitly declared (line 35) for the mapped superclasses.)*
- [x] Compile common and app. *(`./mvnw -pl tchalanet-app -am -DskipTests compile` passed across common/catalog/platform/core/features/app. Full `test` reaches app but still fails on broader ArchUnit/Flyway alignment gates unrelated to Phase 5. Phase 5 grep guards clean: 0 `com.querydsl` imports in `tchalanet-common/src`, 0 stale refs to `common.persistence.config`, the `config/` directory removed, 49 entities still importing the kept base classes, `RlsAwareDataSource` correctly imported from the new `app.config.persistence.DataSourceConfig`.)*

## Phase 6 — Enums and domain vocabulary

- [x] Keep typed IDs in `common.types.id` for now. *(Reviewed: typed IDs remain UUID wrappers with construction/parsing/nullability helpers only.)*
- [ ] Move sales/ticket/offline enums to owning core APIs. *(Partial: `SaleOrigin`, `TicketResultStatus`, `TicketSaleStatus`, `TicketSettlementStatus`, `TicketSyncStatus` moved to `core.sales.api.model`; `BetType` remains open because it is also owned by catalog pricing/selection.)*
- [ ] Move draw/result enums to owning core/catalog APIs. *(Partial: `ResultQuality` moved to `core.drawresult.api.model`; `UsLotteryProvider` moved to `core.uslottery.internal.application.model`. `DrawSource` and `GameCode` remain open because they cross catalog/core boundaries.)*
- [x] Move audit enums to `platform.audit.api.model`.
- [x] Move notification enums to `platform.notification.api.model`.
- [x] Move idempotency enum(s) to `platform.idempotence.api.model` or endpoint owner.
- [x] Move access-control role/permission enums/constants to `platform.accesscontrol` / `platform.identity`, except what `TchRequestContext` needs. *(Reviewed: no permission enum remains in common. `TchRole` intentionally stays in common because `TchRequestContext`, batch context binding, identity/accesscontrol APIs, and UI composition all consume the raw runtime role frame.)*
- [x] Move `common.selection.SelectionKeyCanonicalizer` and `SelectionKey` to owning module after usage analysis. *(Moved to `core.selection`: only `core.sales` and `core.limitpolicy` use this selection business rule. `common.selection` is now empty and no source imports `common.selection.*`.)*
- [x] Compile all modules affected. *(`./mvnw -pl tchalanet-app -am -DskipTests compile` passed across common/catalog/platform/core/features/app after the Phase 6/7 moves.)*

## Phase 7 — Utilities cleanup

- [x] Keep `Hashing` in `common.util` only if it is pure and used by multiple modules. *(Kept: pure SHA-256 helper with JDK-only dependencies, shared by multiple `core.uslottery` provider adapters/cache helpers.)*
- [x] Move `JsonSchemaValidatorUtil` to `platform.document`, `platform.schema`, or owning capability. *(Moved to owning `core.pagemodel.internal.application.service`; dependency moved from common to core.)*
- [x] Move `RoleUtils` to `platform.accesscontrol` / `platform.identity`. *(Resolved by deletion: the only remaining use was JWT role extraction for the existing `AuthContextExtractor`, so the logic is private to that existing class instead of a public generic util. Moving it to platform would invert the dependency back into common context.)*
- [ ] Review `JsonUtils`, `JsonbUtils`, `ObjectMapperHolder` and move runtime holders/config out of common if needed. *(Reviewed but not moved in this batch: these are shared Jackson 3 helpers used by common converters and multiple modules. A separate JSON-runtime decision is needed before removing Spring-managed holders.)*
- [x] Remove `json-schema-validator` from common POM.

## Phase 8 — ArchUnit enforcement

- [x] Add ArchUnit tests for forbidden imports in `common`. *(Added `CommonTechnicalKernelArchitectureTest`; app test compilation is currently blocked before execution by existing `SellerOperationalContextResolverTest` referencing a missing class.)*
- [x] Add ArchUnit tests for forbidden annotations in `common`. *(Covers runtime assembly annotations and repository/service/controller/entity annotations while allowing the existing technical `@Component`, `@Configuration`, and mapped-superclass surface.)*
- [ ] Add ArchUnit tests for no business enums in `common.types.enums`. *(Partial guard added for enums already extracted from common. Full ban remains open because `BetType`, `DrawSource`, `GameCode`, tenant/user/theme/status enums and limit/autonomy vocabulary still cross module boundaries and need explicit ownership decisions.)*
- [x] Add ArchUnit tests for no Spring Batch/ShedLock/WebFlux/Data REST/Redis/QueryDSL in common. *(Also guards Data REST, Redis/Lettuce, Caffeine, and json-schema-validator imports.)*

## Phase 9 — Final validation

- [x] `./mvnw -pl tchalanet-common test` *(Passed: 15 tests.)*
- [ ] `./mvnw -pl tchalanet-app -am test` *(Blocked before execution by existing app test compilation error: `SellerOperationalContextResolverTest` references missing `SellerOperationalContextResolver`.)*
- [ ] `./mvnw clean verify`
- [ ] Update docs: `docs/modules/common.md`, `docs/ARCHITECTURE.md`, and module README if needed.
- [ ] Remove stale packages and imports.
