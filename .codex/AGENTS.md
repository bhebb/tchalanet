# Codex Router — Tchalanet

Codex should use the shared project routers instead of duplicating rules here.

Read first:

- `AGENTS.md`
- `VERSIONS.md`
- `openspec/context/00-index.md`
- `openspec/context/10-non-negotiables.md`
- the component `AGENTS.md` for the files being changed

Common reusable workflows live in:

- `.agents/skills/`

Mode:

- Inspect the smallest relevant file set.
- Prefer focused diffs and targeted validation.
- Do not scan the whole repo unless the task is an explicit audit/inventory.
- Do not modify unrelated components.
- Do not implement broad changes without an OpenSpec change.
