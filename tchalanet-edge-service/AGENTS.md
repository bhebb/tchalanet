# AGENTS.md — Tchalanet Edge Service

Edge-service agent router for `tchalanet-edge-service/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `README.md`
- `CLAUDE.md`
- `openspec/`
- `templates/`
- `rules/`

OpenSpec:

- Use `tchalanet-edge-service/openspec/` for edge-service changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- `npm run typecheck`
- `npm test`
- `npm run build`

Context rule:

- Load root rules, local edge router, relevant template/rule docs, and touched module files.
