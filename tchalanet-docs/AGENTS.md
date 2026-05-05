# AGENTS.md — Tchalanet Docs

Documentation agent router for `tchalanet-docs/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `docs/`
- `mkdocs.yml`
- `CLAUDE.md`
- `openspec/`

OpenSpec:

- Use `tchalanet-docs/openspec/` for published documentation changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- `venv/bin/mkdocs build --config-file mkdocs.yml`
- Use strict mode only when nav coverage/link warnings are intentionally resolved.

Context rule:

- Keep MkDocs as a portal. Link to component docs instead of copying long implementation detail.
