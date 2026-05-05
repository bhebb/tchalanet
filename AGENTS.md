# AGENTS.md — Tchalanet Monorepo Router

This file is the global AI-agent and contributor router for Tchalanet.
It contains cross-project rules only. Component details live in component
`AGENTS.md`, `CLAUDE.md`, near-code docs, and OpenSpec workspaces.

If a rule here conflicts with `openspec/context/10-non-negotiables.md`, the
non-negotiable wins. Any intentional exception requires an ADR in
`tchalanet-docs/docs/03-adr/`.

## Read First

Before coding or generating docs:

1. Read this file.
2. Read `VERSIONS.md`.
3. Read `openspec/context/00-index.md`.
4. Read `openspec/context/10-non-negotiables.md`.
5. Read the component `AGENTS.md` for the touched area.
6. Load only the near-code docs needed for the task.

Use 2-4 context packs max. Do not load the whole repository unless the task is
an explicit inventory or audit.

## Repository Map

| Scope            | Router                             | Canonical docs                                 |
| ---------------- | ---------------------------------- | ---------------------------------------------- |
| Backend          | `tchalanet-server/AGENTS.md`       | `tchalanet-server/docs/`, `src/**/DOMAIN_*.md` |
| Web              | `apps/tchalanet-web/AGENTS.md`     | `apps/tchalanet-web/`, `libs/**/README.md`     |
| Mobile           | `tchalanet-mobile/AGENTS.md`       | `tchalanet-mobile/`, `apps/tchalanet-mobile/`  |
| Edge service     | `tchalanet-edge-service/AGENTS.md` | `tchalanet-edge-service/`                      |
| Infra            | `tchalanet-infra/AGENTS.md`        | `tchalanet-infra/docs/`                        |
| Docs             | `tchalanet-docs/AGENTS.md`         | `tchalanet-docs/docs/`                         |
| Common AI skills | `.agents/README.md`                | `.agents/skills/`                              |

Tool-specific folders are adapters only:

- `.claude/` — Claude commands, agents, and skill routers.
- `.codex/` — Codex startup routing.
- `.github/copilot.md` and `.github/prompts/` — Copilot routing.
- `.agents/` — shared agent roles and canonical reusable skills.

## OpenSpec

OpenSpec is mandatory for planning, architecture changes, refactors, new
domains, and broad documentation organization.

- Global/cross-project changes: `openspec/changes/`
- Backend changes: `tchalanet-server/openspec/changes/`
- Web changes: `apps/tchalanet-web/openspec/changes/`
- Mobile changes: `tchalanet-mobile/openspec/changes/`
- Edge changes: `tchalanet-edge-service/openspec/changes/`
- Infra changes: `tchalanet-infra/openspec/changes/`
- Docs changes: `tchalanet-docs/openspec/changes/`

Do not implement a new capability or broad refactor without an OpenSpec change.
Keep global OpenSpec context light; component details stay in component
OpenSpec and near-code docs.

## Cross-Project Invariants

- Runtime and image versions are owned by `VERSIONS.md`.
- Backend layers are strict: `common`, `catalog`, `core`, `features`.
- `core` must not depend on `features` or `catalog`.
- `catalog` must not emit domain events or own business invariants.
- `common` must not contain business logic.
- Controllers stay thin; business truth stays in domain/application layers.
- Side effects are published after commit.
- Typed IDs are used outside persistence.
- Web is Angular/Nx, mobile-first, token-themed, i18n-aware.
- Mobile is Flutter-first and must not depend on web implementation details.
- Infra must not use `:latest` image tags or commit secrets.
- Edge templates and rules stay in the edge-service project.

## Documentation Rules

- One source of truth per information type.
- Stable published docs: `tchalanet-docs/docs/`.
- Implementation details: docs near the owning code.
- Active SDD/spec work: the owning `openspec/changes/`.
- MkDocs is a portal and navigation layer, not a dump of every component doc.

## Git And Validation

- Never push directly to `main`.
- Prefer a dedicated feature/chore branch.
- Do not revert unrelated user changes.
- Validate with the narrowest relevant command first.
- Run `pnpm ops:check` when the root OpenSpec command is expected to work; if it
  fails because no target is configured, use explicit `openspec validate ...`.

## Shared AI Files

Do not duplicate long rules across `.claude`, `.codex`, `.github`, and
`.agents`.

- Put reusable workflows in `.agents/skills/`.
- Keep tool-specific files short and point them to canonical sources.
- Archive obsolete AI files before deletion.
- Review generated inventories under `tchalanet-docs/docs/99-reference/`.
