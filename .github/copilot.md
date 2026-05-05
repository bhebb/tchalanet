# Copilot Router — Tchalanet

This file is intentionally short. Copilot should follow canonical routers
instead of carrying duplicated architecture rules.

Read first:

1. `AGENTS.md`
2. `VERSIONS.md`
3. `openspec/context/00-index.md`
4. `openspec/context/10-non-negotiables.md`
5. the component `AGENTS.md` for the touched scope

Component routers:

- Backend: `tchalanet-server/AGENTS.md`
- Web: `apps/tchalanet-web/AGENTS.md`
- Mobile: `tchalanet-mobile/AGENTS.md`
- Edge: `tchalanet-edge-service/AGENTS.md`
- Infra: `tchalanet-infra/AGENTS.md`
- Docs: `tchalanet-docs/AGENTS.md`

Rules:

- Load only relevant context, usually 2-4 packs.
- Keep generated code compatible with `VERSIONS.md`.
- Use OpenSpec before broad features, architecture changes, or refactors.
- Prefer near-code docs and existing local patterns.
- Do not duplicate long rules in Copilot prompts.
