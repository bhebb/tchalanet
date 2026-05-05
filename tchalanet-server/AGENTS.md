# AGENTS.md — Tchalanet Server

Backend agent router for `tchalanet-server/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`
- `openspec/context/10-non-negotiables.md`

Canonical local docs:

- `docs/ARCHITECTURE.md`
- `docs/PLAYBOOK.md`
- `docs/NAMING.md`
- `docs/conventions/`
- `src/**/DOMAIN_*.md`
- `src/**/FEATURE_*.md`

OpenSpec:

- Use `tchalanet-server/openspec/` for backend changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- `./mvnw test`
- `./mvnw verify`
- Relevant focused tests for touched packages.

Context rule:

- Load root rules, local `CLAUDE.md`, one relevant convention pack, and near-code docs for the touched domain.
