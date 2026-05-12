# Design — Platform Modulith Migration

## Global architecture

Tchalanet is a Modular Monolith.

Spring Modulith verifies logical module boundaries.
ArchUnit verifies structural and code-level rules.
Maven modules provide macro build boundaries.

## Layers

```text
common      Technical Shared Kernel
catalog     Simple DDD / Reference Catalog
platform    Technical/Application Service Modules
core        Clean Architecture / Hexagonal / CQRS
features    Vertical Slice / BFF
app         Spring Boot assembly
```

## Platform module shape

```text
platform/<capability>/
  api/
  internal/
```

`api` exposes stable Java contracts.
`internal` hides implementation.

## Core module shape

```text
core/<domain>/
  api/
    command/
    query/
    event/
    model/
  internal/
    domain/
    application/
    infra/
```

## Features shape

```text
features/<feature>/<slice>/
  web/
  app/
  model/
  mapper/
```

Features are leaves and expose HTTP contracts, not Java APIs.

## Transactions

Platform services join caller transaction by default.
Independent transactions require a documented exception.
`platform.audit` is the known exception for failure audit.

## Events

- Core publishes core domain events.
- Platform may listen to core events.
- Platform may publish technical/application events.
- Core must not listen to platform events.

## Notification

`core.notification` is not a default home.
Use `platform.notification` for notification inbox/preferences/state/routing.
Use `platform.communication` for provider delivery.
