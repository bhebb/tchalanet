# Spec 02 — Database table rename

> Delta spec for capability `core.session` — table-level rename only, no data migration.
> DB is recreated from scratch: existing migration files are edited in-place.
> No new Flyway migration file is added for this rename.

## Scope

Tables concerned:

| Old name                 | New name                   | Migration file         |
| ------------------------ | -------------------------- | ---------------------- |
| `pos_session`            | `sales_session`            | `V8__core_pos.sql`     |
| `pos_session_totals`     | `sales_session_totals`     | `V8__core_pos.sql`     |
| `pos_session_aud`        | `sales_session_aud`        | `V43__audit_table.sql` |
| `pos_session_totals_aud` | `sales_session_totals_aud` | `V43__audit_table.sql` |

Other files to edit in-place: `V40__rls_policies.sql`, `V35__seed_outlet_terminal_pos.sql`.

---

## MODIFIED Requirements

### Requirement: Table `sales_session` is the canonical persistence table for sessions

The table previously named `pos_session` SHALL be renamed `sales_session` in `V8__core_pos.sql`.
All dependent database objects (indexes, trigger, check constraints) SHALL be renamed accordingly in the same file.
No additive migration file is created.

Rename map for `pos_session`:

| Old object                         | New object                           |
| ---------------------------------- | ------------------------------------ |
| `pos_session`                      | `sales_session`                      |
| `ux_pos_session_open_per_terminal` | `ux_sales_session_open_per_terminal` |
| `ix_pos_session_tenant_terminal`   | `ix_sales_session_tenant_terminal`   |
| `ix_pos_session_tenant_opened_at`  | `ix_sales_session_tenant_opened_at`  |
| `trg_pos_session_updated_at`       | `trg_sales_session_updated_at`       |

#### Scenario: Table exists under new name

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `SELECT 1 FROM sales_session LIMIT 1` succeeds

#### Scenario: Old table name does not exist

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `SELECT 1 FROM pos_session LIMIT 1` returns an error (relation does not exist)

#### Scenario: Trigger renamed

- **WHEN** triggers on `sales_session` are listed
- **THEN** `trg_sales_session_updated_at` exists and `trg_pos_session_updated_at` does not

---

### Requirement: Table `sales_session_totals` replaces `pos_session_totals`

The child table `pos_session_totals` and all its objects SHALL be renamed in `V8__core_pos.sql`.
The FK reference `session_id` SHALL point to `sales_session(id)`.

Rename map for `pos_session_totals`:

| Old object                             | New object                               |
| -------------------------------------- | ---------------------------------------- |
| `pos_session_totals`                   | `sales_session_totals`                   |
| `ux_pos_session_totals_session_id`     | `ux_sales_session_totals_session_id`     |
| `ix_pos_session_totals_tenant`         | `ix_sales_session_totals_tenant`         |
| `ix_pos_session_totals_tenant_session` | `ix_sales_session_totals_tenant_session` |
| `trg_pos_session_totals_updated_at`    | `trg_sales_session_totals_updated_at`    |

#### Scenario: Child table exists under new name

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `SELECT 1 FROM sales_session_totals LIMIT 1` succeeds

#### Scenario: FK references new parent

- **WHEN** `sales_session_totals` schema is inspected
- **THEN** `session_id` references `sales_session(id)` ON DELETE CASCADE; no reference to `pos_session` remains

---

### Requirement: Envers audit tables renamed in `V43__audit_table.sql`

The Hibernate Envers audit tables SHALL be renamed in-place in `V43__audit_table.sql`.
No new migration file is created.

Rename map for audit tables:

| Old table                | New table                  |
| ------------------------ | -------------------------- |
| `pos_session_aud`        | `sales_session_aud`        |
| `pos_session_totals_aud` | `sales_session_totals_aud` |

Constraint renames in `sales_session_aud`:

| Old constraint                 | New constraint                   |
| ------------------------------ | -------------------------------- |
| `pos_session_aud_pkey`         | `sales_session_aud_pkey`         |
| `pos_session_aud_status_check` | `sales_session_aud_status_check` |

Constraint renames in `sales_session_totals_aud`:

| Old constraint                | New constraint                  |
| ----------------------------- | ------------------------------- |
| `pos_session_totals_aud_pkey` | `sales_session_totals_aud_pkey` |

#### Scenario: Audit tables exist under new names

- **WHEN** a fresh DB is created and all migrations run
- **THEN** `SELECT 1 FROM sales_session_aud LIMIT 1` and `SELECT 1 FROM sales_session_totals_aud LIMIT 1` succeed

#### Scenario: Old audit table names do not exist

- **WHEN** all audit tables are listed after migration
- **THEN** `pos_session_aud` and `pos_session_totals_aud` are absent

---

### Requirement: RLS policies list updated in `V40__rls_policies.sql`

The array literal referencing `'pos_session'` in `V40__rls_policies.sql` SHALL be renamed to `'sales_session'`.
`'pos_session_totals'` if present SHALL also be renamed to `'sales_session_totals'`.

#### Scenario: RLS policy active on sales_session

- **WHEN** RLS policies on `sales_session` are listed after migration
- **THEN** the tenant isolation policy exists on `sales_session`

#### Scenario: No RLS policy on old name

- **WHEN** RLS policies are listed after migration
- **THEN** no policy targets `pos_session`

---

### Requirement: Seed file updated in `V35__seed_outlet_terminal_pos.sql`

All references to `pos_session` in `V35__seed_outlet_terminal_pos.sql`
(INSERT, SELECT, RAISE NOTICE, comments) SHALL be renamed to `sales_session`.

#### Scenario: Seed runs without error

- **WHEN** all migrations run on a fresh DB with tenant `tchalanet` present
- **THEN** the seed inserts a row into `sales_session` without error

---

### Requirement: No `pos_session` reference remains in any migration file

After editing all files in-place, no SQL file in `src/main/resources/db/migration` SHALL contain
the token `pos_session` (as table name, index name, trigger name, policy name, constraint name, or comment).

#### Scenario: Migration files are clean

- **WHEN** all `.sql` files under `src/main/resources/db/migration` are grepped for `pos_session`
- **THEN** zero occurrences are found

---

### Requirement: Backward compatibility is not maintained via view alias

No SQL VIEW named `pos_session` SHALL be created as a backward-compatibility shim.
Any downstream consumer referencing `pos_session` MUST be updated directly.

#### Scenario: No view alias for old name

- **WHEN** the database schema is inspected after migration
- **THEN** no view named `pos_session` exists
