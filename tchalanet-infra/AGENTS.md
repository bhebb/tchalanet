# AGENTS.md — Tchalanet Infra

Infra agent router for `tchalanet-infra/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `docs/`
- `CLAUDE.md`
- `openspec/`
- `envs/common/compose.env`

OpenSpec:

- Use `tchalanet-infra/openspec/` for Docker, environment, deployment, and CI/CD changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- `docker compose config`
- Existing `make` targets when relevant.
- Never commit secrets.

Context rule:

- Load root rules, local infra router, `VERSIONS.md`, and only the compose/env files being changed.
