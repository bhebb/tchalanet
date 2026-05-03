# Claude prompt — bootstrap-edge-service-fastify

Apply OpenSpec change `bootstrap-edge-service-fastify`.

Scope only: `tchalanet-edge-service`.

- Don't create new branch - the folder is isolated -
- you can remove all the files/modify the folder.

Goal:

- clean/align the edge-service folder
- add Fastify + TypeScript bootstrap
- add minimal `/ping`, `/health`, `/ready`
- add minimal tests
- ensure server starts

Do not implement Slack, Brevo, Twilio, Redis, rules, feature flags, templates, delivery attachments, or HMAC in this change.

Do not scan or edit unrelated monorepo folders.

Follow:

- `openspec/changes/bootstrap-edge-service-fastify/proposal.md`
- `openspec/changes/bootstrap-edge-service-fastify/tasks.md`
- `openspec/changes/bootstrap-edge-service-fastify/design.md`

Return:

```text
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
```

## Ultra-short variant

Work only in `tchalanet-edge-service`.

Bootstrap Fastify + TypeScript.
Create:

- src/main.ts
- src/app.ts
- src/config/env.ts
- src/modules/ping/ping.routes.ts
- src/modules/health/health.routes.ts
- tests/ping.test.ts

Add dependencies only:

- fastify
- dotenv
- typescript
- tsx
- vitest
- @types/node

Add scripts:

- dev
- start
- build
- test
- typecheck

Implement:

- `GET /ping` -> `{ ok: true, service: "tchalanet-edge-service" }`
- `GET /health` -> `{ status: "UP", service: "tchalanet-edge-service" }`
- `GET /ready` -> `{ status: "READY", service: "tchalanet-edge-service" }`

Run typecheck/build/test.
Do not touch other folders.
Do not implement providers.
