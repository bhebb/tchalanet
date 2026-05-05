# Root AGENTS.md Simplification Proposal

Root `AGENTS.md` is currently the monorepo source of truth. It should become a
short router once component `AGENTS.md` files are reviewed.

Proposed shape:

1. State that `AGENTS.md` is the global router.
2. Point to `VERSIONS.md`.
3. Point to `openspec/context/00-index.md` and `10-non-negotiables.md`.
4. Point to component `AGENTS.md` files.
5. Keep only cross-project invariants.
6. Move component details to component agent files.

Do not remove current root details until reviewers confirm component routers
cover backend, web, mobile, edge, infra, and docs workflows.

Candidate component routers:

- `tchalanet-server/AGENTS.md`
- `apps/tchalanet-web/AGENTS.md`
- `tchalanet-mobile/AGENTS.md`
- `tchalanet-edge-service/AGENTS.md`
- `tchalanet-infra/AGENTS.md`
- `tchalanet-docs/AGENTS.md`
