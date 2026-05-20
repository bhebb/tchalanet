# Spec — Standardize communication capability under platform.communication

## Goal

Move communication delivery/routing/templates to `platform.communication`.

## New package

```text
com.tchalanet.server.platform.communication.api.*
com.tchalanet.server.platform.communication.internal.*
```

## Public API

```text
CommunicationApi
SendMessageRequest
SendMessageResult
Recipient
Channel
DeliveryStatusView
```

## Internal implementation

```text
internal/app/CommunicationService
internal/app/DeliveryRouter
internal/app/MessageTemplateRenderer
internal/adapter/SlackAdapter
internal/adapter/EmailAdapter
internal/adapter/SmsAdapter
internal/persistence/MessageDeliveryJpaEntity
internal/web/CommunicationOpsController
```

## Migration tasks

- [ ] Inventory notification/edge-service client helpers.
- [ ] Move routing/delivery tracking to platform.communication.
- [ ] Keep external edge-service DTOs internal adapter-only unless they are true public contract.
- [ ] Core emits event or calls CommunicationApi depending on flow.
- [ ] Ensure communication failures do not rollback critical core transactions unless explicitly required.

## Verification

- [ ] Existing Slack/email/SMS test flows still work.
- [ ] Batch notifications still routed.
- [ ] No platform.communication -> core dependency.
