# Operational Context — Modulith Runtime Rules

> Status: NORMATIVE  
> Scope: HTTP, batch, scheduler, startup, events, tests, migrations  
> Goal: prevent tenant/RLS/context regressions during the platform migration.

## Principle

Architecture modules do not change the runtime truth:

- request context is produced at system boundaries;
- RLS reads the effective context;
- async/event/batch boundaries must carry context explicitly;
- platform migration must not hide tenant or actor decisions in helpers.

## Context producers

| Flow                  | Context producer                      | Rule                                                                 |
| --------------------- | ------------------------------------- | -------------------------------------------------------------------- |
| HTTP                  | `TchContextFilter`                    | canonical request context from route/auth/tenant/actor               |
| User bootstrap        | actor bootstrap/filter                | may enrich actor only; does not decide tenant                        |
| Batch                 | `BatchTchContextBinder` or equivalent | creates TENANT or PLATFORM context from job params                   |
| Scheduler             | scheduler or launched batch           | prefer launching batch; direct work needs explicit scheduler context |
| Startup tenant work   | explicit startup tenant scope         | used for tenant seed/init work                                       |
| Startup platform work | explicit platform/startup scope       | used for platform technical bootstrap                                |
| Event/listener        | event payload + listener binder       | carry tenant/actor/correlation when thread hop/retry is possible     |
| Tests                 | test helper                           | must set and restore context deterministically                       |

## Platform migration impact

Moving `tenantuser`, `tenantconfig`, `tenanttheme`, `accesscontrol`, `audit`, `document`, `communication` or `idempotence` must not change:

- how tenant is resolved;
- how user/actor is resolved;
- how RLS session variables are applied;
- how audit actor/tenant/request id are recorded;
- how batch/scheduler context is bound;
- how event listeners restore context.

## Rules for platform services

Platform services may consume context through approved APIs:

- `TchRequestContext` passed explicitly;
- `TchContextResolver`;
- `@CurrentContext` at web boundary;
- event payload metadata;
- batch context binder.

Platform services must not:

- parse JWT directly;
- read arbitrary request headers directly;
- set `TchContext` to make downstream code work, except explicit boundary/scope helpers;
- call `set_config` or manipulate RLS directly;
- trust tenant IDs from request bodies for tenant-scoped operations.

## RLS after migration

RLS remains a persistence/runtime concern.

- `common` may contain low-level context primitives.
- `platform` may contain tenant/user/access-control decisions.
- datasource bridge remains the only code applying PostgreSQL session variables.

Tables moved to `platform` must still follow RLS rules:

- tenant-scoped platform tables use `tenant_id` + policies;
- global platform tables are explicitly global;
- audit tables must preserve tenant/actor/request metadata.

## Events

Public events crossing modules must live in the source module `api.event`.

Event payloads must include required context facts:

- tenant id when tenant-scoped;
- actor/user id when actor-sensitive;
- correlation/request id when traceability matters;
- occurredAt.

Listeners must:

- set/restore context if needed;
- execute local operations through the owning module API or internal application service;
- be idempotent where retries are possible;
- not write cross-domain state in the source transaction.

## Verification checklist

After each migration step, verify:

- HTTP request context still binds and clears;
- RLS variables are applied and reset;
- platform reads do not add manual tenant filters unless write-side or explicit exception;
- batch/scheduler jobs set context before DB access;
- events carry tenant/actor metadata;
- audit records preserve tenant/user/request id;
- security permission checks do not import old packages;
- no module imports another module's `internal` package.
