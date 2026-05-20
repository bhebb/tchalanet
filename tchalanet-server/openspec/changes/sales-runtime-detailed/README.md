# Tchalanet OpenSpec Pack — Sales Runtime MVP

## Purpose

This pack defines the ordered changes needed to stabilize the MVP runtime flow:

1. tenant/outlet/terminal operational setup
2. seller sales session
3. sell ticket
4. result/settlement alignment
5. payout
6. tenant admin onboarding/overview
7. cashier seller flow

## Important architecture rule

Core domains keep their canonical CRUD/read/use-case endpoints.

Features do not replace core endpoints.

```text
core = source of truth + domain CRUD/use-cases
features = orchestration / overview / onboarding / UX workflows
catalog = stable declarative registry
```

## Correct interpretation

- `features.tenantadmin.overview` may aggregate data.
- But outlet/terminal/session/payout must still expose their own admin/tenant/ops read endpoints.
- Claude/Codex must not hide missing core queries behind feature overview endpoints.

## Implementation order

```text
00-db-reset-core-operational-sales
01-core-outlet-operational-context
02-core-terminal-runtime
03-core-session-sales-session
03b-core-session-draw-lifecycle-auto-open-close
04-core-limitpolicy-autonomy
05-core-sales-alignment
06-core-payout-alignment
07-features-tenantadmin-onboarding-overview
08-features-cashier-seller-flow
09-cleanup-remove-obsolete-code
```

## Mandatory context packs

Before coding, read:

- `AGENTS.md`
- `VERSIONS.md`
- `openspec/AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/PLAYBOOK.md`
- `docs/conventions/web_api.md`
- `docs/conventions/command_query_handlers.md`
- `docs/conventions/typed_ids.md`
- `docs/conventions/rls.md`
- `docs/conventions/persistence.md`
- `docs/conventions/event_model.md`
- `docs/conventions/audit.md`
