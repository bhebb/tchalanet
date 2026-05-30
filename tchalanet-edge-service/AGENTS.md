# AGENTS.md — Tchalanet Edge Service

Edge-service agent router for `tchalanet-edge-service/` (Node/Fastify).

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical docs (source of truth — do not duplicate here):

- `README.md`
- `docs/`
- `openspec/`
- Notification templates and routing rules stay in this project (under `src/`).

OpenSpec:

- Edge-service changes: `tchalanet-edge-service/openspec/`.
- Root `openspec/` only for cross-project coordination.

Shared workflow skills: `.agents/skills/` (see `.agents/README.md`).

Validation:

- `npm run typecheck`
- `npm test`
- `npm run build`

Context rule:

- Load root rules, this router, the relevant template/rule docs, and touched module files.
