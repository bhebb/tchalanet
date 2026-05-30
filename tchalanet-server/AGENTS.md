# AGENTS.md — Tchalanet Server

Backend agent router for `tchalanet-server/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical docs (source of truth — do not duplicate here):

- `docs/ARCHITECTURE.md` — layers, hexagonal, CQRS, Command/Query Bus, cross-domain calls.
- `docs/PLAYBOOK.md` — workflows, handler templates, inter-domain patterns.
- `docs/NAMING.md` — naming conventions.
- `docs/conventions/` — clean architecture, bus, context, RLS, persistence (e.g. `persistence/persistence.md`).
- `openspec/context/10-non-negotiables.md` — architecture layers and hard constraints.
- `src/**/DOMAIN_*.md`, `src/**/FEATURE_*.md` — near-code invariants and flows.

OpenSpec:

- Backend changes: `tchalanet-server/openspec/`.
- Core module rules: `openspec/context/80-core-rules.md`.
- Feature module rules: `openspec/context/81-features-rules.md`.
- Root `openspec/` only for cross-project coordination.

Architecture: see `openspec/context/10-non-negotiables.md` and `docs/ARCHITECTURE.md`.

DB migrations: see `docs/conventions/persistence/persistence.md` §9. Pré-go-live, ask before creating any new `V*.sql`.

Validation:

- `./mvnw test`, `./mvnw verify`.
- Focused tests for touched packages. `archUnit` enforces layer rules.

Context rule:

- Load root rules, this router, the one relevant convention pack, and near-code `DOMAIN_*.md`/`FEATURE_*.md` for the touched area.
- Prefer narrow reads over whole-repo audits.
