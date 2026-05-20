# Tasks: create-server-notification-usecases

## 1. Domain/application model

- [ ] Add/refine `NotificationType` enum.
- [ ] Add/refine `NotificationSeverity` enum if not already present.
- [ ] Add/refine `NotificationChannel` enum with at least:
  - [ ] `SLACK`
  - [ ] `EMAIL`
  - [ ] `SMS`
  - [ ] `WEB` optional/future
  - [ ] `WHATSAPP` optional/future
- [ ] Add `NotificationRecipient` model supporting:
  - [ ] `channel`
  - [ ] `to`
  - [ ] `channelKey`
  - [ ] `tenantId` optional
  - [ ] `userId` optional
- [ ] Ensure typed IDs are not serialized directly to edge DTOs.

## 2. Command/use case

- [ ] Add `SendNotificationCommand`.
- [ ] Add `SendNotificationResult`.
- [ ] Add `SendNotificationCommandHandler`.
- [ ] Validate command is not null.
- [ ] Validate recipients are not empty.
- [ ] Validate title/message based on type/severity.
- [ ] Generate/require idempotency key.
- [ ] Map command to `SendNotificationPayload`.
- [ ] Delegate to `NotificationGatewayPort`.

## 3. Policy

- [ ] Add `NotificationPolicy`.
- [ ] Enforce channel-specific recipient rules:
  - [ ] Slack requires `channelKey`.
  - [ ] Email requires `to` email.
  - [ ] SMS requires `to` phone.
- [ ] Prevent dangerous free-form client sends.
- [ ] Add TODO/future hook for tenant notification preferences.
- [ ] Add TODO/future hook for opt-in/consent on client notifications.

## 4. Ops test endpoint

- [ ] Add `OpsNotificationController` or extend existing ops controller.
- [ ] Add `POST /api/v1/ops/notifications/test`.
- [ ] Protect with `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- [ ] Request DTO supports:
  - [ ] channel
  - [ ] to
  - [ ] channelKey
  - [ ] severity
  - [ ] title
  - [ ] message
- [ ] Controller maps request to `SendNotificationCommand`.
- [ ] Return `ApiResponse` with accepted/failure result.

## 5. Optional admin endpoint skeleton

- [ ] Add `AdminNotificationController` only if needed now.
- [ ] Restrict to admin/superadmin permissions.
- [ ] Do not allow arbitrary client recipient abuse.
- [ ] Keep tenant-scoped if admin endpoint is added.

## 6. Tests

- [ ] Unit test `NotificationPolicy` Slack requires channelKey.
- [ ] Unit test `NotificationPolicy` Email requires to.
- [ ] Unit test `NotificationPolicy` SMS requires to.
- [ ] Unit test handler delegates to gateway when policy passes.
- [ ] Unit test handler rejects invalid recipients.
- [ ] Web test ops endpoint security if test infra exists.

## 7. Documentation

- [ ] Document ops test curl for Slack.
- [ ] Document ops test curl for Email.
- [ ] Document ops test curl for SMS.
- [ ] Document that web/mobile must use business endpoints, not generic send.
