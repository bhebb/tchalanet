# ADR-002 — Initial Application Service Migration Scope

- **Status**: Proposed
- **Depends on**: ADR-001
- **Decision type**: Migration scope snapshot

This ADR captures the initial migration inventory. Unlike ADR-001, this document may be superseded by later ADRs when new capabilities are introduced.

---

## 1. Initial module placement

| Current area                                | Target                | Rationale                                                  | Notes                                                |
| ------------------------------------------- | --------------------- | ---------------------------------------------------------- | ---------------------------------------------------- |
| `core.audit`                                | `<asl>.audit`         | transversal audit trail, stateful, non-game decision owner | audit may use `REQUIRES_NEW` for failure logging     |
| `core.accesscontrol`                        | `<asl>.accesscontrol` | application permissions and role assignments               | core business eligibility still stays in owning core |
| `core.tenantuser`                           | `platform.identity`   | app user/profile/context, used transversally               | high fan-in; requires bridge migration               |
| `core.tenantconfig`                         | `<asl>.tenantconfig`  | effective tenant values and overrides                      | catalog may own setting definitions                  |
| `core.tenanttheme`                          | `<asl>.tenanttheme`   | effective tenant theme and overrides                       | catalog may own global theme presets                 |
| `common.document` if stateful/workflow      | `<asl>.document`      | document generation/storage lifecycle                      | pure PDF/QR utility can remain common                |
| `common.communication` if stateful/workflow | `<asl>.communication` | delivery lifecycle, templates, providers                   | pure interface primitives can remain common          |
| `common.idempotence` persistence/workflow   | `<asl>.idempotence`   | idempotency records and replay workflow                    | annotations/interfaces may remain common             |
| `common.security` decisions                 | `<asl>.accesscontrol` | application authorization decisions                        | Spring glue remains common.security                  |

---

## 2. Explicit split rules

### Security

```text
common.security
  Spring Security glue, annotations, principal helpers, permission evaluator bridge

<asl>.accesscontrol
  roles, assignments, permission checks, access policies, admin APIs, persistence
```

### Idempotence

```text
common.idempotence
  annotations, marker interfaces, request-key value objects if stateless

<asl>.idempotence
  idempotency records, repositories, replay policy, cleanup jobs, attempt logs
```

### Document

```text
common.document
  pure render primitives only if stateless and generic

<asl>.document
  document metadata, generated artifacts, templates, storage, lifecycle, audit hooks
```

### Communication

```text
common.communication
  marker interfaces / common value objects only if stateless

<asl>.communication
  message delivery, provider adapters, templates, retry, delivery status, ops endpoints
```

---

## 3. Migration order

Order is based on expected fan-in and risk. Measure actual fan-in before implementation.

### Phase 0 — Baseline measurement

For each candidate module, record:

```bash
rg "core\.tenantuser|core\.accesscontrol|core\.tenantconfig|core\.tenanttheme|core\.audit" tchalanet-server/src/main/java
rg "common\.document|common\.communication|common\.idempotence|common\.security" tchalanet-server/src/main/java
```

Classify:

| Fan-in               | Migration style                     |
| -------------------- | ----------------------------------- |
| 0-5 importing files  | direct package move acceptable      |
| 6-20 importing files | two-step API bridge recommended     |
| >20 importing files  | mandatory three-PR bridge migration |

### Phase 1 — Low-risk archetype pilots

1. `<asl>.communication` or `<asl>.document` if mostly isolated.
2. `<asl>.audit` if transaction rules are clear.

Goal: prove structure, tests, Modulith, ArchUnit, transaction/context rules.

### Phase 2 — Medium-risk stateful transversal modules

1. `<asl>.tenanttheme`
2. `<asl>.tenantconfig`
3. `<asl>.accesscontrol`

### Phase 3 — High fan-in user context

1. `platform.identity`
2. bridge existing imports
3. move implementation
4. remove legacy packages

---

## 4. Bridge migration for high fan-in modules

For modules with fan-in > 20 dependent files, use three PRs.

### PR 1 — Create target API bridge

Create `<asl>.<x>.api` that re-exports or wraps existing `core.<x>` contracts.

No implementation move yet.

### PR 2 — Flip imports progressively

Replace consumers:

```text
core.<x>.*  -> <asl>.<x>.api.*
```

No behavior change.

### PR 3 — Move implementation

Move implementation to `<asl>.<x>.internal`.
Delete legacy `core.<x>` package.
Remove bridge compatibility.

---

## 5. Tests migration

Each migration PR must include:

- unit tests moved with the module;
- integration tests audited for dependent modules;
- fixtures/builders moved or wrapped;
- ArchUnit/Spring Modulith baseline updated;
- import fan-in report attached to PR notes.

---

## 6. Stop criteria

The migration is not complete until these packages no longer exist, unless a later ADR explicitly changes the target:

```text
core.audit
core.accesscontrol
core.tenantuser
core.tenantconfig
core.tenanttheme
```

Final ArchUnit stop rule:

```text
noClasses().should().resideInAnyPackage(
  "..core.audit..",
  "..core.accesscontrol..",
  "..core.tenantuser..",
  "..core.tenantconfig..",
  "..core.tenanttheme.."
)
```

During migration, this rule may exist as a documented pending gate in baseline mode. It becomes blocking at the end.

---

## 7. Freeze rule

After ADR-001 is Accepted:

- no new transversal stateful code may be added to `common`;
- no new non-core transversal module may be added to `core`;
- ambiguous placement requires a new ADR or OpenSpec decision before implementation.
