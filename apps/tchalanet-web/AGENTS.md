# AGENTS.md — Tchalanet Web

Web agent router for `apps/tchalanet-web/` and `libs/`.

Read first:

- `../../AGENTS.md`
- `../../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `README.md`
- `CLAUDE.md`
- `openspec/`
- `libs/**/README.md`
- `libs/ui/widget-renderer/README.md`

OpenSpec:

- Use `apps/tchalanet-web/openspec/` for Angular/Nx changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- Use existing Nx/pnpm targets for the touched app or library.
- Keep validation focused on changed web surfaces.

Context rule:

- Load root rules, local web router, one relevant frontend/design doc, and touched component files.
