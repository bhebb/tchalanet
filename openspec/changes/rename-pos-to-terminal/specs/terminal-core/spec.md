# Spec 01 — Package & class rename map

## ADDED Requirements

### Requirement: Package `core.terminal` replaces `core.pos`

The codebase SHALL NOT contain any class under the package `com.tchalanet.server.core.pos`.
All classes previously under `core.pos.**` SHALL be moved to the equivalent path under `core.terminal.**`,
preserving the same sub-package structure.

#### Scenario: Package no longer exists

- **WHEN** the full source tree is scanned for `com.tchalanet.server.core.pos`
- **THEN** zero Java source files are found under that package

#### Scenario: Sub-packages preserved

- **WHEN** a class was at `core.pos.domain.model.Terminal`
- **THEN** it is now at `core.terminal.domain.model.Terminal` with identical content except the `package` declaration

#### Scenario: Compile succeeds

- **WHEN** `./mvnw compile` is executed after the rename
- **THEN** exit code is 0 with no unresolved symbol errors

---

### Requirement: Classes in `core.session` renamed from `PosSession*` to `SalesSession*`

The following class renames SHALL be applied in `core.session`:

| Old name                  | New name                    | Location                               |
| ------------------------- | --------------------------- | -------------------------------------- |
| `PosSession`              | `SalesSession`              | `core.session.domain.model`            |
| `PosSessionView`          | `SalesSessionView`          | `core.session.application.query.model` |
| `PosSessionRepoPort`      | `SalesSessionRepoPort`      | `core.session.application.port.out`    |
| `PosSessionResponse`      | `SalesSessionResponse`      | `core.session.infra.web.model`         |
| `PosSessionController`    | `SalesSessionController`    | `core.session.infra.web`               |
| `PosSessionMapper`        | `SalesSessionMapper`        | `core.session.infra.web`               |
| `PosSessionJpaEntity`     | `SalesSessionJpaEntity`     | `core.session.infra.persistence`       |
| `JpaPosSessionRepository` | `JpaSalesSessionRepository` | `core.session.infra.persistence`       |

No class named `PosSession*` or `JposSession*` SHALL remain in production source.

#### Scenario: No PosSession class remains

- **WHEN** the production source tree is searched for classes named `PosSession*`
- **THEN** zero matches are found

#### Scenario: SalesSessionController is the active controller

- **WHEN** the application starts and `/tenant/sessions` is requested
- **THEN** `SalesSessionController` handles the request

---

### Requirement: All imports updated to reflect new names

Every Java source file in the project SHALL import from `core.terminal.*` (not `core.pos.*`)
and SHALL reference the new `SalesSession*` class names.

#### Scenario: No stale import remains

- **WHEN** the entire source tree is grepped for `import com.tchalanet.server.core.pos.`
- **THEN** zero occurrences are found

#### Scenario: No stale PosSession import remains

- **WHEN** the source tree is grepped for `import .*PosSession`
- **THEN** zero occurrences are found

---

### Requirement: Stable identifiers are NOT renamed

The following SHALL remain unchanged:

- `TerminalId` typed ID class
- `SessionId` typed ID class
- HTTP path `/tenant/sessions/*`
- Field names `terminal_id`, `outlet_id` in any table
- Audit action names (e.g. `SESSION_OPEN`)

#### Scenario: TerminalId unchanged

- **WHEN** the source file for `TerminalId` is read
- **THEN** the class name and package match the pre-change state

---

### Requirement: Documentation updated to reflect new names

All `.md` files in the repository that reference `core.pos`, `PosSession*`, or `pos_session` SHALL be
updated in the same change. No stale documentation reference SHALL remain.

Specific files and their required actions:

| File                                                              | Action                                                                          |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `core/pos/DOMAIN_POS.md`                                          | Moved to `core/terminal/DOMAIN_TERMINAL.md`; title and all references updated   |
| `core/session/DOMAIN_SESSION.md`                                  | `PosSession` → `SalesSession`, `PosSessionRepoPort` → `SalesSessionRepoPort`    |
| `core/sales/DOMAIN_SALES.md`                                      | `PosSessionReaderPort` → `SalesSessionReaderPort`, `core.pos` → `core.terminal` |
| `tchalanet-server/docs/audit/2026-04-26-sales-pipeline-audit.md`  | All `pos_session` / `PosSession*` replaced                                      |
| `tchalanet-server/docs/audit/audit-core-tenantuser-2026-04-27.md` | All `pos_session` / `PosSession*` replaced                                      |
| `tchalanet-docs/docs/00-audit/GAPS.md`                            | `PosSessionController` → `SalesSessionController`                               |
| `tchalanet-docs/docs/00-audit/AUDIT-SALES.md`                     | All `PosSession*` and `pos_session` replaced                                    |
| `tchalanet-docs/docs/02-functional/domains/pos.md`                | Link updated to `DOMAIN_TERMINAL.md`                                            |
| `tchalanet-docs/docs/02-functional/flows/sell-ticket.md`          | Port and listener names updated                                                 |
| `tchalanet-docs/docs/02-functional/flows/verify-ticket.md`        | `core.pos` → `core.terminal`, link updated                                      |

#### Scenario: No stale doc reference remains

- **WHEN** all `.md` files in the repo are grepped for `core\.pos` and `PosSession`
- **THEN** zero occurrences are found outside of `openspec/changes/` and archived audit documents

---

### Requirement: Test classes renamed to match production class map

Test classes SHALL follow the same rename map as production classes:

| Old name                   | New name                     |
| -------------------------- | ---------------------------- |
| `PosSessionControllerTest` | `SalesSessionControllerTest` |
| `PosSessionRepoPortTest`   | `SalesSessionRepoPortTest`   |
| `PosSessionMapperTest`     | `SalesSessionMapperTest`     |

Test fixture files referenced by these tests SHALL also be renamed.

#### Scenario: No PosSession test class remains

- **WHEN** the test source tree is searched for files named `PosSession*Test*`
- **THEN** zero matches are found

#### Scenario: Tests pass after rename

- **WHEN** `./mvnw test` is executed
- **THEN** exit code is 0; no test is skipped or broken due to the rename
