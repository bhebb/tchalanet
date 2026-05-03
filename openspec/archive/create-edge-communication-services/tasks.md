# Tasks: create-edge-communication-services

## Status: DONE

## 1. Inspect current edge-service

- [x] Inspect `tchalanet-edge-service/package.json`
- [x] Inspect `tchalanet-edge-service/src/app.ts`
- [x] Inspect `tchalanet-edge-service/src/config/env.ts`
- [x] Inspect `tchalanet-edge-service/src/modules`
- [x] Inspect existing tests
- [x] Identify package manager from lockfile

## 2. Add dependencies

- [x] Add Slack dependency: `@slack/webhook`
- [x] Add Brevo dependency: `@getbrevo/brevo`
- [x] Add Twilio dependency: `twilio`
- [x] Do not add Redis, Liquid, rules engine, feature-management SDKs, or WhatsApp-specific dependencies yet
- [x] Update the existing lockfile using the repo package manager

## 3. Extend environment config

- [x] Update `src/config/env.ts` with Slack/Email/SMS vars
- [x] Update `.env.example`
- [x] Do not throw at startup if Slack/email/SMS are disabled
- [x] Do not throw at startup if optional provider credentials are missing
- [x] Provider adapters return clear failure reasons when disabled or not configured

## 4. Create notification domain types

- [x] `src/modules/notifications/domain/notification-channel.ts`
- [x] `src/modules/notifications/domain/notification-severity.ts`
- [x] `src/modules/notifications/domain/notification-message.ts`

## 5. Create notification sender port

- [x] `src/modules/notifications/ports/notification-sender.port.ts`

## 6. Implement orchestration service

- [x] `src/modules/notifications/application/send-notification.service.ts`
- [x] Iterates over recipients
- [x] Finds first sender supporting recipient
- [x] If none: `accepted=false`, reason `NO_SENDER_CONFIGURED`
- [x] If sender succeeds: `accepted=true`
- [x] If sender fails: `accepted=false`, reason from error message
- [x] Overall `accepted=true` if at least one delivery accepted
- [x] Does not throw the whole request when one recipient fails

## 7. Implement Slack sender

- [x] `src/modules/notifications/adapters/slack/slack-notification.sender.ts`
- [x] Supports channel `SLACK`
- [x] Maps channelKey to env webhook URL
- [x] `SLACK_DISABLED` when disabled
- [x] `SLACK_WEBHOOK_NOT_CONFIGURED:<key>` when webhook missing
- [x] Uses `@slack/webhook`
- [x] Message includes severity, title, message, eventId, tenantCode
- [x] Does not log webhook URLs

## 8. Implement email sender

- [x] `src/modules/notifications/adapters/email/brevo-email-notification.sender.ts`
- [x] `src/modules/notifications/adapters/email/noop-email-notification.sender.ts`
- [x] Supports channel `EMAIL`
- [x] `EMAIL_DISABLED` / `EMAIL_PROVIDER_NOT_CONFIGURED` when missing
- [x] Subject: `[SEVERITY] title`
- [x] No attachments

## 9. Implement SMS sender

- [x] `src/modules/notifications/adapters/sms/twilio-sms-notification.sender.ts`
- [x] `src/modules/notifications/adapters/sms/noop-sms-notification.sender.ts`
- [x] Supports channel `SMS`
- [x] `SMS_DISABLED` / `SMS_PROVIDER_NOT_CONFIGURED` when missing
- [x] Body max ~320 chars

## 10. Add JSON Schema validation

- [x] `src/modules/notifications/http/notification.schemas.ts`
- [x] `eventId`, `severity`, `title`, `message`, `recipients` required
- [x] `severity` enum `INFO|WARN|ERROR|CRITICAL`
- [x] `channel` enum `SLACK|EMAIL|SMS|WHATSAPP`
- [x] `to`, `channelKey` optional
- [x] `context` optional object
- [x] `additionalProperties=false`

## 11. Add notification route

- [x] `src/modules/notifications/http/notification.routes.ts`
- [x] `POST /internal/notifications/send`
- [x] Uses JSON Schema
- [x] Returns HTTP 202
- [x] TODO comment for HMAC

## 12. Register module in app

- [x] `src/app.ts` wires senders, service, route
- [x] Existing ping/health routes still working

## 13. Add tests

- [x] Service: accepted when sender succeeds
- [x] Service: `NO_SENDER_CONFIGURED` when no sender matches
- [x] Service: partial accepted when one recipient fails
- [x] Route: 202 with valid body
- [x] Route: 400 on invalid body (missing fields, bad enum, empty recipients)

## 14. Manual test documentation

- [x] README: Slack configuration
- [x] README: Brevo email configuration
- [x] README: Twilio SMS configuration
- [x] README: curl for Slack
- [x] README: curl for email
- [x] README: curl for SMS
- [x] README: WhatsApp is future
- [x] README: secrets must not be committed

## 15. Validate

- [x] `npm run typecheck` → 0 errors
- [x] `npm run build` → dist/ compiled
- [x] `npm test` → 11/11 pass

## 16. Final report

- [x] Reported in conversation
