# Claude Implementation Prompt — add-public-drawresults

You are working in the Tchalanet backend.

Implement the OpenSpec change:

```text
openspec/changes/add-public-drawresults
```

## Important project rules

- Use Java/Spring backend conventions already present in the project.
- Use `QueryBus.send(...)`, not `ask(...)`.
- Query handlers are side-effect free.
- Write handlers use `@UseCase`; write transactions use `@TchTx`, but this change is mostly read/query.
- Do not expose JPA/entities outside infrastructure.
- Do not make `features.publicdrawresults` access persistence directly.
- Do not use tenant-scoped `core.draw` for public global results.
- Do not call `TchContext.requireTenantId()` in public drawresult queries.
- Inject `Clock` for time calculations.
- Public DTOs should not expose internal IDs by default.

## Main task

Create/standardize `features.publicdrawresults` and global `core.drawresult` public queries.

The central query is:

```java
public record ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider,
    boolean includeHistory,
    int historyLimit
) implements Query<List<PublicDrawResultSlotView>> {}
```

Rules:

```text
includeHistory=false:
  - force historyLimit=0
  - do not execute history lookup
  - return history = List.of()

includeHistory=true:
  - default historyLimit=5 when invalid/missing
  - cap historyLimit at 10
  - return recent history per slot
```

## PageModel rule

Update/introduce `PublicDrawResultsProvider` so public PageModel widgets use:

```java
queryBus.send(new ListPublicDrawResultSlotsQuery(slotKeys, provider, false, 0));
```

The PageModel provider must not use tenant-scoped draw latest queries for public global result widgets.

## Reader strategy

There are around 10 result slots. Keep the reader simple:

```text
1. query active result slots
2. query latest draw_result per slot
3. if includeHistory=true, query history per slot limited by server-normalized limit
4. compute next/countdown using result_slot.draw_time + timezone and Clock
5. assemble in memory
```

No pagination needed for the slot list. Keep advanced history search paginated.

## Deliverables

- Query model + handler under `core.drawresult.application.query`.
- Projection records under `core.drawresult.application.query.projection`.
- Reader port under `core.drawresult.application.port.out`.
- JDBC/JPA adapter under `core.drawresult.infra.persistence`.
- Public DTOs/mapper/controller under `features.publicdrawresults` if endpoint is needed.
- PageModel provider update to use `includeHistory=false`.
- Tests for includeHistory behavior and no-tenant behavior.

## Verify

- Public PageModel home renders results with no tenant context required.
- `includeHistory=false` does not run history query.
- `includeHistory=false` returns `history=[]` or the PageModel mapper omits it.
- `includeHistory=true&historyLimit=20` returns at most 10 history entries per slot.
- Advanced history endpoint remains paginated and range-limited.
