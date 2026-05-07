# Tasks: P1 — Edge-service messages route

## 1. Add canonical internal messages route

- [x] Add `POST /internal/messages/send`.
- [x] Remove `POST /internal/notifications/send`.
- [x] Ensure only `/internal/messages/send` is registered for outbound messages.
- [x] Update route naming from notification to message where safe.

## 2. Preserve HMAC verification

- [x] Require HMAC middleware for `/internal/messages/send`.
- [x] Verify `X-Tch-Timestamp`.
- [x] Verify `X-Tch-Signature`.
- [x] Verify signature against the raw body.
- [x] Ensure body parsing happens after HMAC verification or uses preserved raw body.
- [x] Preserve replay/expiration behavior if currently implemented.
- [x] Add tests for both routes using the same signed payload.

## 3. Rename internal service concepts

- [x] Rename route file from notification naming to message naming where safe.
- [x] Rename service from `NotificationService` to `MessageService` or `CommunicationService`.
- [x] Rename request model to `SendMessageRequest`.
- [x] Rename recipient model to `MessageRecipient`.
- [x] Keep TypeScript compatibility aliases only if needed internally during transition.
- [x] Ensure provider adapters do not need to be renamed unless trivial.

## 4. Keep payload compatibility

- [x] Preserve existing request shape:
  - `eventId`
  - `severity`
  - `title`
  - `message`
  - `recipients`
  - `context`
- [x] Preserve Slack behavior using `channelKey`.
- [x] Preserve email/SMS/WhatsApp behavior using `to`.
- [x] Preserve response shape unless a separate change is approved.

## 5. Documentation/config

- [x] Document endpoint:
  - `POST /internal/messages/send`
- [x] Document that `/internal/notifications/send` is removed.
- [x] State that web/mobile must never call edge-service.
- [x] Add/update a small HMAC curl doc with:
  - [x] `X-Request-Id`
  - [x] `Idempotency-Key`
  - [x] `X-Tch-Timestamp`
  - [x] `X-Tch-Signature`
  - [x] signature formula `timestamp + "." + raw_json_body`
  - [x] `/internal/messages/send` example
- [x] Update environment variable names only if safe; otherwise defer to a later cleanup.

## 6. Tests

- [x] `POST /internal/messages/send` accepts a valid HMAC-signed request.
- [x] `POST /internal/messages/send` rejects invalid signature.
- [x] `POST /internal/messages/send` rejects missing timestamp/signature.
- [x] `POST /internal/notifications/send` is not registered.
- [x] Slack recipient with `channelKey` still works.
- [x] Email/SMS/WhatsApp recipient with `to` still works.

## 7. Cleanup

- [x] Remove legacy route references from README/manual curl examples.
- [x] Ensure server follow-up targets only `/internal/messages/send`.
