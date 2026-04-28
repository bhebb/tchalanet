# Design — `pos-v0-foundation`

> How to implement the foundation. Reference [proposal.md](proposal.md) for the "why".
> Reference [specs/](specs/) for acceptance criteria.
> Depends on: `rename-pos-to-terminal` (must be complete before this starts).

## Context

**Current state** (measured from source):

| File                                               | Status                                                                                       | What's missing                                                                    |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| `V5__core_terminal.sql`                            | missing `kind`, `owner_agent_id`, `status`                                                   | ADD 3 columns + 3 constraints + 1 partial unique index                            |
| `V4__core_outlet_table.sql`                        | missing `kind`, `owner_agent_id`                                                             | ADD 2 columns + 2 constraints + 1 index                                           |
| `V8__core_pos.sql` (→ `sales_session` post-rename) | missing z-report fields + ABORTED status                                                     | ADD 8 columns + update 2 constraints                                              |
| `V3__core_settings.sql`                            | `level` only goes to TERMINAL; no `agent_id`, no override flags                              | ADD 1 column + 3 flag columns + update 3 constraints + drop/recreate unique index |
| `V32__seed_iam_roles_permissions.sql`              | `terminal.*`, `outlet.*`, `setting.*`, `sales.*`, `limit.*`, `print.*`, `sync.*` keys absent | ADD INSERT rows for ~30 permission keys + role assignments                        |

All schema changes are **in-place edits** to existing migration files. DB recreated from scratch (same policy as `rename-pos-to-terminal`).

External cross-domain call surface (existing):

- `core.sales` publishes `TicketPlacedEvent` — consumed by `core.session`
- `core.user` publishes `UserSuspendedEvent` — consumed by `core.session`
- `core.terminal` publishes `TerminalStatusChangedEvent` — consumed by `core.session`
- `core.outlet` publishes `OutletKindOrOwnerChangedEvent` / `OutletStatusChangedEvent` — consumed by `core.session`

## Goals / Non-Goals

**Goals:**

- Schema ready: `ddl-auto=validate` passes after migration.
- All 5 domain extensions implemented with their commands, queries, ports, handlers, events.
- All 7 cross-domain listeners wired and idempotent.
- `SettingKey` registry validates at startup.
- All new permission keys seeded for default roles.
- No BFF endpoints. No Flutter client.

**Non-Goals:**

- BFF / Flutter POS app → `pos-v0-features`
- Multi-outlet operators → v1+
- Offline mode → v1+
- Limit policy changes (already supports AGENT via `limit_assignment.target_type`)
- `core.autonomy` changes (already supports `requireApprovalOnBlock`)

## Decisions

### D1 — SQL migrations: in-place edit, no new file

Same policy as `rename-pos-to-terminal`. DB recreated from scratch.

**Files to edit:**

| File                                         | Change                                                                                                                                                                                                       |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `V5__core_terminal.sql`                      | ADD `kind`, `owner_agent_id`, `status` columns + constraints + partial unique index                                                                                                                          |
| `V4__core_outlet_table.sql`                  | ADD `kind`, `owner_agent_id` columns + constraints + index                                                                                                                                                   |
| `V8__core_pos.sql`                           | ADD z-report columns to `sales_session` + update status and close constraints                                                                                                                                |
| `V3__core_settings.sql`                      | ADD `agent_id`, override flag columns; update `level` constraint; update scope constraint; drop/recreate unique index to include `agent_id`                                                                  |
| `V32__seed_iam_roles_permissions.sql`        | ADD ~30 new permission keys + role-permission rows using `INSERT … ON CONFLICT DO NOTHING`                                                                                                                   |
| `V43__audit_table.sql` — `terminal_aud`      | ADD `kind varchar(16)`, `owner_agent_id uuid`, `status varchar(16)`                                                                                                                                          |
| `V43__audit_table.sql` — `outlet_aud`        | ADD `kind varchar(16)`, `owner_agent_id uuid`                                                                                                                                                                |
| `V43__audit_table.sql` — `app_setting_aud`   | ADD `agent_id uuid`, `is_overridable_by_outlet bool`, `is_overridable_by_terminal bool`, `is_overridable_by_agent bool`; update `app_setting_aud_level_check` to include `'AGENT'`                           |
| `V43__audit_table.sql` — `sales_session_aud` | ADD 8 z-report columns (`opening_float`, `closing_amount`, `expected_amount`, `variance`, `variance_note`, `closed_by`, `tickets_count`, `total_stake_htg`); update status constraint to include `'ABORTED'` |

**Constraint**: only valid because team commits to `docker compose down -v` before applying.

---

### D2 — `TerminalKind` and `TerminalStatus` as Java enums (not strings)

`kind` and `status` columns are `varchar` with DB CHECK constraints.
On the Java side: `TerminalKind { PHYSICAL, VIRTUAL }` and `TerminalStatus { ACTIVE, DISABLED, ARCHIVED }` as enums with JPA `@Enumerated(EnumType.STRING)` converters.

**Rationale**: avoids raw string comparisons in domain code; compile-time safety; readable.

---

### D3 — `ProvisionVirtualTerminalCommand` idempotency via port + unique index

The unique partial index `ux_terminal_one_active_virtual_per_agent ON terminal(tenant_id, owner_agent_id) WHERE kind='VIRTUAL' AND status='ACTIVE' AND deleted_at IS NULL` provides the DB-level guarantee.
The handler calls `terminalReaderPort.findActiveVirtualForAgent(agentId)` first; if found, returns it. Otherwise inserts.

**Alternative considered**: optimistic insert + catch `DataIntegrityViolationException` — rejected because it hides intent and is harder to test.

---

### D4 — `OutletField` enum classifies fields at compile time

`OutletField` enum carries `isOperational()` / `isJuridical()` predicates.
`UpdateOutletCommandHandler` loops over the submitted `Map<OutletField, Object>` and checks each field's classification against the caller's role.

**Rationale**: all authorization logic for field updates lives in one handler method; no scattered `if` branches. Adding a new field only requires adding an enum entry with its classification.

---

### D5 — `SettingKey` registry is the single source of truth

A `SettingKey` Java enum is the authoritative list of `(namespace, key)` pairs. A `SettingRegistryValidator` `@Component` queries all active `app_setting` rows at startup and fails if any row's key is not in the enum.

**Rationale**: prevents drift between DB data and code without requiring a separate validation step. Spring Boot's fail-fast pattern.

**Risk**: startup fails if DB contains stale/unknown keys. Mitigation: validator logs all unknown keys before throwing, making the diff obvious.

---

### D6 — Cascade resolver: ordered candidate list with gates

The `SettingsResolverPort` implementation builds candidates in order: AGENT → TERMINAL → OUTLET → TENANT → GLOBAL.
For each candidate level:

1. Skip if `key.maxLevel` is lower than this level.
2. Skip if the TENANT row does not have the corresponding `is_overridable_by_*` flag set.
3. Special case: if `terminal.kind=VIRTUAL` and `terminal.ownerAgentId=ctx.agentId`, treat as ownership exception and allow AGENT level even if `is_overridable_by_terminal=false`.
4. Return the first candidate that passes all gates, or fall back to `SettingKey.defaultValue`.

**Rationale**: single linear pass; easy to add levels; no recursive lookup. The ownership exception (D6.3) is the only non-linear rule.

---

### D7 — `EffectiveSetting.editable` computed server-side

When building `EffectiveSetting`, the handler computes `editable` by simulating whether the caller context could write at AGENT or VIRTUAL-TERMINAL level. The Flutter client consumes this flag as-is — it never re-evaluates permissions.

**Rationale**: avoids permission logic in the Flutter client; prevents stale client impersonating write access.

---

### D8 — Cross-domain listeners use `CommandBus` to dispatch abort commands

When `TerminalStatusChangedEvent` arrives, `SalesSessionAbortOnTerminalDisabledListener` dispatches `AbortSalesSessionsForTerminalCommand` via the `CommandBus` (not a direct service call), ensuring the abort runs in its own `@TchTx` transaction.

**Rationale**: consistent with the event-driven architecture pattern already present in the codebase; audit entries for the abort are written in the same transaction as the abort.

---

### D9 — `sales_session` status constraint updated in-place (not additive)

`chk_sales_session_status` and `chk_sales_session_close` are dropped and recreated in `V8__core_pos.sql` to include `ABORTED`.

**Rationale**: constraint names already exist (post `rename-pos-to-terminal`); additive migration would require a separate `DROP CONSTRAINT / ADD CONSTRAINT` step which is pointless on a fresh DB.

## Risks / Trade-offs

| Risk                                                                                | Mitigation                                                                                                                         |
| ----------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `SettingRegistryValidator` fails on unknown legacy `app_setting` rows               | Audit `V3__core_settings.sql` and `V36__seed_app_settings_batch.sql` before starting; clean up stale keys in-place                 |
| Cross-domain listeners fail silently if `ProcessedEventPort` has a bug              | Unit-test each listener independently; integration test with duplicate event replay                                                |
| `ProvisionVirtualTerminalCommand` race condition on concurrent calls for same agent | Unique index catches it at DB level; the second transaction gets a constraint violation that surfaces as 409                       |
| `OutletField.isOperational()` predicate not exhaustive if a new field is added      | The enum is the only valid extension point; ArchUnit rule can enforce that all `OutletField` constants implement `isOperational()` |
| `app_setting` unique index drop/recreate fails if concurrent DB access              | Acceptable: this only runs on a fresh DB                                                                                           |
| `TerminalStatus.ARCHIVED` transition is permanent — no restore path                 | By design; if needed, a new command `RestoreTerminalCommand` can be added in a future change                                       |

## Migration Plan

> Precondition: `rename-pos-to-terminal` is fully applied. DB recreated from scratch.

1. **`V5__core_terminal.sql`** — add columns, constraints, partial unique index.
2. **`V4__core_outlet_table.sql`** — add columns, constraints, index.
3. **`V8__core_pos.sql`** — add z-report columns to `sales_session`; drop/recreate status and close constraints to include `ABORTED`.
4. **`V3__core_settings.sql`** — add `agent_id`, override flags; update `level` CHECK; update scope constraint; drop/recreate unique index.
5. **`V32__seed_iam_roles_permissions.sql`** — add permission keys and role-permission rows with `ON CONFLICT DO NOTHING`.
6. **`V43__audit_table.sql`** — add missing columns to `terminal_aud`, `outlet_aud`, `app_setting_aud`, `sales_session_aud`; update level and status constraints in `app_setting_aud` and `sales_session_aud`.
7. **DB re-init** — `docker compose down -v && docker compose up -d postgres`.
8. **Flyway migrate** — `./mvnw flyway:migrate` — exit 0.
9. **Validation gate** — `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` — exit 0.
10. **SettingKey startup gate** — application starts with no `SettingRegistryValidator` failure.
11. **Grep clean check** — no `kind = 'PHYSICAL'` / `kind = 'VIRTUAL'` hardcoded strings in Java source (only enum references).

## Open Questions

- **OQ-1**: `V36__seed_app_settings_batch.sql` seeds `app_setting` rows — are any of those keys absent from the future `SettingKey` enum? → Audit before writing the enum.
- **OQ-2**: Does `core.outlet` already have a `status` column (ACTIVE/INACTIVE)? → Check `V4__core_outlet_table.sql` before the `ChangeOutletStatusCommand` handler references it.
- **OQ-3**: `UserSuspendedEvent` — which package publishes it in `core.user`? → Confirm the event class exists before wiring `SalesSessionAbortOnUserSuspendedListener`.
- **OQ-4**: `TicketPlacedEvent` — does it carry a `sessionId` field already? → Required by `SalesSessionTicketCountersListener`; check `core.sales` event model.
