# Tchalanet Agents Router

Global AI-agent router. Cross-project routing only — no inline rules.
Durable rules live in `docs/`, `openspec/`, and `VERSIONS.md`. Do not duplicate them here.

## Context budget

For slice work, load only:

1. this file
2. the target project `AGENTS.md`
3. one relevant skill from `.agents/skills/`
4. files being edited or reviewed

Target: <500 lines outside source code. Loading unnecessary context is an error.

## Durable truth (do not duplicate)

- `VERSIONS.md` — runtime and image versions.
- `openspec/context/00-index.md` — context-pack router.
- `openspec/context/05-version-guard.md` — version enforcement.
- `tchalanet-server/openspec/context/10-non-negotiables.md` — backend architecture layers and hard constraints.

## Project routers

- Backend: `tchalanet-server/AGENTS.md`
- Web: `tchalanet-web/AGENTS.md`
- Mobile: `tchalanet-mobile/AGENTS.md`
- Edge: `tchalanet-edge-service/AGENTS.md`
- Infra: `tchalanet-infra/AGENTS.md`
- Docs: `tchalanet-docs/AGENTS.md`

## Shared skills

- Canonical workflow skills: `.agents/skills/` (see `.agents/README.md`).
- Web Nx/Angular skills: `tchalanet-web/.agents/skills/`.
- Adapters are thin pointers: `.claude/`, `.codex/`, `.github/copilot.md`.

## OpenSpec

OpenSpec is mandatory for planning, architecture, refactors, and new capabilities.
Changes live in the touched project's `openspec/changes/`; use root `openspec/changes/` only for cross-project work.
See `.agents/skills/openspec-workflow/SKILL.md`.

## Safety

- If a task touches multiple projects, state the slices explicitly before editing.
- If unsure, stop and ask for scope confirmation.
- Never run global scans (`grep -R`, `find /`, `tree` at root) without an explicit reason.
- Never push to `main`, never force-push, never auto-merge.
- See `.agents/skills/ai-safety/SKILL.md`.
