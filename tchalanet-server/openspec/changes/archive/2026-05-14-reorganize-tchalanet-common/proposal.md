# Proposal: Reorganize `tchalanet-common` as a strict Technical Shared Kernel

## Why

The current `tchalanet-common` module compiles, but it still mixes several responsibilities:

- pure shared primitives (`bus`, `event`, `tx`, `typed ids`, `web api`, `paging`);
- application/runtime glue (`batch`, `scheduler`, `shedlock`, `Spring Batch`, `WebClient`, `Data REST`, Redis config);
- platform capabilities (`batch notifications`, permission constants, audit enums, notification enums, idempotency scope);
- domain vocabulary (`BetType`, `GameCode`, ticket statuses, selection canonicalization, sales/draw/payout concepts);
- persistence/runtime assembly (`DataSourceConfig`, `PersistenceConfig`, cache managers, Redis, RLS datasource wrapper).

This makes the module heavier than its intended role and forces dependencies such as Spring Batch, ShedLock, WebFlux, Redis, Data REST, QueryDSL/JPA starter, OAuth JOSE, and json-schema-validator into `common`.

## Goal

Make `tchalanet-common` the Technical Shared Kernel only:

- no inter-module dependencies;
- no domain workflows;
- no platform capability state/workflow;
- no external gateway/client runtime configuration;
- no Spring Batch/ShedLock/WebFlux/Data REST runtime configuration;
- no business enums except minimal technical roles/context enums that are needed by the request context contract;
- minimal external dependencies.

## Non-goals

- Do not split Maven into more modules yet.
- Do not move all typed IDs out of `common` in this change.
- Do not rewrite business flows.
- Do not introduce outbox, Kafka, or async bus behavior.

## Main decisions

1. Keep typed ID wrappers in `common.types.id` for now, as simple type-safety primitives.
2. Move business enums out of `common.types.enums` to their owning `core`, `catalog`, or `platform` packages.
3. Keep `common.job.annotation.TchJob`, `common.job.key.JobKey`, `common.job.exception.JobSkippedException`, and minimal pure `common.job.params` primitives.
4. Move Spring Batch runtime, job registry, gates, launchers, notification, ShedLock, context binding, and validators to `tchalanet-app` or a later `platform.job` capability.
5. Keep `common.web.api`, `common.web.error`, `common.web.paging`, `common.web.advice`, and typed ID web converters.
6. Move Spring Data REST config out of `common` to app/catalog runtime config.
7. Move HTTP client factories/config out of `common` to app or platform capabilities.
8. Keep cache abstractions in `common.cache`; move Redis/Caffeine manager runtime config to app/platform cache configuration.
9. Keep RLS bridge only if it is treated as low-level context-to-datasource infrastructure; move raw datasource/runtime assembly config to app.
10. Keep JSON typed ID serializer/deserializer support if used globally; move JsonSchema validation utility to platform document/schema or owning capability.

## Expected impact

- `tchalanet-common` POM becomes smaller.
- Batch/ShedLock/WebFlux/Redis/Data REST dependencies are removed from `tchalanet-common`.
- `common` package boundaries become easier to enforce with ArchUnit.
- Claude/Codex agents have a concrete package map for future class-by-class cleanup.
