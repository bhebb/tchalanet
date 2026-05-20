# OpenSpec Proposal — PageModel Dynamic Providers Query Handlers

## Change id

`pagemodel-query-handlers`

## Status

Draft / implementation-ready.

## Scope

This change implements the query side required by existing PageModel dynamic provider stubs.

The current dynamic resolver/moulinette already exists:

```text
PageModelDoc widget binding
  -> PageModelDynamicResolver
  -> PageModelDynamicProvider.supports(logicalId, widgetType, source)
  -> provider.load(...)
  -> PageDynamicPayload.widgets[widgetId]
```

This OpenSpec does **not** replace the resolver. It adds the missing query handlers and reader projections behind providers.

## MVP scope

Implement first:

1. Public dynamic widgets
2. Cashier dashboard dynamic widgets
3. Admin tenant dashboard dynamic widgets

## Post-MVP scope

Superadmin dashboard dynamic widgets are documented here but intentionally delayed.

## Why

Provider stubs must not contain business logic, SQL, or repository access. Each provider must only:

1. Read widget props.
2. Resolve context from `TchRequestContext`.
3. Dispatch a stable query via `QueryBus`.
4. Return the query result as widget payload.

The query handler owns application composition. The SQL reader/projection owns optimized reads.

## Goals

- Keep `features.pagemodel` as BFF/UI orchestration only.
- Put business queries in the owning bounded context.
- Create missing query models, handlers, view DTOs, reader ports, and JDBC readers.
- Use optimized SQL projections for dashboard widgets.
- Keep all public responses masked and tenant-safe.
- Defer superadmin providers to post-MVP but document the intended sources.

## Non-goals

- Do not redesign the PageModel JSON model.
- Do not move business rules into PageModel providers.
- Do not add cross-domain SQL inside `features.pagemodel`.
- Do not implement superadmin dashboard in MVP.
- Do not create a second provider framework.

## Architectural rule

For every provider:

```text
Provider stub
  -> identify owner domain
  -> check existing query
  -> create query if missing
  -> create handler
  -> create reader port
  -> create SQL projection adapter
  -> return stable widget DTO
```
