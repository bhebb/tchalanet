Apply OpenSpec change `create-edge-communication-services`.

Scope only: `tchalanet-edge-service`.

Goal:

- add notifications module
- support Slack, email, and SMS providers for now
- keep WhatsApp as future channel type only
- add POST `/internal/notifications/send`
- allow manual Slack test and email test by curl
- add JSON Schema validation
- add mocked/noop-safe tests

Do not implement:

- WhatsApp sending
- ticket attachments
- Redis anti-spam/idempotency
- templates
- rules engine
- feature management proxy
- provider webhooks
- Spring Boot code

Provider choices:

- Slack: Incoming Webhooks with `@slack/webhook`
- Email: Brevo with `@getbrevo/brevo`
- SMS: Twilio with `twilio`

Important token rule:

- inspect only `tchalanet-edge-service/package.json`, `src/app.ts`, `src/config/env.ts`, `src/modules`, and `tests`
- do not scan the full monorepo
- do not touch unrelated folders

Follow:

- `openspec/changes/create-edge-communication-services/proposal.md`
- `openspec/changes/create-edge-communication-services/tasks.md`
- `openspec/changes/create-edge-communication-services/design.md`

Return:
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
