# Spec 05 — Permissions added to `core.accesscontrol`

## Domain

`core.accesscontrol` (extension — new permission keys and role assignments).

## ADDED Requirements

### Requirement: Terminal permission keys defined and seeded

The following permission keys SHALL exist in the permission catalogue and be assigned to the listed default roles via Flyway seed:

| Key                   | Default roles       |
| --------------------- | ------------------- |
| `terminal.read`       | TENANT_ADMIN, AGENT |
| `terminal.write`      | TENANT_ADMIN        |
| `terminal.self.write` | AGENT (revocable)   |

#### Scenario: AGENT has terminal.read by default

- **WHEN** an AGENT's effective permissions are resolved
- **THEN** `terminal.read` is present

#### Scenario: AGENT does not have terminal.write by default

- **WHEN** an AGENT's effective permissions are resolved
- **THEN** `terminal.write` is absent

---

### Requirement: Outlet permission keys defined and seeded

| Key                 | Default roles                                              |
| ------------------- | ---------------------------------------------------------- |
| `outlet.read`       | TENANT_ADMIN, AGENT                                        |
| `outlet.write`      | TENANT_ADMIN                                               |
| `outlet.self.write` | AGENT (revocable; meaningful for owners of MOBILE/VIRTUAL) |

#### Scenario: outlet.self.write is seeded for AGENT

- **WHEN** the seed migration runs
- **THEN** a role-permission row exists for AGENT → `outlet.self.write`

---

### Requirement: Settings permission keys defined and seeded

| Key                      | Default roles           |
| ------------------------ | ----------------------- |
| `setting.read`           | all authenticated users |
| `setting.global.write`   | SUPER_ADMIN             |
| `setting.tenant.write`   | TENANT_ADMIN            |
| `setting.outlet.write`   | TENANT_ADMIN            |
| `setting.terminal.write` | TENANT_ADMIN            |
| `setting.self.write`     | AGENT (revocable)       |
| `setting.restore`        | TENANT_ADMIN            |
| `setting.hard_delete`    | SUPER_ADMIN             |

#### Scenario: SUPER_ADMIN has setting.global.write

- **WHEN** a SUPER_ADMIN's effective permissions are resolved
- **THEN** `setting.global.write` is present

---

### Requirement: Sales session permission keys defined and seeded

| Key             | Default roles |
| --------------- | ------------- |
| `session.read`  | TENANT_ADMIN  |
| `session.abort` | TENANT_ADMIN  |

> Opening and closing one's own session is gated by the AGENT role and ownership in the handler — no permission key required.

#### Scenario: session.abort not seeded for AGENT

- **WHEN** an AGENT's effective permissions are resolved
- **THEN** `session.abort` is absent

---

### Requirement: Sales permission keys defined and seeded

| Key                         | Default roles                         |
| --------------------------- | ------------------------------------- |
| `sales.place`               | AGENT                                 |
| `sales.approve`             | TENANT_ADMIN, OPERATOR                |
| `sales.history.self.read`   | AGENT                                 |
| `sales.history.outlet.read` | TENANT_ADMIN                          |
| `sales.history.tenant.read` | TENANT_ADMIN                          |
| `sales.results.margin.read` | TENANT_ADMIN                          |
| `sales.cancel.self`         | AGENT (within configured time window) |
| `sales.cancel.any`          | TENANT_ADMIN                          |

#### Scenario: AGENT has sales.place by default

- **WHEN** an AGENT's effective permissions are resolved
- **THEN** `sales.place` is present

---

### Requirement: Limits and autonomy permission keys defined

| Key              | Default roles |
| ---------------- | ------------- |
| `limit.read`     | TENANT_ADMIN  |
| `limit.write`    | TENANT_ADMIN  |
| `autonomy.read`  | TENANT_ADMIN  |
| `autonomy.write` | TENANT_ADMIN  |

#### Scenario: AGENT has no limit.write

- **WHEN** an AGENT's effective permissions are resolved
- **THEN** `limit.write` is absent

---

### Requirement: Print and sync permission keys defined

| Key            | Default roles                      |
| -------------- | ---------------------------------- |
| `print.test`   | AGENT (only on PHYSICAL terminals) |
| `sync.trigger` | AGENT                              |

#### Scenario: print.test seeded for AGENT

- **WHEN** the seed migration runs
- **THEN** a role-permission row exists for AGENT → `print.test`

---

### Requirement: Seed migration is idempotent

The Flyway seed migration SHALL use `INSERT … ON CONFLICT DO NOTHING` or equivalent so it is safe to run multiple times on an existing DB.

#### Scenario: Seed re-run does not duplicate rows

- **WHEN** the seed migration is applied to a DB that already contains the permission rows
- **THEN** no duplicate rows are created and the migration exits without error

---

### Requirement: AGENT role does not imply any other role

Granting the `AGENT` role to a user SHALL NOT implicitly grant `TENANT_ADMIN`, `OPERATOR`, or any other role.
An admin who needs to sell must be explicitly assigned both `AGENT` and their admin role.

#### Scenario: AGENT cannot call admin endpoint

- **WHEN** a user with only the AGENT role calls an admin-only endpoint (e.g., `session.abort`)
- **THEN** 403 is returned
