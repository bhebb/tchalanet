# Change: create-edge-communication-services

## Why

`tchalanet-edge-service` must evolve from a basic Fastify bootstrap into a real communication edge service.

The service will initially support:

- Slack notifications
- Email sending
- SMS sending

It must be designed so that WhatsApp can be added later without changing the public/internal contract too much.

The edge-service must remain peripheral. It must not own Tchalanet business truth.

Spring Boot remains responsible for:

- ticket truth
- draw/drawresult/settlement truth
- permissions
- tenant context / RLS
- audit métier
- transactional state
- critical feature decisions

Edge-service is responsible for:

- formatting messages
- routing to Slack/email/SMS providers
- calling external providers
- returning delivery result
- logging technical delivery status
- providing a small test endpoint or test mode for local verification

Core rule:

```text
Spring Boot prepares, validates, authorizes, and audits.
Edge-service delivers, formats, routes, and integrates.
```

## What

This change adds the first real communication modules to `tchalanet-edge-service`.

It must:

- create a `notifications` module
- create provider adapters for Slack, email, and SMS
- add a shared notification contract
- add provider configuration via env
- add JSON Schema validation for notification requests
- add an internal route to send a notification
- add test routes or safe local test capability to verify Slack and email sending
- keep WhatsApp as a future channel type, not implemented yet
- add unit tests with mocked providers
- document how to manually test Slack and email

## Non-goals

This change must not implement:

- WhatsApp sending
- ticket delivery with attachments
- PDF download from signed URL
- Redis anti-spam/idempotency
- rules engine
- feature management proxy
- webhooks from providers
- persistence of delivery logs
- Spring Boot adapter code

Those are follow-up changes.

## Scope

Repository path:

```text
tchalanet-edge-service/
```

Allowed files/directories:

```text
tchalanet-edge-service/package.json
tchalanet-edge-service/package-lock.json OR pnpm-lock.yaml
tchalanet-edge-service/.env.example
tchalanet-edge-service/README.md
tchalanet-edge-service/src/**
tchalanet-edge-service/tests/**
```

Do not modify Spring Boot, Angular, mobile, infra, or docs in this change.

## Existing assumptions

This change assumes the bootstrap change has already created:

```text
src/main.ts
src/app.ts
src/config/env.ts
src/modules/ping
src/modules/health
```

If those files do not exist, create the smallest missing pieces needed for this change to compile.

## Target providers

### Slack

MVP provider:

```text
Slack Incoming Webhooks
```

Environment variables:

```env
SLACK_ENABLED=true
SLACK_WEBHOOK_TCHALANET=
SLACK_WEBHOOK_BATCH_DRAWS=
SLACK_WEBHOOK_DELIVERY=
SLACK_WEBHOOK_OPS_ALERTS=
SLACK_WEBHOOK_SECURITY_AUDIT=
```

Channel keys:

```text
tchalanet
batch-draws
delivery
ops-alerts
security-audit
```

### Email

MVP provider:

```text
Brevo
```

Environment variables:

```env
EMAIL_PROVIDER=brevo
EMAIL_ENABLED=true
BREVO_API_KEY=
EMAIL_FROM_NAME=Tchalanet
EMAIL_FROM_ADDRESS=no-reply@example.com
```

### SMS

MVP provider:

```text
Twilio by default, Brevo later if desired
```

Environment variables:

```env
SMS_PROVIDER=twilio
SMS_ENABLED=false
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM=
```

SMS can remain stubbed if credentials are not present, but the adapter contract must exist.

## Dependencies

Add only the dependencies needed for this change.

Recommended runtime dependencies:

```text
@slack/webhook
@getbrevo/brevo
twilio
```

Keep existing dependencies:

```text
fastify
dotenv
```

Do not add Redis/Liquid/rules dependencies in this change.

## New module structure

Create:

```text
src/modules/notifications/
  domain/
    notification-channel.ts
    notification-severity.ts
    notification-message.ts
  application/
    send-notification.service.ts
  ports/
    notification-sender.port.ts
  adapters/
    slack/
      slack-notification.sender.ts
    email/
      brevo-email-notification.sender.ts
      noop-email-notification.sender.ts
    sms/
      twilio-sms-notification.sender.ts
      noop-sms-notification.sender.ts
  http/
    notification.routes.ts
    notification.schemas.ts
  index.ts
```

Optional shared infra:

```text
src/infra/slack/
src/infra/email/
src/infra/sms/
```

Only create `infra/` if useful. Prefer keeping adapter implementation under the module for MVP.

## Internal route

Add:

```text
POST /internal/notifications/send
```

MVP auth:

- If HMAC plugin does not exist yet, leave a TODO and do not block this change.
- Do not expose this route as public.

Request:

```json
{
  "eventId": "evt_local_001",
  "tenantCode": "demo",
  "severity": "INFO",
  "title": "Local test",
  "message": "Hello from Tchalanet Edge Service",
  "recipients": [
    {
      "channel": "SLACK",
      "channelKey": "batch-draws"
    },
    {
      "channel": "EMAIL",
      "to": "admin@example.com"
    }
  ],
  "context": {
    "requestId": "req_local_001"
  }
}
```

Response:

```json
{
  "accepted": true,
  "eventId": "evt_local_001",
  "deliveries": [
    {
      "channel": "SLACK",
      "accepted": true
    },
    {
      "channel": "EMAIL",
      "accepted": true
    }
  ]
}
```

If a provider is disabled or missing config, return a delivery item with `accepted=false` and a reason.

Example:

```json
{
  "channel": "EMAIL",
  "accepted": false,
  "reason": "EMAIL_PROVIDER_NOT_CONFIGURED"
}
```

## Local test capability

At the end of this change, a developer must be able to test Slack or email in one of these ways:

Preferred:

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId":"local-slack-test-001",
    "severity":"INFO",
    "title":"Tchalanet Slack test",
    "message":"Edge-service can send Slack messages.",
    "recipients":[{"channel":"SLACK","channelKey":"batch-draws"}]
  }'
```

Email test:

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId":"local-email-test-001",
    "severity":"INFO",
    "title":"Tchalanet Email test",
    "message":"Edge-service can send email messages.",
    "recipients":[{"channel":"EMAIL","to":"you@example.com"}]
  }'
```

If provider credentials are not configured, tests should still pass with noop/mocked providers.

## Acceptance criteria

- `npm run typecheck` or equivalent passes.
- `npm run build` passes.
- `npm test` passes.
- Server starts with `npm run dev` or `pnpm dev`.
- `GET /ping` still works.
- `POST /internal/notifications/send` validates payloads with JSON Schema.
- Slack adapter can send a real Slack message when `SLACK_ENABLED=true` and webhook is configured.
- Email adapter can send a real email when `EMAIL_ENABLED=true`, `EMAIL_PROVIDER=brevo`, and Brevo env vars are configured.
- SMS adapter contract exists, and Twilio adapter is implemented or safely disabled/noop if credentials are missing.
- No secrets are committed.

## Claude constraints

Important: conserve tokens.

Before editing:

1. Inspect only `tchalanet-edge-service/package.json`, `src/app.ts`, `src/config/env.ts`, `src/modules`, and `tests`.
2. Do not scan the entire monorepo.
3. Do not touch Spring Boot, Angular, mobile, infra, or docs.
4. Do not implement WhatsApp or attachments in this change.
5. Do not introduce Redis/rules/templates/feature-management in this change.
6. Keep the patch minimal.

Output format after implementation:

```text
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
```
