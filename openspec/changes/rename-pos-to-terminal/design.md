# Design — `rename-pos-to-terminal`

> How to implement the rename. Reference [proposal.md](proposal.md) for the "why".
> Reference [specs/](specs/) for acceptance criteria.

## Context

**Current state** (measured from source):

| Scope                                                          | Count                               |
| -------------------------------------------------------------- | ----------------------------------- |
| Java files under `core.pos.**`                                 | 28 files                            |
| Java files referencing `PosSession*` (production)              | ~43 files                           |
| Java files referencing `core.pos.*` imports (total with tests) | ~76 files                           |
| SQL migration files containing `pos_session`                   | 4 files (`V8`, `V35`, `V40`, `V43`) |

`core.session` contains more `PosSession*` classes than the initial spec listed. The full rename scope includes `PosSessionTotals`, `PosSessionStatus`, all `PosSessionTotals*` variants, and all their adapters, ports, mappers and listeners (see spec-01 for the authoritative map).

External dependents of `core.pos` / `PosSession*`:

- `core.sales` (imports `PosSession` for ticket validation)
- `features.stats.cashier_dashboard` (imports session read adapter)

## Goals / Non-Goals

**Goals:**

- Zero occurrences of `core.pos` package in production or test source after the change.
- Zero occurrences of any `PosSession*` class name in production or test source.
- Zero occurrences of `pos_session` in any SQL migration file.
- Build passes (`./mvnw compile test`) with no changes to behavior.

**Non-Goals:**

- Field or behavior changes of any kind.
- HTTP path changes.
- New endpoints or domain logic.
- Anything outside `tchalanet-server`.

## Decisions

### D1 — IDE rename (not sed) for Java classes

**Decision**: Use IntelliJ "Rename" refactor on each production class. Do not use shell `sed`.

**Rationale**: IntelliJ rename handles `package` declaration, all `import` statements, Spring bean names, and string literals referencing the class name (e.g., in `@Envers` config). A `sed` bulk-replace risks hitting unrelated strings or missing edge cases like `@Table(name=...)` annotations.

**Alternative considered**: `find … | xargs sed -i 's/PosSession/SalesSession/g'` — rejected because it does not update `package` declarations correctly and can corrupt test fixture filenames.

---

### D2 — Package move: directory rename + adjust `package` declarations

**Decision**: Move the directory `core/pos/` → `core/terminal/` and update every `package com.tchalanet.server.core.pos` declaration.

**Rationale**: The sub-package structure is identical; only the top-level segment changes. A single IDE "Move Package" operation handles all declarations and cross-package imports atomically.

---

### D3 — SQL migrations: in-place edit, no new file

**Decision**: Edit `V8__core_pos.sql`, `V35__seed_outlet_terminal_pos.sql`, `V40__rls_policies.sql`, `V43__audit_table.sql` directly. Do not create a new migration version.

**Rationale**: The DB is recreated from scratch. A `Vxx__rename_pos_session.sql` would add `ALTER TABLE … RENAME` DDL that is pointless when the base migration already uses the new name. In-place editing keeps the migration log clean.

**Constraint**: This approach is only safe because the team explicitly commits to `docker compose down -v` + full DB re-init before applying. Any environment with live data MUST use an additive migration instead.

---

### D4 — Envers `@RevisionEntity` table name: update `@Audited` entity `tableName` or rely on convention

**Decision**: After renaming `PosSessionJpaEntity` → `SalesSessionJpaEntity`, Envers picks up the audit table name from the `@AuditTable` annotation (if present) or falls back to `<table>_aud`. Since `PosSessionJpaEntity` maps to `sales_session` (post-rename of `@Table`), Envers will look for `sales_session_aud`. The `V43` edit must use `sales_session_aud` and `sales_session_totals_aud` accordingly.

**Risk**: If `@AuditTable(name = "pos_session_aud")` is currently hardcoded, it must be removed or updated to `"sales_session_aud"`. This must be verified during implementation.

---

### D5 — `PosSessionTotals*` scope included

**Decision**: The rename scope from spec-01 is extended to cover `PosSessionTotals*` classes and their Envers counterpart `pos_session_totals_aud`.

**Rationale**: The actual source reveals `PosSessionTotals`, `PosSessionTotalsController`, `PosSessionTotalsMapper`, `PosSessionTotalsJpaEntity`, `PosSessionTotalsReaderPort`, `PosSessionTotalsWriterPort`, `PosSessionTotalsAggregatePort`, `PosSessionTotalsProjectionListener`, `RecomputePosSessionTotalsCommand`. All follow the same prefix and must be included.

## Risks / Trade-offs

| Risk                                                                        | Mitigation                                                                            |
| --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| Missed `@AuditTable` annotation still pointing to `pos_session_aud`         | Grep for `@AuditTable` in `core.session` before compiling after rename                |
| `spring.jpa.hibernate.ddl-auto=validate` fails if any column/table mismatch | Run `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` as a gate after DB re-init |
| `core.sales` or `features.stats` still reference old class names            | Compile errors will surface immediately — do not suppress                             |
| Checkstyle or ArchUnit rule catches package rule violation                  | Expected and desired — fix immediately                                                |

## Migration Plan

> Precondition: DB will be recreated from scratch (`docker compose down -v`). No live data at risk.

1. **Edit SQL files in-place** (4 files) — replace all `pos_session` tokens with `sales_session`.
2. **Move Java package** — `core.pos.**` → `core.terminal.**` (IntelliJ "Move Package").
3. **Rename `PosSession*` classes** — apply class rename map from spec-01, including `PosSessionTotals*` variants.
4. **Verify annotations** — check `@AuditTable`, `@Table`, `@Entity` in renamed classes for stale string literals.
5. **Update documentation** — 10 `.md` files listed in §Documentation Updates above; move `DOMAIN_POS.md` → `DOMAIN_TERMINAL.md`.
6. **Compile gate** — `./mvnw compile` must be green.
7. **DB re-init** — `docker compose down -v && docker compose up -d postgres`.
8. **Flyway migrate** — `./mvnw flyway:migrate` — exit 0.
9. **Validation gate** — `./mvnw -Dspring.jpa.hibernate.ddl-auto=validate test` — exit 0.
10. **Grep clean check** — `grep -r "pos_session\|PosSession\|core\.pos" src/ docs/` — zero hits (excluding `openspec/changes/` and archived audits).

## Documentation Updates

All `.md` files referencing `core.pos`, `PosSession*` or `pos_session` must be updated in the same PR.
Measured from source — 10 files:

### Close-to-code docs (`tchalanet-server/src/`)

| File                             | Action                                                                                        |
| -------------------------------- | --------------------------------------------------------------------------------------------- |
| `core/pos/DOMAIN_POS.md`         | **Move** to `core/terminal/DOMAIN_TERMINAL.md`; update title and all class/package references |
| `core/session/DOMAIN_SESSION.md` | Replace `PosSession` → `SalesSession`, `PosSessionRepoPort` → `SalesSessionRepoPort`          |
| `core/sales/DOMAIN_SALES.md`     | Replace `PosSessionReaderPort` → `SalesSessionReaderPort`, `core.pos` → `core.terminal`       |

### Server architecture docs (`tchalanet-server/docs/`)

| File                                        | Action                                                |
| ------------------------------------------- | ----------------------------------------------------- |
| `audit/2026-04-26-sales-pipeline-audit.md`  | Replace all `pos_session` / `PosSession*` occurrences |
| `audit/audit-core-tenantuser-2026-04-27.md` | Replace all `pos_session` / `PosSession*` occurrences |

### Central docs (`tchalanet-docs/docs/`)

| File                                   | Action                                                                                                                                   |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `00-audit/GAPS.md`                     | Replace `PosSessionController` → `SalesSessionController`                                                                                |
| `00-audit/AUDIT-SALES.md`              | Replace all `PosSession*` class names and `pos_session` table references                                                                 |
| `02-functional/domains/pos.md`         | Update link to `DOMAIN_POS.md` → `DOMAIN_TERMINAL.md`                                                                                    |
| `02-functional/flows/sell-ticket.md`   | Replace `PosSessionReaderPort` → `SalesSessionReaderPort`, `PosSessionTotalsProjectionListener` → `SalesSessionTotalsProjectionListener` |
| `02-functional/flows/verify-ticket.md` | Replace `core.pos` → `core.terminal`, update link to `DOMAIN_TERMINAL.md`                                                                |

---

## Open Questions

- **OQ-1**: Is `@AuditTable(name = "pos_session_aud")` hardcoded anywhere in `core.session`? → Verify before step 4.
- **OQ-2**: Does `features.stats.cashier_dashboard` only import the adapter interface or also a `PosSession` domain class? → Check `CashierSessionReadRepositoryAdapter.java` imports to confirm full impact.
