# .agents — Canonical agent skills

Single source of truth for reusable agent workflows across Tchalanet.
Tool adapters (`.claude/`, `.codex/`, `.github/copilot.md`) are thin pointers to these skills — they must not duplicate rules.

Rule: a skill = one responsibility, ≤120 lines. Workflow skills only — no business/domain skills.

## Shared skills

- `skills/ai-safety/` — sensitive actions, forbidden commands, approval gate.
- `skills/openspec-workflow/` — when and where to create OpenSpec changes.
- `skills/pr-readiness/` — pre-PR checklist and per-project validation.
- `skills/handoff/` — compact session handoff format.
- `skills/mcp-on-demand/` — MCP activation rule + log.
- `skills/scoped-task/` — default skill for a bounded single-project task.
- `skills/spec-scoping/` — turn an idea into an implementable spec (no code).

## Per-project skills

- Web Nx/Angular: `tchalanet-web/.agents/skills/` (`angular-developer`, `nx-workspace`, `nx-generate`, `nx-run-tasks`, `nx-import`, `nx-plugins`, `link-workspace-packages`, `monitor-ci`).

## MCP

On-demand only. No permanent MCP by default. Log activations in `mcp-activations.md`; audit monthly. See `skills/mcp-on-demand/`.

## Routing

Start at root `AGENTS.md`, then the target project `AGENTS.md`, then one skill here, then touched files.
