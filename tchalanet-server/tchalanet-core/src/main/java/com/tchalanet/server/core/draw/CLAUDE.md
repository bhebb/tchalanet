# Claude — core.draw

Scope:

- Tenant-scoped draw lifecycle only.
- Generate, open, close, apply result, settle.
- Draw states: SCHEDULED, OPEN, CLOSED, RESULTED, SETTLED, CANCELED.

Out of scope:

- Provider HTTP fetching.
- US lottery normalization.
- Haiti projection rules.
- Ticket payout calculation.
- Frontend/mobile.

Rules:

- Draw is tenant-scoped.
- Draw domain must stay pure: no Spring, no JPA, no repositories.
- Use typed IDs outside persistence.
- Use `Instant` for persisted event moments.
- Use injected `Clock` / time provider for now.
- Persistence must remain slot-driven.
- Do not match provider results by `channelCode`.
- Do not depend on `DrawChannelJpaEntity` from domain/application.
- Batch/schedulers call application commands; no business logic in scheduler.
- Cross-domain effects publish events after commit.

Apply results:

- Apply is tenant-scoped.
- Fetch remains global.
- Attach draw_result via result_slot / draw_result relation.
- Publish `DrawResultAppliedEvent` after commit when a tenant draw changes.

Before editing:

- Inspect only files listed by the task.
- Load `/batch` only for scheduler/job changes.
- Load `/event` only for listeners/events.
- Load `/rls` only for tenant SQL/RLS changes.
- Read `docs/CALENDARS.md` for provider no-draw days (`result_slot_calendar_override`),
  generation skip, and open-today cancellation.

Output:

1. Files inspected
2. Minimal change
3. Tests
4. Risks
5. Handoff
