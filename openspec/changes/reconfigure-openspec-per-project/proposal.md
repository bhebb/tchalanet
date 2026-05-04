# Change: Reconfigure OpenSpec per project

## Status

Proposed

## Owner

Tchalanet platform

## Context

Tchalanet is now split into multiple autonomous surfaces:

- `tchalanet-server` — backend Java/Spring Boot
- `tchalanet-web` — Angular/Nx web application
- `tchalanet-mobile` — Flutter mobile application
- `tchalanet-edge-service` — Fastify TypeScript edge/delivery service
- `tchalanet-infra` — docker-scripts-deployment
- root workspace — cross-project coordination only

Currently, OpenSpec changes may live in one global `openspec/changes` directory. This makes Claude/Codex/agents load too much context, mix unrelated surfaces, or work from stale worktrees without seeing the correct OpenSpec files.

The goal is to move to a project-local OpenSpec model:

- each project owns its own `openspec/`
- the root `openspec/` remains only for cross-project coordination
- agents must use the nearest project-local OpenSpec
- archive directories must stay inside the OpenSpec where the change was created
- worktree/sandbox usage must be guarded by strict context verification

## Problem

Without project-local OpenSpec boundaries:

- backend tasks may accidentally inspect or modify web/mobile/edge
- web tasks may load backend architectural context unnecessarily
- OpenSpec archive operations may be done manually with `rm`, `cp`, or `mv`
- desktop agents may operate inside stale worktrees
- dependency analysis may be wrong because worktrees may not have current dependencies installed
- agents may not find OpenSpec if it is not committed or is located only at the workspace root

## Goals

- Create a clear OpenSpec structure per project.
- Keep root OpenSpec lightweight and cross-project only.
- Add clear Claude/agent rules to select the correct OpenSpec.
- Prevent manual OpenSpec archiving.
- Ensure OpenSpec directories and archives are versioned.
- Make worktree/sandbox verification mandatory before analysis or edits.
- Reduce token usage by limiting context to the active project.

## Non-goals

- Do not rewrite all existing specs immediately.
- Do not force every small component to have its own OpenSpec.
- Do not duplicate backend rules into web/mobile/edge specs.
- Do not create UI/component-level OpenSpecs such as one OpenSpec for `HeaderComponent` or `FooterComponent`.

## Decision

Use one OpenSpec per autonomous project:

```text
tchalanet/
├── openspec/
│   ├── project.md
│   ├── specs/
│   ├── changes/
│   └── archive/
│
├── tchalanet-server/
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
├── tchalanet-web/
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
├── tchalanet-mobile/
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
└── tchalanet-edge-service/
    └── openspec/
        ├── project.md
        ├── specs/
        ├── changes/
        └── archive/
└── tchalanet-infra/
    └── openspec/
        ├── project.md
        ├── specs/
        ├── changes/
        └── archive/
```

The root `openspec/` is reserved for:

- MVP roadmap
- cross-project contracts
- backend/web/mobile/edge coordination
- client demo readiness
- major integration decisions

Project-specific implementation changes must live inside the relevant project OpenSpec.

## Change naming convention

Use capability-level changes, not component-level changes.

Good examples:

```text
tchalanet-web/openspec/changes/upgrade-angular-nx
tchalanet-web/openspec/changes/resync-pagemodel-runtime
tchalanet-web/openspec/changes/repair-auth-flow
tchalanet-server/openspec/changes/harden-core-sales-sell-print
tchalanet-edge-service/openspec/changes/dev-staging-notifications
tchalanet-mobile/openspec/changes/flutter-seller-mvp
```

Bad examples:

```text
tchalanet-web/openspec/changes/header-component
tchalanet-web/openspec/changes/footer-component
tchalanet-web/openspec/changes/sidebar-component
tchalanet-web/openspec/changes/login-button
```

Components should usually be tasks inside a capability change.

## Archive rule

A change must be archived inside the same OpenSpec where it was created.

Examples:

```text
tchalanet-server/openspec/changes/harden-core-sales-sell-print
→ tchalanet-server/openspec/archive/YYYY-MM-DD-harden-core-sales-sell-print

tchalanet-web/openspec/changes/repair-auth-flow
→ tchalanet-web/openspec/archive/YYYY-MM-DD-repair-auth-flow

openspec/changes/client-demo-readiness
→ openspec/archive/YYYY-MM-DD-client-demo-readiness
```

Agents must not manually archive OpenSpec changes with `rm`, `cp`, or `mv`.

They must use the OpenSpec CLI from the correct project directory:

```bash
openspec archive <change-id> --yes
openspec validate --strict
```

If the CLI is unavailable or fails, the agent must stop and report the issue.

## Agent rule

Before analysis or editing, agents must identify:

- current working directory
- git root
- active branch
- don't create new branch
- last commit
- project-local OpenSpec path
- whether the worktree is stale, detached, or missing OpenSpec

Mandatory commands:

```bash
pwd
git rev-parse --show-toplevel
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 4 -type d -name openspec
```

If the desktop app forces a worktree/sandbox, the agent must treat it as a separate checkout and verify it before analysis.

## Expected outcome

After this change:

- server agents use `tchalanet-server/openspec`
- web agents use `tchalanet-web/openspec`
- mobile agents use `tchalanet-mobile/openspec`
- edge agents use `tchalanet-edge-service/openspec`
- root OpenSpec remains lightweight and cross-project only
- archives are project-local
- stale worktrees are detected before analysis
- Claude/Codex tasks consume less context and avoid cross-project drift

## Risks

- Existing global changes may need to be moved carefully.
- Agents may still use stale worktrees if rules are not enforced.
- Empty `archive/` folders may be ignored by git unless `.gitkeep` is added.
- Multiple OpenSpec roots require clear documentation.

## Migration strategy

1. Create project-local OpenSpec directories.
2. Add `.gitkeep` to empty `archive/`, `changes/`, and `specs/` directories if needed.
3. Add root and project-level `CLAUDE.md` rules.
4. Move only active relevant changes to their project-local OpenSpec.
5. Leave cross-project changes at root.
6. Validate each OpenSpec independently.
