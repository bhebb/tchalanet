# Tasks: create-edge-communication-services

## 1. Inspect current edge-service

- [ ] Inspect `tchalanet-edge-service/package.json`
- [ ] Inspect `tchalanet-edge-service/src/app.ts`
- [ ] Inspect `tchalanet-edge-service/src/config/env.ts`
- [ ] Inspect `tchalanet-edge-service/src/modules`
- [ ] Inspect existing tests
- [ ] Identify package manager from lockfile

## 2. Add dependencies

- [ ] Add Slack dependency: `@slack/webhook`
- [ ] Add Brevo dependency: `@getbrevo/brevo`
- [ ] Add Twilio dependency: `twilio`
- [ ] Do not add Redis, Liquid, rules engine, feature-management SDKs, or WhatsApp-specific dependencies yet
- [ ] Update the existing lockfile using the repo package manager

## 3. Extend environment config

Update `src/config/env.ts` and `.env.example` with:

```env
SLACK_ENABLED=true
SLACK_WEBHOOK_TCHALANET=
SLACK_WEBHOOK_BATCH_DRAWS=
SLACK_WEBHOOK_DELIVERY=
SLACK_WEBHOOK_OPS_ALERTS=
SLACK_WEBHOOK_SECURITY_AUDIT=

EMAIL_ENABLED=true
EMAIL_PROVIDER=brevo
BREVO_API_KEY=
EMAIL_FROM_NAME=Tchalanet
EMAIL_FROM_ADDRESS=no-reply@example.com

SMS_ENABLED=false
SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM=
```

Rules:

- [ ] Do not throw at startup if Slack/email/SMS are disabled
- [ ] Do not throw at startup if optional provider credentials are missing
- [ ] Provider adapters should return clear failure reasons when disabled or not configured

## 4. Create notification domain types

Create:

```text
src/modules/notifications/domain/notification-channel.ts
src/modules/notifications/domain/notification-severity.ts
src/modules/notifications/domain/notification-message.ts
```

Required types:

```ts
export type NotificationChannel = 'SLACK' | 'EMAIL' | 'SMS' | 'WHATSAPP';
export type NotificationSeverity = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';
```

Recipient shape:

```ts
export interface NotificationRecipient {
  channel: NotificationChannel;
  to?: string;
  channelKey?: string;
}
```

Request/response:

```ts
export interface SendNotificationRequest { ... }
export interface SendNotificationResponse { ... }
```

## 5. Create notification sender port

Create:

```text
src/modules/notifications/ports/notification-sender.port.ts
```

Contract:

```ts
export interface NotificationSender {
  supports(recipient: NotificationRecipient): boolean;
  send(notification: SendNotificationRequest, recipient: NotificationRecipient): Promise<void>;
}
```

## 6. Implement orchestration service

Create:

```text
src/modules/notifications/application/send-notification.service.ts
```

Behavior:

- [ ] Iterate over recipients
- [ ] Find first sender supporting recipient
- [ ] If none, delivery result `accepted=false`, reason `NO_SENDER_CONFIGURED`
- [ ] If sender succeeds, delivery result `accepted=true`
- [ ] If sender fails, delivery result `accepted=false`, reason from error message
- [ ] Overall response `accepted=true` if at least one delivery accepted
- [ ] Do not throw the whole request when one recipient fails

## 7. Implement Slack sender

Create:

```text
src/modules/notifications/adapters/slack/slack-notification.sender.ts
```

Behavior:

- [ ] Supports recipient channel `SLACK`
- [ ] Uses `channelKey`, default `batch-draws` or `ops-alerts`
- [ ] Maps channel key to env webhook URL
- [ ] If `SLACK_ENABLED=false`, throw/return reason `SLACK_DISABLED`
- [ ] If webhook missing, throw/return reason `SLACK_WEBHOOK_NOT_CONFIGURED:<channelKey>`
- [ ] Uses `@slack/webhook` when configured
- [ ] Message includes severity, title, message, eventId, tenantCode if present
- [ ] Do not log webhook URLs

Supported channel keys:

```text
tchalanet
batch-draws
delivery
ops-alerts
security-audit
```

## 8. Implement email sender

Create:

```text
src/modules/notifications/adapters/email/brevo-email-notification.sender.ts
src/modules/notifications/adapters/email/noop-email-notification.sender.ts
```

Behavior:

- [ ] Supports recipient channel `EMAIL`
- [ ] Requires `recipient.to`
- [ ] If email disabled, return/throw reason `EMAIL_DISABLED`
- [ ] If Brevo config missing, return/throw reason `EMAIL_PROVIDER_NOT_CONFIGURED`
- [ ] Sends simple text/html email through Brevo when configured
- [ ] Subject format: `[SEVERITY] title`
- [ ] Body includes message, eventId, tenantCode if present
- [ ] Do not implement attachments in this change

## 9. Implement SMS sender

Create:

```text
src/modules/notifications/adapters/sms/twilio-sms-notification.sender.ts
src/modules/notifications/adapters/sms/noop-sms-notification.sender.ts
```

Behavior:

- [ ] Supports recipient channel `SMS`
- [ ] Requires `recipient.to`
- [ ] If SMS disabled, return/throw reason `SMS_DISABLED`
- [ ] If Twilio config missing, return/throw reason `SMS_PROVIDER_NOT_CONFIGURED`
- [ ] Sends simple SMS through Twilio when configured
- [ ] Body should be short and safe, max around 320 chars

## 10. Add JSON Schema validation

Create:

```text
src/modules/notifications/http/notification.schemas.ts
```

Validate:

- [ ] `eventId` required string
- [ ] `severity` required enum `INFO|WARN|ERROR|CRITICAL`
- [ ] `title` required string
- [ ] `message` required string
- [ ] `recipients` required non-empty array
- [ ] recipient `channel` required enum `SLACK|EMAIL|SMS|WHATSAPP`
- [ ] recipient `to` optional string
- [ ] recipient `channelKey` optional string
- [ ] `context` optional object
- [ ] `additionalProperties=false` where practical

## 11. Add notification route

Create:

```text
src/modules/notifications/http/notification.routes.ts
```

Register:

```text
POST /internal/notifications/send
```

Behavior:

- [ ] Uses JSON Schema
- [ ] Calls `SendNotificationService`
- [ ] Returns HTTP 202 with response body
- [ ] Does not require HMAC in this change if plugin is not implemented yet
- [ ] Add TODO comment for HMAC if absent

## 12. Register module in app

Update `src/app.ts`:

- [ ] Wire configured senders
- [ ] Instantiate `SendNotificationService`
- [ ] Register notification routes
- [ ] Keep existing ping/health routes working

## 13. Add tests

Add tests under `tests/`:

- [ ] notification service returns accepted delivery when sender succeeds
- [ ] notification service returns failure delivery when no sender configured
- [ ] POST `/internal/notifications/send` rejects invalid body
- [ ] POST `/internal/notifications/send` accepts valid body with mocked/noop sender

Avoid real provider calls in automated tests.

## 14. Manual test documentation

Update README with:

- [ ] how to configure Slack webhook
- [ ] how to configure Brevo email
- [ ] how to configure Twilio SMS
- [ ] curl for Slack test
- [ ] curl for email test
- [ ] note that WhatsApp is future
- [ ] note that secrets must not be committed

## 15. Validate

Run:

```bash
npm run typecheck
npm run build
npm test
```

Or pnpm equivalents if repo uses pnpm.

Then start:

```bash
npm run dev
```

Test ping:

```bash
curl http://localhost:3000/ping
```

Test Slack:

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

Test email:

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId":"local-email-test-001",
    "severity":"INFO",
    "title":"Tchalanet Email test",
    "message":"Edge-service can send email messages.",
    "recipients":[{"channel":"EMAIL","to":"your-email@example.com"}]
  }'
```

## 16. Final report

Claude must report:

```text
Plan
Changes
Files touched
Commands run
How to test
Risks / follow-up
```
