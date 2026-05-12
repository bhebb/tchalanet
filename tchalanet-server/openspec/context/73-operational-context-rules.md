# OpenSpec — Operational Context Rules (73)

> Scope: HTTP, batch, scheduler, startup, events, tests  
> Status: NORMATIVE  
> Purpose: preserve tenant, actor, RLS and audit correctness across the Modulith migration.

## 1. Context ownership

HTTP context is owned by the HTTP boundary.

- `TchContextFilter` creates canonical request context.
- User bootstrap may enrich actor information only.
- Tenant resolution does not move into platform services.

## 2. Boundary matrix

| Flow             | Rule                                            |
| ---------------- | ----------------------------------------------- |
| HTTP             | bind/clear context in filter                    |
| Batch            | create explicit context from job parameters     |
| Scheduler        | launch batch or bind explicit scheduler context |
| Startup tenant   | use explicit startup tenant scope               |
| Startup platform | use explicit startup platform scope             |
| Event listener   | use event metadata and restore prior context    |
| Tests            | set/restore context deterministically           |

## 3. Platform service restrictions

Platform services MUST NOT:

- parse JWT;
- trust tenant ids from request bodies;
- directly manipulate `SecurityContext` for business decisions;
- call PostgreSQL `set_config`;
- blindly clear `TchContext` when a previous context exists.

They MAY use:

- explicit `TchRequestContext` argument;
- `TchContextResolver`;
- event metadata;
- boundary scope helpers.

## 4. RLS

RLS remains the isolation authority.

Java read-side code MUST NOT add tenant filters to compensate for context mistakes.

Platform tables must be classified as:

- tenant-scoped with `tenant_id` + RLS;
- global with explicit documentation;
- audit/technical with documented visibility rules.

## 5. Events

Events crossing modules MUST live in `api.event` and carry tenant/actor/correlation metadata when needed.

Listeners MUST be idempotent when retry or duplicate delivery is possible.

## 6. Verification after migration

After migrating any context-sensitive module, verify:

- HTTP context binding;
- RLS application/reset;
- audit metadata;
- batch/scheduler context;
- listener context restoration;
- no manual tenant filter introduced on read-side queries;
- no `internal` imports across modules.
