# CLAUDE.md — Tchalanet

Entry point for Claude Code. This file is a pointer, not a rule store.

1. Read `AGENTS.md` (root router).
2. Read the target project `AGENTS.md` for the slice you touch.
3. Load one relevant skill from `.agents/skills/` (see `.agents/README.md`).

Slice-first: load only the router + one skill + touched files. Target <500 lines outside source code.
Safety: `.agents/skills/ai-safety/SKILL.md`. Never push to `main`, never force-push, never auto-merge.
