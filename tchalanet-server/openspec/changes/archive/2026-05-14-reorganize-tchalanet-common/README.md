# Tchalanet Common Reorganization — OpenSpec bundle

This bundle is a Claude/Codex handoff for reorganizing `tchalanet-common` after reviewing the uploaded `tchalanet-common.zip`.

Primary files:

- `openspec/changes/reorganize-tchalanet-common/proposal.md`
- `openspec/changes/reorganize-tchalanet-common/design.md`
- `openspec/changes/reorganize-tchalanet-common/tasks.md`
- `openspec/changes/reorganize-tchalanet-common/specs/tchalanet-common-organization/spec.md`
- `docs/CLAUDE_COMMON_REORG.md`
- `audit/current-inventory.md`
- `audit/classification.csv`

Operating rule: `common` is a Technical Shared Kernel. It may expose primitives, contracts, typed IDs, web contracts, context contracts, and thin technical adapters only when they do not introduce platform/core/catalog/features ownership or heavy runtime configuration.
