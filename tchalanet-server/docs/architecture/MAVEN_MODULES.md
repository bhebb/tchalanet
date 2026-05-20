# Maven Modules — Macro Build Structure

## Target modules

```text
tchalanet-server/
  pom.xml                       parent aggregator
  tchalanet-common/             technical shared kernel
  tchalanet-catalog/            reference catalogs
  tchalanet-platform/           transversal platform services
  tchalanet-core/               core business domains
  tchalanet-features/           BFF / UI vertical slices
  tchalanet-app/                Spring Boot runtime assembly
```

## Dependency graph

```text
tchalanet-common
  -> none

tchalanet-catalog
  -> tchalanet-common

tchalanet-platform
  -> tchalanet-common
  -> tchalanet-catalog

tchalanet-core
  -> tchalanet-common
  -> tchalanet-catalog
  -> tchalanet-platform

tchalanet-features
  -> tchalanet-common
  -> tchalanet-catalog
  -> tchalanet-platform
  -> tchalanet-core

tchalanet-app
  -> all modules
```

## Build commands

Compile one module:

```bash
./mvnw -pl tchalanet-common verify
```

Compile a module and its required dependencies:

```bash
./mvnw -pl tchalanet-core -am verify
```

Run the application assembly:

```bash
./mvnw -pl tchalanet-app -am spring-boot:run
```

Full verification:

```bash
./mvnw clean verify
```

## Rule

Agents and developers may use targeted validation during work, but every PR must pass full verification before merge.

## Notes

- Keep versions in the parent POM and `VERSIONS.md`.
- Only `tchalanet-app` is executable.
- Do not create one Maven module per core domain or platform capability yet.
- Spring Modulith + ArchUnit enforce logical submodule boundaries.
