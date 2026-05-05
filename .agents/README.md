# Shared AI Agent Files

`.agents/` is the shared home for reusable AI-agent roles and skills.

Canonical reusable skills:

- `.agents/skills/`

Tool-specific folders should point here instead of copying full workflows:

- `.claude/skills/`
- `.claude/commands/`
- `.github/prompts/`
- `.github/skills/`
- `.codex/`

Routing rule:

1. Start with root `AGENTS.md`.
2. Read `VERSIONS.md`.
3. Read the relevant component `AGENTS.md`.
4. Load one shared skill only when it matches the task.

Do not delete old tool-specific files directly. Convert to a router first, then
archive in a reviewed follow-up if the router is no longer needed.
