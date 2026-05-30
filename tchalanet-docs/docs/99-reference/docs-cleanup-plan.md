# Documentation Cleanup Plan

This plan is generated from the inventory. It proposes review actions only; no document is deleted by the script.

| Action | Status source | Meaning | Count |
| --- | --- | --- | ---: |
| keep | `CANONICAL` | Canonical source; keep in place and link from MkDocs. | 64 |
| keep summary | `SUMMARY` | Curated portal/context summary; keep concise. | 234 |
| link only | `LINK_ONLY` | Keep near owner and expose via MkDocs links. | 291 |
| merge or archive after review | `DUPLICATE` | Confirm canonical source before changing anything. | 54 |
| archive | `ARCHIVE` | Already archived or archive candidate; keep discoverable. | 236 |
| delete later | `DELETE_LATER` | Deletion requires a follow-up reviewed change. | 0 |
| review | `UNKNOWN` | Ownership/action unclear; do not move or delete yet. | 211 |

## Review Queue

| Path | Owner | Status | Recommended action |
| --- | --- | --- | --- |
| `.agents/README.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/mcp-activations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/ai-safety/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/handoff/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/mcp-on-demand/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/pr-readiness/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/scoped-task/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.agents/skills/spec-scoping/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/backend-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/dependabot-review.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/mobile-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/ready-check.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/spec.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/commands/web-task.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/ai-safety/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/handoff/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/mcp-on-demand/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/pr-readiness/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/scoped-task/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.claude/skills/spec-scoping/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.codex/AGENTS.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/ISSUE_TEMPLATE/bug_report.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/ISSUE_TEMPLATE/custom_issue.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/copilot.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `.github/pull_request_template.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `CLAUDE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/SKILL.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/angular-developer/references/angular-animations.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/angular-developer/references/angular-aria.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/cli.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/component-harnesses.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/component-styling.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/components.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/creating-services.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/data-resolvers.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/define-routes.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/defining-providers.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/di-fundamentals.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/e2e-testing.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/effects.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/hierarchical-injectors.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/host-elements.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/angular-developer/references/injection-context.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/inputs.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/angular-developer/references/linked-signal.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/loading-strategies.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/mcp.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/migrations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/navigate-to-routes.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/outputs.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/angular-developer/references/reactive-forms.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/rendering-strategies.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/resource.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/route-animations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/route-guards.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/router-lifecycle.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/router-testing.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/show-routes-with-outlets.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/signal-forms.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/signals-overview.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/tailwind-css.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/template-driven-forms.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/angular-developer/references/testing-fundamentals.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/link-workspace-packages/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/monitor-ci/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/monitor-ci/references/fix-flows.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-generate/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-import/SKILL.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/nx-import/references/ESLINT.md` | root/global | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-web/.agents/skills/nx-import/references/GRADLE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-import/references/JEST.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-import/references/NEXT.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-import/references/TURBOREPO.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-import/references/VITE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-plugins/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-run-tasks/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-workspace/SKILL.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.agents/skills/nx-workspace/references/AFFECTED.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.github/agents/ci-monitor-subagent.agent.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/.github/prompts/monitor-ci.prompt.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/link-workspace-packages/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/monitor-ci/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/monitor-ci/references/fix-flows.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-generate/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/ESLINT.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/GRADLE.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/JEST.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/NEXT.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/TURBOREPO.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-import/references/VITE.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-plugins/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-run-tasks/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-workspace/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.github/skills/nx-workspace/references/AFFECTED.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/agents/ci-monitor-subagent.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/commands/monitor-ci.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/link-workspace-packages/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/monitor-ci/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/monitor-ci/references/fix-flows.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-generate/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/ESLINT.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/GRADLE.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/JEST.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/NEXT.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/TURBOREPO.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-import/references/VITE.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-plugins/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-run-tasks/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-workspace/SKILL.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/.opencode/skills/nx-workspace/references/AFFECTED.md` | root/global | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-web/AGENTS.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/CLAUDE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_AGENTS.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_ARCHITECTURE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_DEV_ARCHITECTURE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_FEATURE_PLAYBOOK.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_NAMING.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_NX_BOUNDARIES.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_PLACEMENT_GUIDE.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/WEB_STATE_MANAGEMENT.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/docs/web/frontend-architecture-todo.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/API-PROXY-FIX.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/DEV-MODES.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/ENV-MIGRATION-SUMMARY.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/ENVIRONMENT-CONFIG.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/I18N-ASSETS-FIX.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/PORTS-CONFIG.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/SESSION-RECAP.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/STYLES-FIX.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/explanations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/legal/privacy.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/legal/regulation.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/legal/responsible.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/official-reports.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/security.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/support.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/en/tchala.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/explanations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/legal/privacy.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/legal/regulation.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/legal/responsible.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/official-reports.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/security.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/support.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/fr/tchala.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/explanations.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/legal/privacy.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/legal/regulation.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/legal/responsible.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/official-reports.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/security.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/support.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/apps/tchalanet-portal/public/assets/content/ht/tchala.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-web/web-backup/libs/ui/styles/src/lib/theme-help.md` | root/global | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-docs/docs/02-functional/domains/sales/public-verify-ticket/single/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/reject-ticket/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/00-index.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/01-backend.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/02-frontend.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/02-functional/domains/sales/void-ticket/03-terminal.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-docs/docs/99-reference/docs-inventory.md` | tchalanet-docs | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-edge-service/AGENTS.md` | tchalanet-edge-service | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-edge-service/CLAUDE.md` | tchalanet-edge-service | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-edge-service/docs/internal-messages-hmac-curl.md` | tchalanet-edge-service | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/AGENTS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/CLAUDE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/compose/docker-compose.index.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/ACTION-REALM-REGEN.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/BUILD-LOCAL-VS-PUBLISHED.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/DEMARRAGE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/DEPLOYMENT.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/ENV-ARCHITECTURE.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/HETZNER.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/IMAGES-DEPLOYMENT.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/OPERATIONS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/VITE-ALLOWED-HOSTS.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/mobile-distribution-v0.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/docs/reference/scripts-inventory.md` | tchalanet-infra | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-infra/docs/scripts-index.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-infra/keycloak/README-DEV.md` | tchalanet-infra | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/AGENTS.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/CLAUDE.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/API_CONTRACT.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/ARCHITECTURE.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/NAMING.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/OFFLINE.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/PLAYBOOK.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/TESTING.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/mobile/01_mobile_product_rules.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/mobile/02_mobile_design_tokens.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/mobile/03_mobile_components.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/mobile/04_mobile_screens_v1.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-mobile/docs/mobile/ui-rules.md` | tchalanet-mobile | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.junie/memory/errors.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.junie/memory/feedback.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/.junie/memory/tasks.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/AGENTS.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/AI.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/CLAUDE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/ARCHITECTURE.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/docs/CALENDARS.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/EDGE-NOTIFICATION-CONFIG.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/NAMING.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/OFFLINE_MODE_FUNCTIONAL_TECHNICAL_DESIGN.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/PLATFORM_TEMPLATE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/PLAYBOOK.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/REDIS-CONFIG.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/RFC_CORE_ARCHITECTURE_INTENSITE_VARIABLE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/adr/ADR-001-modulith-application-service-layer.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/adr/ADR-001-modulith-platform-architecture.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/adr/ADR-001-modulith-platform-layer.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/adr/ADR-002-initial-application-service-migration-scope.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/agents/DECISION_TREE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/architecture/ARCHITECTURE_MODULITH.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/docs/architecture/IMPLEMENTATION_PLAN.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/docs/architecture/MAVEN_MODULES.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/architecture/OPERATIONAL_CONTEXT.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/conventions/batch/tchalanet-draw-results-tick-decision-barriers.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/conventions/persistence/audit.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/modules/catalog.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/modules/common.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/modules/core.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/modules/features.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/modules/platform.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/reference/application-service-modules.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/reference/naming-decision.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/docs/reference/platform-modules.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/openspec/changes/add-offlinesync-module/design.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/add-offlinesync-module/docs/DOMAIN_OFFLINESYNC.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/add-offlinesync-module/proposal.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/add-offlinesync-module/specs/offlinesync/spec.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/add-offlinesync-module/tasks.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/tchalanet-security-login-terminal/specs/auth-context/spec.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/tchalanet-security-login-terminal/specs/terminal-security/spec.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/tchalanet-security-login-terminal/specs/transaction-security/spec.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/changes/tchalanet-security-login-terminal/tasks.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/openspec/specs/features-cashier/spec.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/scripts/E2E_README.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-catalog/src/main/java/com/tchalanet/server/catalog/plan/CATALOG_PLAN.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-common/src/main/java/com/tchalanet/server/common/cache/CACHE.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/limitpolicy/IMPLEMENTATION_MAPPERS.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/offlinesync/ROADMAP.md` | tchalanet-server | UNKNOWN | review: mentions legacy workflow |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/promotion/promotion_design.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/sales/api/model/sale/SaleIssueCatalog.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/subscription/CORE_SUBSCRIPTION.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/terminal/terminal_binding.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/terminal/terminal_onboarding.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/cashier/MOBILE_FLOW.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/cashier/mobile_cashier_flow_v2.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/accesscontrol/PLATFORM_ACCESSCONTROL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/address/PLATFORM_ADDRESS.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/audit/PLATFORM_AUDIT.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/communication/PLATFORM_COMMUNICATION.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/document/PLATFORM_DOCUMENT.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/entitlement/PLATFORM_ENTITLEMENT.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/idempotence/PLATFORM_IDEMPOTENCY.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/identity/PLATFORM_IDENTITY.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/notification/PLATFORM_NOTIFICATION.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/tenantconfig/PLATFORM_TENANTCONFIG.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/tenantconfig/internal/INTERNAL.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/tenantgame/PLATFORM_TENANTGAME.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/tenanttheme/PLATFORM_TENANTTHEME.md` | tchalanet-server | UNKNOWN | review ownership before moving or deleting |
| `tchalanet-server/testing/e2e/.pytest_cache/README.md` | tchalanet-server | DUPLICATE | review duplicate group; keep/link canonical source before archive |
