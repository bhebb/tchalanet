# OpenSpec — Naming Rules aligned to Modulith (74)

> Status: NORMATIVE

## 1. Package root names

Top-level package families are fixed:

```text
common
catalog
platform
core
features
```

## 2. Public/internal packages

Modules consumed by other modules MUST expose an `api` package and hide implementation in `internal`.

Applies to:

- `catalog.<name>.api` / `catalog.<name>.internal`
- `platform.<capability>.api` / `platform.<capability>.internal`
- `core.<domain>.api` / `core.<domain>.internal`

Features do not expose Java APIs by default.

## 3. Core API names

Core public contracts:

- commands: `XxxCommand`
- queries: `XxxQuery`
- public events: `XxxEvent`
- results/views: `XxxResult`, `XxxView`, `XxxSummary`, `XxxRow`

Core internals:

- handlers: `XxxCommandHandler`, `XxxQueryHandler`
- ports: `XxxReaderPort`, `XxxWriterPort`, `XxxGatewayPort`
- persistence: `XxxJpaEntity`, `XxxJpaRepository`, `XxxJpaAdapter`

## 4. Platform names

Platform public contracts:

- API facade: `XxxApi`
- request/result models: `XxxRequest`, `XxxResult`
- views: `XxxView`, `XxxSummary`

Platform internals:

- services: `XxxService`, `XxxResolver`, `XxxPolicy`, `XxxManager`
- adapters: `XxxAdapter`, `XxxGateway`, `XxxClient`
- persistence: `XxxJpaEntity`, `XxxRepository`

## 5. Feature names

Features use UI names:

- `XxxController`
- `XxxService` or `XxxOrchestrator`
- `XxxRequest`, `XxxResponse`, `XxxView`, `XxxItem`

The term `Dto` is forbidden.
