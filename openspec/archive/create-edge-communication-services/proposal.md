# Change: create-edge-communication-services

## Why

`tchalanet-edge-service` must evolve from a basic Fastify bootstrap into a real communication edge service supporting Slack, Email, and SMS — called from Spring Boot via internal HTTP.

Architecture rule:

```
Spring Boot prepares, validates, authorizes, and audits.
Edge-service delivers, formats, routes, and integrates.
```

## What

Added the first real communication modules to `tchalanet-edge-service`:

- `notifications` module with domain types, sender port, orchestration service
- Slack adapter (Incoming Webhooks via `@slack/webhook`)
- Email adapter (Brevo v5 via `@getbrevo/brevo`)
- SMS adapter (Twilio via `twilio`)
- Noop adapters for safe local dev without credentials
- `POST /internal/notifications/send` with JSON Schema validation
- Per-recipient delivery results — partial failures don't block the request
- 11 automated tests via `app.inject()` (no real provider calls)

## Non-goals

Not implemented: WhatsApp sending, ticket attachments, Redis idempotency, rules engine, feature management, provider webhooks, HMAC auth, persistence of delivery logs.

---

## Archive Information

**Archived:** 2026-05-03
**Duration:** 1 day
**Outcome:** Successfully implemented

### Files Created

- `src/modules/notifications/domain/notification-channel.ts`
- `src/modules/notifications/domain/notification-severity.ts`
- `src/modules/notifications/domain/notification-message.ts`
- `src/modules/notifications/ports/notification-sender.port.ts`
- `src/modules/notifications/application/send-notification.service.ts`
- `src/modules/notifications/adapters/slack/slack-notification.sender.ts`
- `src/modules/notifications/adapters/email/brevo-email-notification.sender.ts`
- `src/modules/notifications/adapters/email/noop-email-notification.sender.ts`
- `src/modules/notifications/adapters/sms/twilio-sms-notification.sender.ts`
- `src/modules/notifications/adapters/sms/noop-sms-notification.sender.ts`
- `src/modules/notifications/http/notification.schemas.ts`
- `src/modules/notifications/http/notification.routes.ts`
- `tests/notification.service.test.ts`
- `tests/notification.routes.test.ts`

### Files Modified

- `src/config/env.ts` — Slack/Email/SMS env vars
- `src/app.ts` — senders + service + route wired
- `.env.example` — provider vars documented
- `README.md` — manual test curl commands

### Quality Gates

- `npm run typecheck` → 0 errors
- `npm run build` → dist/ compiled
- `npm test` → 11/11 pass

### Specs Updated

- `openspec/specs/edge-service/spec.md` — notification requirements added
