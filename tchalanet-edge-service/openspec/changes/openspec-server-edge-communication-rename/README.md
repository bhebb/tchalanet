# Generated OpenSpecs — server + edge-service communication rename

This zip contains two separate OpenSpec changes:

## P0 — tchalanet-server

Path:

```text
tchalanet-server/openspec/changes/p0-extract-common-communication-document
```

Purpose:

- extract generic outbound communication transport to `common.communication`;
- extract generic document/receipt rendering primitives to `common.document`;
- keep `core.notification` as notification center;
- keep `core.sales` as ticket/receipt business read model owner;
- prevent `features.cashier -> features.receipt`;
- preserve HMAC signing when renaming edge notification adapter to communication.

## P1 — tchalanet-edge-service

Path:

```text
tchalanet-edge-service/openspec/changes/p1-rename-notification-route-to-messages
```

Purpose:

- add canonical `POST /internal/messages/send`;
- keep legacy `POST /internal/notifications/send`;
- ensure both routes share the same handler/service and HMAC verification;
- rename edge-service internals progressively from notification to message/communication naming.
