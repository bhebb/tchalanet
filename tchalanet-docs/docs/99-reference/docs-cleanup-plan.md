# Documentation Cleanup Plan

This plan is generated from the inventory. It proposes review actions only; no document is deleted by the script.

| Action | Status source | Meaning | Count |
| --- | --- | --- | ---: |
| keep | `CANONICAL` | Canonical source; keep in place and link from MkDocs. | 74 |
| keep summary | `SUMMARY` | Curated portal/context summary; keep concise. | 180 |
| link only | `LINK_ONLY` | Keep near owner and expose via MkDocs links. | 198 |
| merge or archive after review | `DUPLICATE` | Confirm canonical source before changing anything. | 55 |
| archive | `ARCHIVE` | Already archived or archive candidate; keep discoverable. | 25 |
| delete later | `DELETE_LATER` | Deletion requires a follow-up reviewed change. | 0 |
| review | `UNKNOWN` | Ownership/action unclear; do not move or delete yet. | 127 |

## Review Queue

| Path | Owner | Status | Recommended action |
| --- | --- | --- | --- |
| `.agents/backend.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/frontend-web.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/mobile-flutter.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/reviewer.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/documentation/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/spec.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/backend-reviewer.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/draw-slice-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/drawresult-slice-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/edge-service-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/infra-docker-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/mobile-slice-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/nx-workspace-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/uslottery-slice-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/agents/web-slice-agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/batch-slices.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/handoff.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/infra-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/mobile-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/nx-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/opsx/apply.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/archive.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/bulk-archive.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/continue.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/explore.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/new.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/propose.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/sync.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/opsx/verify.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/review-backend.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/commands/task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/documentation/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-apply-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-apply/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-archive-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-archive/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-bulk-archive-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-continue-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-explore/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-new-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-proposal/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-propose/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-sync-specs/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.claude/skills/openspec-verify-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.codex/AGENTS.md` | root/global | UNKNOWN | review: mentions legacy Ionic mobile workflow |
| `.codex/instructions.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/ISSUE_TEMPLATE/bug_report.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/ISSUE_TEMPLATE/custom_issue.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/copilot.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/prompts/opsx-apply.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-archive.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-bulk-archive.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-continue.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-explore.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-new.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-propose.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-sync.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/prompts/opsx-verify.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/pull_request_template.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/skills/openspec-apply-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-archive-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-bulk-archive-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-continue-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-explore/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-new-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-propose/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-sync-specs/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `.github/skills/openspec-verify-change/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `CLAUDE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-docs/docs/02-functional/domains/sales/public-verify-ticket/single/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/reject-ticket/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/00-index.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/01-backend.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/02-frontend.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/99-reference/docs-cleanup-plan.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/99-reference/docs-inventory.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-edge-service/CLAUDE.md` | tchalanet-edge-service | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-edge-service/openspec/changes/bootstrap-edge-service-fastify/proposal.md` | tchalanet-edge-service | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-infra/.claude/skills/infrastructure/SKILL.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/ACTION-REALM-REGEN.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/BUILD-LOCAL-VS-PUBLISHED.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/CLAUDE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/VITE-ALLOWED-HOSTS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/WEB-KEYCLOAK-CONFIG.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/compose/docker-compose.index.md` | tchalanet-infra | UNKNOWN | review: mentions legacy Ionic mobile workflow |
| `tchalanet-infra/docs/00-infra-charter.md` | tchalanet-infra | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-infra/docs/CLEANUP-FINAL.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/DEMARRAGE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/DEPLOYMENT.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/DOPPLER-DOWNLOAD-SECRETS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/ENV-ARCHITECTURE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/ENV-SEPARATION.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/HETZNER.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/IMAGES-DEPLOYMENT.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/OPERATIONS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/SOLUTION-AUTH-REGISTRY.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/SUMMARY-ENV-OPTIMIZATION.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/TROUBLESHOOTING-SERVICES-FAIL.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/mobile-distribution-v0.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/reference/scripts-inventory.md` | tchalanet-infra | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-infra/docs/scripts-index.md` | tchalanet-infra | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-infra/docs/spec-local-dev-stack-2026-04-27.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-mobile/.claude/skills/flutter/SKILL.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-mobile/CLAUDE.md` | tchalanet-mobile | UNKNOWN | review: mentions legacy Cordova workflow; mentions legacy workflow |
| `tchalanet-mobile/CLAUDE.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-architecture/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-events/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-naming/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-persistence/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-rls/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-testing/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/backend-typed-ids/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/catalog-module/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/common-module/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/core-module/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/feature-module/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/java-best-practices/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/spring-boot-core/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.claude/skills/spring-security/SKILL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/AI.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/CHANGELOG.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/CLAUDE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/ARCHITECTURE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/DRAW_PROVIDER_RESULTSLOT_MATRIX.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/EDGE-NOTIFICATION-CONFIG.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/NAMING.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/NOTIFICATION-OPS-TEST.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/PLAYBOOK.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/audit/2026-04-25-draw-pipeline-audit.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/audit/2026-04-26-sales-pipeline-audit.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/docs/audit/spec-batch-draw-alignment-2026-04-27.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/audit/spec-draw-provider-config-resultslot-2026-04-27.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/conventions/batch/tchalanet-draw-results-tick-decision-barriers.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/conventions/persistence/audit.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/flow/auth_and_context.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/flow/page_model.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/flow/results_pipeline.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/flow/tenant_configuration.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/tenant-admin/PR_CHECKLIST.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/openspec/context/10-non-negotiables.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/context/90-security-flows-guide.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/src/main/java/com/tchalanet/server/common/idempotency/COMMON_IDEMPOTENCY.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/src/main/java/com/tchalanet/server/core/draw/CLAUDE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/src/main/java/com/tchalanet/server/core/drawresult/CLAUDE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/src/main/java/com/tchalanet/server/core/uslottery/CLAUDE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/.claude/skills/angular-nx/SKILL.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/API-PROXY-FIX.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/CLAUDE.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/DEV-MODES.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/ENV-MIGRATION-SUMMARY.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/ENVIRONMENT-CONFIG.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/I18N-ASSETS-FIX.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/PORTS-CONFIG.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/SESSION-RECAP.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/STYLES-FIX.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/explanations.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/legal/privacy.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/legal/regulation.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/legal/responsible.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/official-reports.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/security.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/support.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/en/tchala.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/explanations.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/legal/privacy.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/legal/regulation.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/legal/responsible.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/official-reports.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/security.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/support.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/fr/tchala.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/explanations.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/legal/privacy.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/legal/regulation.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/legal/responsible.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/official-reports.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/security.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/support.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `apps/tchalanet-web/public/assets/content/ht/tchala.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
| `libs/ui/styles/src/lib/theme-help.md` | tchalanet-web | UNKNOWN | review ownership before moving or deleting |
