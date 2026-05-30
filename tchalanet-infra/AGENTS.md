# AGENTS.md — Tchalanet Infra

Infra agent router for `tchalanet-infra/`.

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `docs/architecture/` — ENV-ARCHITECTURE.md, BUILD-LOCAL-VS-PUBLISHED.md
- `docs/setup/` — DEMARRAGE.md, LAN-SETUP.md, DOPPLER-SETUP-GUIDE.md
- `docs/operations/` — DEPLOYMENT.md, OPERATIONS.md, HETZNER.md, IMAGES-DEPLOYMENT.md
- `docs/services/` — EDGE-SERVICE.md, VITE-ALLOWED-HOSTS.md, ACTION-REALM-REGEN.md
- `docs/reference/` — QUICK-REFERENCE.md, scripts-index.md
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
