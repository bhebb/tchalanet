# Tasks

## 1. Inventory AI-agent files

- [ ] Add or update a script to scan AI-agent files.
- [ ] Include:
  - [ ] `AGENTS.md`
  - [ ] `CLAUDE.md`
  - [ ] `CODEX.md`
  - [ ] `COPILOT.md`
  - [ ] `.claude/**`
  - [ ] `.codex/**`
  - [ ] `.copilot/**`
  - [ ] `.agents/**`
  - [ ] `*.prompt.md`
- [ ] Exclude generated/vendor directories.
- [ ] Generate `build/ai-agent-files-inventory.json`.
- [ ] Generate `tchalanet-docs/docs/99-reference/ai-agent-files-inventory.md`.

## 2. Classify AI files

- [ ] Classify each file:
  - [ ] `CANONICAL`
  - [ ] `TOOL_ROUTER`
  - [ ] `COMPONENT_SPECIFIC`
  - [ ] `DUPLICATE`
  - [ ] `OBSOLETE`
  - [ ] `ARCHIVE`
  - [ ] `UNKNOWN`
- [ ] Identify stale instructions.
- [ ] Identify duplicated instructions.
- [ ] Identify conflicting instructions.

## 3. Define global router

- [ ] Create or simplify root `AGENTS.md`.
- [ ] Ensure root `AGENTS.md` points to:
  - [ ] `VERSIONS.md`
  - [ ] `openspec/context/00-context-index.md`
  - [ ] component `AGENTS.md`
  - [ ] component docs
- [ ] Ensure root `AGENTS.md` does not duplicate full component rules.

## 4. Define component agent files

- [ ] Create or update:
  - [ ] `tchalanet-server/AGENTS.md`
  - [ ] `tchalanet-web/AGENTS.md`
  - [ ] `tchalanet-mobile/AGENTS.md`
  - [ ] `tchalanet-edge-service/AGENTS.md`
  - [ ] `tchalanet-infra/AGENTS.md`
  - [ ] `tchalanet-docs/AGENTS.md`
- [ ] Each component file should include:
  - [ ] local commands
  - [ ] local docs path
  - [ ] local OpenSpec path
  - [ ] validation commands
  - [ ] context loading rules

## 5. Clean tool-specific folders

- [ ] Review `.claude/`.
- [ ] Review `.codex/`.
- [ ] Review `.copilot/`.
- [ ] Review `.agents/`.
- [ ] Convert long duplicated files into links or short routers.
- [ ] Archive obsolete files.

## 6. Add AI context docs to MkDocs

- [ ] Add `tchalanet-docs/docs/99-reference/ai-agent-files-inventory.md`.
- [ ] Add a page explaining context loading.
- [ ] Link to root/component `AGENTS.md`.

## 7. Validation

- [ ] Run AI-agent inventory script.
- [ ] Run MkDocs build.
- [ ] Validate OpenSpec:
  - [ ] `openspec validate organize-ai-agent-files --strict`
- [ ] Confirm no AI-agent files are deleted without archive/approval.
