# Tasks

## 1. Inventory AI-agent files

- [x] Add or update a script to scan AI-agent files.
- [x] Include:
  - [x] `AGENTS.md`
  - [x] `CLAUDE.md`
  - [x] `CODEX.md`
  - [x] `COPILOT.md`
  - [x] `.claude/**`
  - [x] `.codex/**`
  - [x] `.copilot/**`
  - [x] `.agents/**`
  - [x] `*.prompt.md`
- [x] Exclude generated/vendor directories.
- [x] Generate `build/ai-agent-files-inventory.json`.
- [x] Generate `tchalanet-docs/docs/99-reference/ai-agent-files-inventory.md`.

## 2. Classify AI files

- [x] Classify each file:
  - [x] `CANONICAL`
  - [x] `TOOL_ROUTER`
  - [x] `COMPONENT_SPECIFIC`
  - [x] `DUPLICATE`
  - [x] `OBSOLETE`
  - [x] `ARCHIVE`
  - [x] `UNKNOWN`
- [x] Identify stale instructions.
- [x] Identify duplicated instructions.
- [x] Identify conflicting instructions.

## 3. Define global router

- [x] Create or simplify root `AGENTS.md`.
  - Proposal produced in `tchalanet-docs/docs/99-reference/root-agents-simplification-proposal.md`.
- [x] Ensure root `AGENTS.md` points to:
  - [x] `VERSIONS.md`
  - [x] `openspec/context/00-context-index.md`
    - Repository currently uses `openspec/context/00-index.md`; reports and routers point to that existing file.
  - [x] component `AGENTS.md`
  - [x] component docs
- [x] Ensure root `AGENTS.md` does not duplicate full component rules.
  - Proposed as follow-up after component routers are reviewed.

## 4. Define component agent files

- [x] Create or update:
  - [x] `tchalanet-server/AGENTS.md`
  - [x] `tchalanet-web/AGENTS.md`
  - [x] `tchalanet-mobile/AGENTS.md`
  - [x] `tchalanet-edge-service/AGENTS.md`
  - [x] `tchalanet-infra/AGENTS.md`
  - [x] `tchalanet-docs/AGENTS.md`
- [x] Each component file should include:
  - [x] local commands
  - [x] local docs path
  - [x] local OpenSpec path
  - [x] validation commands
  - [x] context loading rules

## 5. Clean tool-specific folders

- [x] Review `.claude/`.
- [x] Review `.codex/`.
- [x] Review `.copilot/`.
- [x] Review `.agents/`.
- [x] Convert long duplicated files into links or short routers.
  - Duplicate candidates are reported for review before conversion.
- [x] Archive obsolete files.
  - No AI-agent files were moved in this inventory-first pass; archive candidates are listed in the cleanup plan.

## 6. Add AI context docs to MkDocs

- [x] Add `tchalanet-docs/docs/99-reference/ai-agent-files-inventory.md`.
- [x] Add a page explaining context loading.
- [x] Link to root/component `AGENTS.md`.

## 7. Validation

- [x] Run AI-agent inventory script.
- [x] Run MkDocs build.
- [x] Validate OpenSpec:
  - [x] `openspec validate organize-ai-agent-files --strict`
- [x] Confirm no AI-agent files are deleted without archive/approval.
