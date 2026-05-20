# Design: P1 — Edge internal messages route

## Canonical route

```text
POST /internal/messages/send
```

This route is internal-only and receives signed server-to-edge messages.

The legacy notification route is removed:

```text
POST /internal/notifications/send
```

There is no alias and no fallback route.

## HMAC verification

The route must use HMAC verification:

```text
raw request body
  -> validate X-Tch-Timestamp
  -> validate X-Tch-Signature
  -> reject expired/replayed timestamp if currently implemented
  -> parse JSON
  -> sendMessageHandler
```

Important:

- Signature must be verified against the raw request body.
- Do not parse and reserialize before verifying.
- Signature algorithm must match Spring:

```text
payload_to_sign = X-Tch-Timestamp + "." + raw_json_body
X-Tch-Signature = "sha256=" + hex(HMAC_SHA256(secret, payload_to_sign))
```

- Required headers:

```text
X-Request-Id
Idempotency-Key
X-Tch-Timestamp
X-Tch-Signature
```

## Naming

Preferred target names:

```text
NotificationService       -> MessageService
NotificationSendRequest   -> SendMessageRequest
NotificationRecipient     -> MessageRecipient
NotificationDelivery      -> MessageDelivery
```

If a full internal type rename is risky, keep TypeScript compatibility aliases internally, but do not expose the old HTTP route.

## Request shape

Keep compatibility with the existing payload shape unless a separate breaking change is approved:

```json
{
  "eventId": "...",
  "severity": "INFO",
  "title": "...",
  "message": "...",
  "recipients": [
    {
      "channel": "SLACK",
      "to": null,
      "channelKey": "batch-draws"
    }
  ],
  "context": {}
}
```

## Non-goals

- Do not expose edge-service publicly.
- Do not change provider implementations in this change.
- Do not keep `/internal/notifications/send`.
- Do not redesign provider routing.
- Do not change HMAC algorithm/headers unless required by a separate security ADR.
