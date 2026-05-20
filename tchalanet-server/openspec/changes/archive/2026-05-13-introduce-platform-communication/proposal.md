# Change: introduce-platform-communication

## Why

Tchalanet needs a dedicated external messaging capability for email, SMS, Slack and future push delivery. Core domains, batch jobs and common utilities must not depend on provider gateways.

## What changes

- Add `platform.communication` capability.
- Add outbound message persistence and delivery attempts.
- Add message templates and tenant communication settings.
- Add `CommunicationApi.enqueue` as the default path.
- Keep `sendNow` only for controlled ops tests/diagnostics.
- Move Slack/email/SMS provider adapters under `platform.communication.internal.adapter`.

## Impact

- New platform package.
- New Flyway migration.
- Old direct Slack gateway calls must be replaced.
- New ArchUnit rules prevent provider imports outside platform.communication.internal.
