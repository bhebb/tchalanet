# AGENTS.md — Tchalanet Docs

Documentation agent router for `tchalanet-docs/` (MkDocs).

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical docs (source of truth — do not duplicate here):

- `docs/` — published documentation.
- `mkdocs.yml` — navigation and portal config.
- `openspec/`

OpenSpec:

- Published-docs changes: `tchalanet-docs/openspec/`.
- Root `openspec/` only for cross-project coordination.

Shared workflow skills: `.agents/skills/` (see `.agents/README.md`).

Validation:

- `venv/bin/mkdocs build --config-file mkdocs.yml`
- Strict mode only when nav/link warnings are intentionally resolved.

Context rule:

- Keep MkDocs as a portal. Link to component docs; do not copy long implementation detail.
