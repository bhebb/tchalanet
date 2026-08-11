# Tasks

## 1 — Contract

- [ ] Confirm tenant channel activation and fee policy for SMS, WhatsApp, and email.
- [ ] Define recipient capture timing and whether a channel can alter sale pricing.
- [ ] Define text/link/attachment behavior and localized templates per channel.
- [ ] Define delivery status, retry, idempotency, and audit payloads.

## 2 — Mobile

- [ ] Add post-ticket delivery actions without coupling them to printer adapters.
- [ ] Show queued, sent, failed, and retryable states independently from sale and print.
- [ ] Prevent duplicate sends on double tap or app retry.

## 3 — Backend and tests

- [ ] Verify or add channel capability/policy response for the active tenant and terminal.
- [ ] Verify SMS, WhatsApp, and email provider behavior with delivery event audits.
- [ ] Add tests for each channel, disabled channel, invalid recipient, provider timeout, and retry idempotency.
