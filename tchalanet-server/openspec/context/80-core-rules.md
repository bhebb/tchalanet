# OpenSpec — Core Rules (80)

## Status

NORMATIVE.

## 1. Definition

`core` contains Tchalanet core business domains: business invariants, lifecycle rules, transactional consistency, domain decisions, and side-effects.

## 2. Structure

```text
core/<domain>/api/
  command/
  query/
  event/
  model/

core/<domain>/internal/
  domain/
    model/
    service/
    event/
    exception/
  application/
    command/handler/
    query/handler/
    port/out/
    service/
  infra/
    persistence/
    web/
    event/
    batch/
    scheduler/
    cache/
    config/
```

## 3. Public API

`core.<domain>.api` is the Java contract consumed by other modules.

Allowed in `api`:

- commands;
- queries;
- public integration/application events;
- read models;
- command result models;
- criteria used by public queries.

Forbidden in `api`:

- domain aggregates/entities;
- JPA entities;
- repositories;
- handlers;
- output ports;
- controllers;
- Spring MVC DTOs specific to a web endpoint;
- cache or persistence implementation types.

## 4. Internal Clean Architecture

Inside `internal`, core remains Clean Architecture / Hexagonal / CQRS:

- domain is pure;
- writes use CommandBus and handlers;
- reads use QueryBus and handlers;
- persistence behind ports;
- side effects after commit;
- controllers thin;
- no infra dependency from application/domain.

## 5. Migrated-out components

The following are no longer core domains after the platform migration:

```text
core.audit        -> platform.audit
core.accesscontrol -> platform.accesscontrol
core.tenanttheme  -> platform.tenanttheme
core.tenantuser   -> platform.usercontext
core.tenantconfig -> platform.tenantconfig
```

Any remaining core dependency on those packages must be changed to `platform.<capability>.api`.
