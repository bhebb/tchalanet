## Why

Tchalanet is already a multi-scope monorepo, but it does not yet have a Codex/agents setup that clearly enforces:

- OpenSpec-first delivery
- backend as business source of truth
- Angular web as a client
- Flutter as a future client without later refactor pressure

We need a lightweight but explicit setup aligned with the existing repo truth:

- `AGENTS.md`
- `openspec/`
- `tchalanet-server/docs/`
- `tchalanet-docs/docs/`
- `.claude` instructions

## What Changes

- add `.codex/setup.sh`
- add `.codex/instructions.md`
- add focused agent guides in `.agents/`
- update `AGENTS.md` with Codex/OpenSpec operational rules for multiplatform work
- add missing OpenSpec scripts to `package.json`

## Capabilities

### New Capabilities

- `codex-agent-workflow`: defines the Codex/agents workflow for a multi-frontend repo where specs stay UI-agnostic and the backend owns business logic

## Impact

- workflow and documentation only
- no functional changes to backend, web, or mobile code

## Non-Goals

- no Flutter implementation in this change
- no mobile migration
- no backend logic refactor
