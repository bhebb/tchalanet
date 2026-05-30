# GitHub Copilot — Tchalanet

Thin adapter. Follow the root `AGENTS.md` first, then the target project `AGENTS.md`.

Slice-first: stay inside the touched project. Load one skill from `.agents/skills/` when relevant (see `.agents/README.md`). Web Nx/Angular guidance: `tchalanet-web/.agents/skills/`.

- Do not introduce a new library without explanation.
- Do not bypass Tchalanet conventions (`docs/`, `openspec/context/`).
- Prefer small, local, testable changes.
- Never push to `main`, never force-push, never auto-merge.
