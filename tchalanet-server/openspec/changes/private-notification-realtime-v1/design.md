# Design

## Transport

The browser opens an authenticated `text/event-stream` request using `fetch`, so the Firebase bearer
token and support-session tenant override remain HTTP headers. Native `EventSource` is not used because
it cannot attach these headers.

The stream payload is only:

```text
event: notification-change
data: {}
```

The web client responds by refetching the existing latest-items and unread-count endpoints.

## Recipient routing

Each stream is registered with its request context: user id, effective tenant id and scope.

- `PLATFORM_ADMINS` signals platform streams.
- tenant audiences signal admin streams for the matching effective tenant.
- `SPECIFIC_ACTORS` signals only matching app-user streams.
- broad all-user signals refresh all connected private streams.

An irrelevant stream receiving no event is normal. No notification title, body, target or metadata is
sent on the stream; authorization remains enforced by the existing REST snapshot reads.

## Lifecycle

`NotificationPublishedEvent` is observed after commit. The in-memory stream registry removes emitters
on timeout, error and completion. A client reconnects with bounded backoff after a transport failure.
