# Tasks: integrate-edge-notification-gateway

## 1. Rename existing Node integration

- [ ] Rename `NodeNotificationGatewayAdapter` to `EdgeNotificationGatewayAdapter`.
- [ ] Rename `NodeNotificationConfigProperties` to `EdgeNotificationProperties`.
- [ ] Rename `NodeNotificationConfig` to `EdgeNotificationConfig`.
- [ ] Rename properties prefix from `tch.notification.node` to `tch.notification.edge`.
- [ ] Keep backward compatibility only if required by current config; otherwise remove old naming.

## 2. Edge properties

- [ ] Add/confirm `EdgeNotificationProperties` with:
  - [ ] `enabled`
  - [ ] `baseUrl`
  - [ ] `notificationsPath`
  - [ ] `hmacSecret`
  - [ ] `connectTimeout`
  - [ ] `readTimeout`
- [ ] Validate required properties when `enabled=true`.
- [ ] Use Spring Boot configuration properties binding.

## 3. Edge HTTP DTO

- [ ] Create `EdgeNotificationRequest` DTO matching edge-service:
  - [ ] `eventId`
  - [ ] `severity`
  - [ ] `title`
  - [ ] `message`
  - [ ] `recipients[]`
  - [ ] `context`
- [ ] Create `EdgeNotificationRecipient` DTO:
  - [ ] `channel`
  - [ ] `to`
  - [ ] `channelKey`
- [ ] Do not serialize `TenantId`/`UserId` typed objects directly to edge.
- [ ] Convert tenant/user IDs to string values if included in context.

## 4. HMAC signing

- [ ] Create `EdgeHmacSigner`.
- [ ] Serialize request body to raw JSON before signing.
- [ ] Sign `timestamp + "." + rawJsonBody`.
- [ ] Send headers:
  - [ ] `X-Request-Id`
  - [ ] `Idempotency-Key`
  - [ ] `X-Tch-Timestamp`
  - [ ] `X-Tch-Signature`
  - [ ] `Content-Type: application/json`
- [ ] Use injected `Clock` for timestamp.
- [ ] Avoid direct `Instant.now()`.

## 5. Edge adapter

- [ ] Implement/refine `EdgeNotificationGatewayAdapter implements NotificationGatewayPort`.
- [ ] If edge notification integration is disabled, log debug/info and return without HTTP call.
- [ ] Map `SendNotificationPayload` to `EdgeNotificationRequest`.
- [ ] Ensure Slack batch notifications produce recipient `{ channel: "SLACK", channelKey: "batch-draws" }`.
- [ ] Ensure email notifications produce recipient `{ channel: "EMAIL", to: "..." }`.
- [ ] Ensure SMS notifications produce recipient `{ channel: "SMS", to: "..." }`.
- [ ] Propagate requestId/idempotencyKey when available.
- [ ] Handle HTTP failures with clear logs and a domain-safe exception/result.

## 6. Batch technical notification cleanup

- [ ] Keep `BatchNotification` record unchanged unless compilation requires small alignment.
- [ ] Keep `BatchNotificationStatus` enum unchanged unless compilation requires small alignment.
- [ ] Add `BatchNotificationPolicy` under `common.batch.notification`.
- [ ] Move `shouldNotify`, cooldown, cache lookup and fingerprint logic from `BatchEventNotificationService` to `BatchNotificationPolicy`.
- [ ] Keep `BatchEventNotificationService` as thin orchestrator.
- [ ] Ensure `STARTED` and `SUCCEEDED` do not send notifications.
- [ ] Ensure `FAILED` sends a notification, subject to cooldown.
- [ ] Ensure `SKIPPED` sends only when `code == "gate_disabled"`, subject to cooldown.
- [ ] Keep `BatchNotificationCacheSpecProvider` with cache name `infra.notification.batch_dedup`.
- [ ] Ensure cache TTL uses short L1 and longer L2.
- [ ] Resolve tenantId from `TchContext` when available.
- [ ] Resolve requestId from `TchContext` when available.
- [ ] Generate a fallback requestId only if no context requestId exists.
- [ ] Do not throw if context is absent; global jobs may legitimately have no tenant context.
- [ ] Ensure `BatchScheduledJobAspect` remains responsible only for wrapping annotated jobs and reporting status to `BatchEventNotificationService`.

## 7. Tests

- [ ] Unit test `EdgeHmacSigner` deterministic signature with fixed clock/secret/body.
- [ ] Unit test `EdgeNotificationGatewayAdapter` maps Slack recipient correctly.
- [ ] Unit test `EdgeNotificationGatewayAdapter` maps Email recipient correctly.
- [ ] Unit test `EdgeNotificationGatewayAdapter` sends HMAC headers.
- [ ] Unit test `BatchNotificationPolicy`:
  - [ ] `STARTED=false`
  - [ ] `SUCCEEDED=false`
  - [ ] `SKIPPED` with other code = false
  - [ ] `SKIPPED gate_disabled = true`
  - [ ] `FAILED = true`
  - [ ] repeated same fingerprint inside cooldown = false
- [ ] Unit test `BatchEventNotificationService` sends only when policy allows.

## 8. Docs/config

- [ ] Add `.env`/deployment note for `EDGE_INTERNAL_HMAC_SECRET`.
- [ ] Document local base URL: `http://localhost:3000`.
- [ ] Document docker base URL: `http://tchalanet-edge-service:3000`.
