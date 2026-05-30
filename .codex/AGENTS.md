# Codex — Tchalanet

Thin adapter. The canonical router is the root `AGENTS.md`; read it first.

Slice-first: for any task load only
1. `AGENTS.md` (root)
2. the target project `AGENTS.md`
3. one skill from `.agents/skills/`
4. touched files

Target <500 lines outside source code. No global scans without an explicit reason.

Canonical workflow skills live in `.agents/skills/` (see `.agents/README.md`):
ai-safety, openspec-workflow, pr-readiness, handoff, mcp-on-demand, scoped-task, spec-scoping.

Safety: never push to `main`, never force-push, never auto-merge. See `.agents/skills/ai-safety/SKILL.md`.
