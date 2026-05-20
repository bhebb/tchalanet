# Change: P1 — Replace edge notification route with internal messages route

## Priority

P1

## Why

The edge-service currently exposes an internal notification route, but its responsibility is broader than notifications: it transports outbound communications to providers such as Slack, email, SMS, and WhatsApp.

Server-side naming is moving from `EdgeNotificationGatewayAdapter` to `EdgeCommunicationGatewayAdapter`, and the edge route should become:

```text
POST /internal/messages/send
```

The legacy notification route should be removed in the same change so there is only one internal outbound-message endpoint.

## What

Add the internal route:

```text
POST /internal/messages/send
```

Remove the legacy route:

```text
POST /internal/notifications/send
```

The remaining route must require HMAC verification.

Rename edge-service internals from notification-specific naming to message/communication naming where safe:

```text
NotificationService -> MessageService or CommunicationService
NotificationPayload -> SendMessageRequest
NotificationRecipient -> MessageRecipient
notification routes -> message routes
```

## Impact

- No public route is added.
- No web/mobile client should call edge-service.
- Existing `/internal/notifications/send` stops being supported.
- Spring must target `/internal/messages/send`.
- HMAC verification must be enabled for the new route.
