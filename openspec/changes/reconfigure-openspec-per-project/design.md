# Design: Project-local OpenSpec layout

## Overview

Tchalanet uses multiple independent implementation surfaces. Each surface must have its own OpenSpec context so that agents can work locally without loading unrelated project context.

The root OpenSpec remains only for cross-project coordination.

## Directory layout

```text
tchalanet/
├── CLAUDE.md
├── openspec/
│   ├── project.md
│   ├── specs/
│   ├── changes/
│   └── archive/
│
├── tchalanet-server/
│   ├── CLAUDE.md
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
├── tchalanet-web/
│   ├── CLAUDE.md
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
├── tchalanet-mobile/
│   ├── CLAUDE.md
│   └── openspec/
│       ├── project.md
│       ├── specs/
│       ├── changes/
│       └── archive/
│
└── tchalanet-edge-service/
    ├── CLAUDE.md
    └── openspec/
        ├── project.md
        ├── specs/
        ├── changes/
        └── archive/
```

## Root OpenSpec responsibility

The root OpenSpec may contain:

- cross-project roadmap
- client demo readiness plan
- shared API contract decisions
- high-level milestone coordination
- dependency map between server/web/mobile/edge

The root OpenSpec must not contain detailed backend/web/mobile/edge implementation specs unless the change is explicitly cross-project.

## Project OpenSpec responsibility

### `tchalanet-server/openspec`

Owns:

- Java/Spring Boot backend changes
- database migrations
- RLS
- command/query handlers
- domain refactors
- events, audit, batch, cache
- public/backend APIs
- Keycloak/resource-server integration

### `tchalanet-web/openspec`

Owns:

- Angular/Nx upgrades
- PageModel runtime
- public home rendering
- web auth flow
- private shell/sidebar
- Angular Material/theming/i18n
- web API client contracts

### `tchalanet-mobile/openspec`

Owns:

- Flutter seller/POS MVP
- mobile login
- tenant/outlet/terminal confirmation
- ticket sale flow
- ticket summary/history
- offline foundation
- mobile API integration

### `tchalanet-edge-service/openspec`

Owns:

- Fastify/TypeScript edge service
- notification delivery
- Slack/email/SMS/WhatsApp adapters
- HMAC internal API security
- templates/routing/rules
- webhook handling
- anti-spam/cooldown

## Claude root rules

Root `CLAUDE.md` must instruct agents:

````md
# Tchalanet Workspace Rules

This workspace contains multiple autonomous projects.

Use the nearest project-local OpenSpec:

- `tchalanet-server/openspec` for backend tasks.
- `tchalanet-web/openspec` for web tasks.
- `tchalanet-mobile/openspec` for mobile tasks.
- `tchalanet-edge-service/openspec` for edge tasks.
- root `openspec` only for cross-project coordination.

Do not load all project contexts by default.
Do not modify multiple projects unless explicitly requested.
Do not create or use worktrees unless explicitly requested.

If the desktop app forces a worktree/sandbox, verify it before analysis:

```bash
pwd
git rev-parse --show-toplevel
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 4 -type d -name openspec
```
````

If the worktree is stale, detached, missing OpenSpec, or on the wrong branch, stop and report.

Never archive OpenSpec changes manually with `rm`, `cp`, or `mv`.
Use the project-local OpenSpec CLI.

````

## Project CLAUDE.md template

Each project should have a local `CLAUDE.md`.

Example:

```md
# Tchalanet Web — Claude Rules

Use this file only when working inside `tchalanet-web`.

Use the project-local OpenSpec:

```text
tchalanet-web/openspec/
````

Do not inspect or modify server, mobile, or edge files unless explicitly requested.

Before editing:

```bash
pwd
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 3 -type d -name openspec
```

If a task appears to require another project, stop and explain the cross-project dependency instead of editing that project.

````

## OpenSpec archive handling

Required behavior:

```bash
cd <project-root>
openspec validate --strict
openspec archive <change-id> --yes
openspec validate --strict
git status --short
````

Forbidden behavior:

```bash
rm -rf openspec/changes/<change-id>
cp -r openspec/changes/<change-id> openspec/archive/...
mv openspec/changes/<change-id> openspec/archive/...
```

## Git tracking

The following directories must be committed:

```text
openspec/
tchalanet-server/openspec/
tchalanet-web/openspec/
tchalanet-mobile/openspec/
tchalanet-edge-service/openspec/
```

Empty directories must contain `.gitkeep`.

## Validation

Each project OpenSpec should be validated independently.

Examples:

```bash
cd tchalanet-server
openspec validate --strict

cd ../tchalanet-web
openspec validate --strict

cd ../tchalanet-mobile
openspec validate --strict

cd ../tchalanet-edge-service
openspec validate --strict
```

The root OpenSpec should also validate independently:

```bash
cd <workspace-root>
openspec validate --strict
```

## Migration of existing changes

Existing changes should be moved according to ownership:

```text
server-* or backend domain changes
→ tchalanet-server/openspec/changes/

web-* changes
→ tchalanet-web/openspec/changes/

mobile-* changes
→ tchalanet-mobile/openspec/changes/

edge-* changes
→ tchalanet-edge-service/openspec/changes/

cross-* or roadmap/demo readiness
→ root openspec/changes/
```

Do not move archived changes unless necessary. If moved, preserve history in git and document the move.
