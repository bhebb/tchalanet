# /nx-task

Use for Nx workspace, Angular build/test/lint, shared libs, aliases, project config.

Default agent:

- nx-workspace-agent for workspace/config tasks
- web-slice-agent for app UI tasks

Rules:

- Do one task only.
- Do not scan all apps/libs.
- Inspect only the project affected.
- Do not upgrade dependencies unless explicitly requested.
- Respect versions in `VERSIONS.md`.
- Ask before changing package versions or lockfile.
- Run narrow validation only.

Required input:

- Project/app affected
- Task
- Files to inspect first
- Files/folders allowed to edit
- Validation command

Output:

1. Project affected
2. Files inspected
3. Files changed
4. Command run
5. Risks
6. Handoff

Template:

Project:

- apps/tchalanet-web

Task:

- <one precise task>

Can edit:

- ...

Must inspect first:

- ...

Do not touch:

- tchalanet-server
- tchalanet-infra
- tchalanet-edge-service

Validation:

- pnpm nx build tchalanet-web
