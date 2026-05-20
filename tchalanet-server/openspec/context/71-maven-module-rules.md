# OpenSpec — Maven Module Rules (71)

## Status

NORMATIVE

## Macro modules

The backend uses macro Maven modules:

```text
tchalanet-common
tchalanet-catalog
tchalanet-platform
tchalanet-core
tchalanet-features
tchalanet-app
```

## Dependency graph

```text
common      -> none
catalog     -> common
platform    -> common, catalog
core        -> common, catalog, platform
features    -> common, catalog, platform, core
app         -> all
```

## Rules

- `tchalanet-app` is the only executable Spring Boot application.
- Do not create one Maven module per domain/capability during this migration.
- Use Spring Modulith and ArchUnit for logical submodule enforcement.
- Targeted builds are allowed during development.
- Full `./mvnw clean verify` is required before merge.

## Commands

```bash
./mvnw -pl tchalanet-platform -am verify
./mvnw -pl tchalanet-core -am verify
./mvnw -pl tchalanet-app -am spring-boot:run
./mvnw clean verify
```
