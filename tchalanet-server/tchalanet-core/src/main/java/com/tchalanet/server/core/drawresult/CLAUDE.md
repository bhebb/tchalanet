# Claude — core.drawresult

Scope:

- Global draw_result orchestration and persistence.
- Fetch external results window.
- Upsert draw_result.
- Source hash, status, force behavior.
- Result slot mapping.

Out of scope:

- Tenant draw lifecycle except through ports/commands.
- Provider HTTP parsing details.
- Ticket settlement.
- Frontend/mobile.

Rules:

- `draw_result` is global, not tenant-scoped.
- Fetch is driven by `result_slot_key`.
- Never fetch by `draw_channel_code`.
- Never fetch by sold `game_code`.
- `occurredAt` comes from result_slot date + draw_time + timezone.
- Use injected `Clock`, not direct `Instant.now()`.
- Upsert by `(result_slot_id, occurred_at)`.
- Use source_hash for idempotency/change detection.
- Respect `force`.
- Do not overwrite CONFIRMED/OVERRIDDEN unless explicitly allowed.
- Manual force overwrite requires reason/audit.
- Eviction/events happen after commit.

Statuses:

- PROVISIONAL
- CONFIRMED
- OVERRIDDEN
- Optional later: REJECTED / INVALID

Before editing:

- Inspect only files listed by the task.
- Load `/cache` only if cache keys/eviction change.
- Load `/event` only if event publication/listeners change.
- Load `/batch` only if scheduler/job launch changes.

Output:

1. Diagnosis
2. Minimal patch
3. Files changed
4. Tests
5. Handoff
